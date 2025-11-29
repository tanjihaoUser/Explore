package com.wait.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wait.config.script.TimeLineScripts;
import com.wait.entity.domain.Post;
import com.wait.mapper.PostMapper;
import com.wait.service.CommentService;
import com.wait.service.HotRankingService;
import com.wait.service.PostService;
import com.wait.service.RankingService;
import com.wait.service.RelationService;
import com.wait.service.TimelineSortedSetService;
import com.wait.service.UserService;
import com.wait.util.BoundUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;

    private final TimeLineScripts timeLineScripts;

    private final ObjectMapper redisObjectMapper;

    private final BoundUtil boundUtil;
    private final TimelineSortedSetService timelineSortedSetService;
    private final HotRankingService hotRankingService;
    private final RankingService rankingService;
    private final RelationService relationService;
    private final CommentService commentService;
    private final UserService userService;

    private static final String POST_PREFIX = "post:";
    private static final String USER_POST_REL_PREFIX = "user:post:rel:";
    // 使用Set存储所有空用户ID，节省空间（业界常见做法）
    private static final String USER_POST_EMPTY_SET = "user:post:empty:set";
    private static final long POST_EXPIRE_TIME = 60 * 5;
    private static final long USER_POST_EMPTY_EXPIRE_TIME = 60 * 10;
    // Redis中保存用户帖子的上限（测试环境设置为5）
    private static final int USER_POST_LIST_MAX_SIZE = 5;

    @Override
    public Long insert(Post post) {
        post.setLikeCount(0);
        post.setCommentCount(0);
        postMapper.insert(post);
        // MyBatis 通过 useGeneratedKeys="true" keyProperty="id" 自动将主键设置到 post.id 中
        Long postId = post.getId();
        if (postId == null) {
            throw new IllegalStateException("insert fail, can not get primary ID");
        }

        // 使用与 RedisTemplate 相同的 ObjectMapper 手动序列化对象为 JSON 字符串
        // 这样可以避免在 Lua 脚本执行时 RedisTemplate 再次序列化导致的多重转义问题
        String postJson;
        try {
            postJson = redisObjectMapper.writeValueAsString(post);
        } catch (IOException e) {
            log.error("Failed to serialize post to JSON", e);
            throw new IllegalStateException("Failed to serialize post to JSON", e);
        }

        // 如果用户在空用户Set中，需要移除（用户现在有帖子了）
        Boolean isInEmptySet = boundUtil.sIsMember(USER_POST_EMPTY_SET, post.getUserId());
        if (isInEmptySet != null && isInEmptySet) {
            boundUtil.sRem(USER_POST_EMPTY_SET, post.getUserId());
            log.debug("Removed user {} from empty set as new post is created", post.getUserId());
        }

        // 使用 Lua 脚本原子性地执行：1. 存储 Post 对象 2. 添加到用户帖子列表 3. 检查上限并删除超限元素
        List<String> keyList = new ArrayList<>();
        keyList.add(POST_PREFIX + postId);
        keyList.add(USER_POST_REL_PREFIX + post.getUserId());

        Long removedCount = timeLineScripts.executeScript(TimeLineScripts.PUBLISH_POST,
                keyList,
                postJson, postId, POST_EXPIRE_TIME, USER_POST_LIST_MAX_SIZE);

        if (removedCount != null && removedCount > 0) {
            log.info("Post list exceeded max size, removed {} old posts from user {} list",
                    removedCount, post.getUserId());
        }

        // 新增：添加到时间线 Sorted Set（使用当前时间作为时间戳）
        long publishTime = System.currentTimeMillis();
        try {
            timelineSortedSetService.publishToTimeline(post.getUserId(), postId, publishTime);
            log.debug("Added post {} to timeline", postId);
        } catch (Exception e) {
            log.error("Failed to add post {} to timeline", postId, e);
            // 不影响主流程，继续执行
        }

        // 新增：初始化热度分数（可选，如果帖子初始时就有统计数据）
        try {
            hotRankingService.updateHotScore(postId);
            log.debug("Initialized hot score for post {}", postId);
        } catch (Exception e) {
            log.error("Failed to initialize hot score for post {}", postId, e);
            // 不影响主流程，继续执行
        }

        return postId;
    }

    /**
     * 分页查询用户帖子
     * 
     * 业界常见做法（混合策略）：
     * 1. Redis只缓存最新的N条（如20-50条），用于快速访问首页
     * 2. 分页查询策略：
     * - 如果请求的是前N条（在Redis缓存范围内），直接从Redis获取（快速）
     * - 如果请求的是历史数据（超出Redis范围），从数据库查询（完整数据）
     * 3. 不在查询时截断列表，因为用户可能查看历史，应该保留更多数据
     * 
     * 优点：
     * - 首页访问快速（从Redis获取）
     * - 历史数据完整（从数据库获取）
     * - 内存可控（只缓存最新N条）
     * - 用户体验好（可以查看所有历史）
     */
    @Override
    public List<Post> getUserPagedPosts(Long userId, int page, int pageSize) {
        String key = USER_POST_REL_PREFIX + userId;

        // 先检查用户是否在空用户Set中
        Boolean isInEmptySet = boundUtil.sIsMember(USER_POST_EMPTY_SET, userId);
        if (isInEmptySet != null && isInEmptySet) {
            // 用户确实没有帖子
            log.debug("User {} has no posts (in empty set)", userId);
            return Collections.emptyList();
        }

        // 计算分页范围
        int start = (page - 1) * pageSize;
        int end = start + pageSize - 1;

        // 判断是否在Redis缓存范围内
        // Redis只缓存最新的USER_POST_LIST_MAX_SIZE条，所以只处理前N条
        if (end < USER_POST_LIST_MAX_SIZE) {
            // 在Redis缓存范围内，从Redis获取
            return getUserPagedPostsFromCache(userId, key, start, end);
        } else {
            // 超出Redis缓存范围，从数据库查询
            log.debug("Requested page {} exceeds Redis cache range, querying from database", page);
            return getUserPagedPostsFromDatabase(userId, page, pageSize);
        }
    }

    /**
     * 从Redis缓存中分页查询用户帖子
     */
    private List<Post> getUserPagedPostsFromCache(Long userId, String key, int start, int end) {
        // 检查列表 key 是否存在
        Boolean keyExists = boundUtil.exists(key);

        if (!Boolean.TRUE.equals(keyExists)) {
            // key 不存在，需要从数据库加载并初始化关系表（只加载最新的N条）
            log.info("Cache key {} not exists, loading from database and initializing", key);
            initializeUserPostRelation(userId);

            // 初始化后再次检查是否在空用户Set中
            Boolean isInEmptySet = boundUtil.sIsMember(USER_POST_EMPTY_SET, userId);
            if (isInEmptySet != null && isInEmptySet) {
                return Collections.emptyList();
            }
        }

        // 获取列表大小
        Long size = boundUtil.listSize(key);
        if (size == null || size == 0) {
            log.warn("List key {} exists but size is 0, this should not happen", key);
            return Collections.emptyList();
        }

        // 检查分页是否超出范围
        if (start >= size) {
            log.debug("Page out of cache range, start: {}, size: {}", start, size);
            return Collections.emptyList();
        }

        // 从Redis关系表判断分页数据是否存在
        // 在 LLEN 到 LRANGE 之间，可能有其他线程/Lua 脚本对列表做了 LTRIM、DEL、LREM 等操作
        List<Long> idList = boundUtil.range(key, start, end, Long.class);
        if (idList == null || idList.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量获取帖子详情（优化：避免N+1查询问题）
        return getPostsByIds(idList);
    }

    /**
     * 从数据库分页查询用户帖子（用于查询历史数据）
     * 使用数据库层面的分页（LIMIT/OFFSET），避免查询所有数据再分割
     * 
     * 业界常见做法：
     * 1. SQL中直接写LIMIT和OFFSET（当前实现）- 简单直接，性能好，适合大多数场景
     * 2. PageHelper插件 - 使用拦截器自动添加分页，代码更简洁，但需要引入依赖
     * 3. MyBatis-Plus - 功能强大，但需要引入整个框架
     * 
     * 对于当前项目，使用SQL直接分页是最佳选择：
     * - 不需要引入额外依赖
     * - 性能好（数据库层面分页）
     * - 代码清晰易懂
     */
    private List<Post> getUserPagedPostsFromDatabase(Long userId, int page, int pageSize) {
        // 计算偏移量（OFFSET）
        int offset = (page - 1) * pageSize;

        // 从数据库分页查询（数据库层面分页，性能更好）
        return postMapper.selectByUserIdWithPagination(userId, offset, pageSize);
    }

    /**
     * 初始化用户帖子关系表
     * 从数据库加载该用户的最新N条帖子ID，存入Redis列表
     * 
     * 注意：只加载最新的USER_POST_LIST_MAX_SIZE条，不加载所有帖子
     * 这样既保证首页访问速度，又控制内存使用
     */
    private void initializeUserPostRelation(Long userId) {
        List<Post> posts = postMapper.selectByUserId(userId);
        String key = USER_POST_REL_PREFIX + userId;

        if (posts == null || posts.isEmpty()) {
            // 用户没有帖子，添加到空用户Set中，避免频繁查询数据库
            boundUtil.sAdd(USER_POST_EMPTY_SET, userId);
            boundUtil.expire(USER_POST_EMPTY_SET, USER_POST_EMPTY_EXPIRE_TIME, TimeUnit.SECONDS);
            log.info("User {} has no posts, added to empty set", userId);
            return;
        }

        // 只取最新的USER_POST_LIST_MAX_SIZE条帖子
        int totalPosts = posts.size();
        int postsToCache = Math.min(totalPosts, USER_POST_LIST_MAX_SIZE);
        List<Post> postsToCacheList = posts.subList(0, postsToCache);

        // 将帖子ID按倒序（最新的在前）存入Redis列表
        List<Long> postIds = new ArrayList<>();
        for (Post post : postsToCacheList) {
            postIds.add(post.getId());
        }

        // 使用 rightPush 批量添加，保持倒序（最新的在列表头部）
        Collections.reverse(postIds); // 反转列表，最新的在前
        Long[] postIdArray = postIds.toArray(new Long[0]);
        boundUtil.rightPush(key, postIdArray);

        log.info("Initialized user post relation for user {}, cached {} posts (total: {})",
                userId, postsToCache, totalPosts);
    }

    /**
     * 根据ID获取单个帖子详情
     * 优先从Redis缓存获取，缓存未命中时从数据库查询
     */
    @Override
    public Post getPostById(Long postId) {
        if (postId == null) {
            return null;
        }

        // 1. 先从Redis缓存获取
        String cacheKey = POST_PREFIX + postId;
        Post cachedPost = boundUtil.get(cacheKey, Post.class);
        if (cachedPost != null) {
            log.debug("Post {} found in cache", postId);
            return cachedPost;
        }

        // 2. 缓存未命中，从数据库查询
        Post post = postMapper.selectById(postId);
        if (post != null && post.getIsDeleted() == 0) {
            // 3. 将查询结果写入Redis缓存
            boundUtil.set(cacheKey, post, POST_EXPIRE_TIME, TimeUnit.SECONDS);
            log.debug("Post {} loaded from database and cached", postId);
            return post;
        }

        log.debug("Post {} not found or deleted", postId);
        return null;
    }

    /**
     * 批量获取帖子详情
     * 优化：使用批量查询避免N+1问题，同时优先从Redis缓存获取
     * 
     * 策略：
     * 1. 先从Redis批量获取（MGET）
     * 2. 对于缓存未命中的，从数据库批量查询
     * 3. 将数据库查询结果写入Redis缓存
     * 4. 保持原有顺序（按postIds的顺序）
     */
    @Override
    public List<Post> getPostsByIds(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 使用Map保持顺序和快速查找
        Map<Long, Post> resultMap = new LinkedHashMap<>();
        List<Long> missingIds = new ArrayList<>();

        // 1. 批量从Redis获取
        List<String> cacheKeys = new ArrayList<>();
        for (Long postId : postIds) {
            cacheKeys.add(POST_PREFIX + postId);
        }
        List<Post> cachedPosts = boundUtil.mGet(cacheKeys, Post.class);

        // 2. 分离缓存命中和未命中的
        for (int i = 0; i < postIds.size(); i++) {
            Long postId = postIds.get(i);
            Post cachedPost = cachedPosts.get(i);
            if (cachedPost != null) {
                resultMap.put(postId, cachedPost);
            } else {
                missingIds.add(postId);
            }
        }

        // 3. 批量从数据库查询未命中的
        if (!missingIds.isEmpty()) {
            List<Post> dbPosts = postMapper.selectByIds(missingIds);
            if (dbPosts != null && !dbPosts.isEmpty()) {
                // 4. 将数据库查询结果写入Redis缓存
                // BoundUtil.set() 会自动使用 RedisTemplate 的序列化器序列化 Post 对象
                for (Post post : dbPosts) {
                    if (post != null && post.getIsDeleted() == 0) {
                        resultMap.put(post.getId(), post);
                        boundUtil.set(POST_PREFIX + post.getId(), post, POST_EXPIRE_TIME, TimeUnit.SECONDS);
                    }
                }
            }
        }

        // 5. 按postIds的顺序返回结果
        List<Post> result = new ArrayList<>();
        for (Long postId : postIds) {
            Post post = resultMap.get(postId);
            if (post != null) {
                result.add(post);
            }
        }

        return result;
    }

    /**
     * 批量获取帖子详情，并填充当前用户的点赞、收藏状态和统计数据
     * 优化：一次请求返回所有需要的数据，避免前端多次请求
     */
    @Override
    public List<Post> getPostsByIdsWithRelation(List<Long> postIds, Long currentUserId) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 获取帖子基本信息
        List<Post> posts = getPostsByIds(postIds);
        if (posts.isEmpty()) {
            return posts;
        }

        // 2. 批量获取统计数据（点赞数、收藏数、评论数）
        Map<Long, Long> likeCounts = new HashMap<>();
        Map<Long, Long> favoriteCounts = new HashMap<>();
        Map<Long, Integer> commentCounts = commentService.batchGetCommentCounts(postIds);

        for (Post post : posts) {
            Long postId = post.getId();
            if (postId != null) {
                likeCounts.put(postId, relationService.getLikeCount(postId));
                favoriteCounts.put(postId, relationService.getFavoriteCount(postId));
            }
        }

        // 3. 如果提供了当前用户ID，批量查询点赞和收藏状态
        Map<Long, Boolean> likedMap = Collections.emptyMap();
        Map<Long, Boolean> favoritedMap = Collections.emptyMap();
        if (currentUserId != null) {
            likedMap = relationService.batchCheckLiked(currentUserId, postIds);
            favoritedMap = relationService.batchCheckFavorited(currentUserId, postIds);
        }

        // 4. 批量获取用户名（从帖子中提取userId，去重后批量查询）
        Set<Long> userIds = posts.stream()
                .map(Post::getUserId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, String> usernameMap = new HashMap<>();
        for (Long userId : userIds) {
            try {
                com.wait.entity.domain.UserBase user = userService.findById(userId);
                if (user != null && user.getUsername() != null) {
                    usernameMap.put(userId, user.getUsername());
                }
            } catch (Exception e) {
                log.warn("Failed to get username for user {}", userId, e);
            }
        }

        // 5. 填充数据到Post对象
        for (Post post : posts) {
            Long postId = post.getId();
            if (postId != null) {
                // 填充统计数据
                Long likeCount = likeCounts.get(postId);
                if (likeCount != null) {
                    post.setLikeCount(likeCount.intValue());
                }
                Long favoriteCount = favoriteCounts.get(postId);
                if (favoriteCount != null) {
                    post.setFavoriteCount(favoriteCount.intValue());
                }
                Integer commentCount = commentCounts.get(postId);
                if (commentCount != null) {
                    post.setCommentCount(commentCount);
                }

                // 填充关系状态（如果提供了当前用户ID）
                if (currentUserId != null) {
                    post.setIsLiked(likedMap.getOrDefault(postId, false));
                    post.setIsFavorited(favoritedMap.getOrDefault(postId, false));
                }

                // 填充用户名
                if (post.getUserId() != null) {
                    String username = usernameMap.get(post.getUserId());
                    if (username != null) {
                        post.setUsername(username);
                    }
                }
            }
        }

        return posts;
    }

    /**
     * 更新帖子
     * 使用Cache-Aside策略：先更新数据库，然后更新缓存
     * 注意：BoundUtil.set() 会自动使用 RedisTemplate 的序列化器（Jackson2JsonRedisSerializer）
     * 将 Post 对象序列化为 JSON，无需手动序列化
     */
    @Override
    public int update(Post post) {
        Long postId = post.getId();
        if (postId == null) {
            log.warn("Post ID is null, cannot update");
            return 0;
        }

        // 1. 先更新数据库（Cache-Aside策略）
        int rowsAffected = postMapper.update(post);
        if (rowsAffected <= 0) {
            return rowsAffected;
        }

        // 2. 更新Redis缓存
        // BoundUtil.set() 会自动使用 RedisTemplate 的序列化器序列化 Post 对象
        String cacheKey = POST_PREFIX + postId;
        boundUtil.set(cacheKey, post, POST_EXPIRE_TIME, TimeUnit.SECONDS);
        log.debug("Updated post cache: {}", postId);

        // 新增：如果评论数变化，更新排行榜和热度分数（可选）
        // 注意：这里假设评论数是通过Post的commentCount字段更新的
        // 如果需要更精确的控制，可以在评论Service中直接调用排行榜更新方法
        try {
            hotRankingService.updateHotScore(postId);
            log.debug("Updated hot score for post {} after update", postId);
        } catch (Exception e) {
            log.error("Failed to update hot score for post {}", postId, e);
            // 不影响主流程，继续执行
        }

        return rowsAffected;
    }

    /**
     * 评论数更新时调用，更新排行榜和热度分数
     * 
     * @param postId 帖子ID
     */
    @Override
    public void onCommentCountChanged(Long postId) {
        if (postId == null) {
            return;
        }
        try {
            // 更新评论排行榜
            rankingService.onComment(postId);
            // 更新热度分数
            hotRankingService.onComment(postId);
            log.debug("Updated ranking and hot score for post {} due to comment count change", postId);
        } catch (Exception e) {
            log.error("Failed to update ranking/hot score for post {} due to comment", postId, e);
            // 不影响主流程
        }
    }

    /**
     * 增加评论数
     * 
     * @param postId 帖子ID
     */
    @Override
    public void incrementCommentCount(Long postId) {
        if (postId == null) {
            return;
        }
        try {
            // 更新数据库
            Post post = postMapper.selectById(postId);
            if (post != null) {
                post.setCommentCount((post.getCommentCount() != null ? post.getCommentCount() : 0) + 1);
                postMapper.update(post);
                // 更新缓存
                boundUtil.set(POST_PREFIX + postId, post, POST_EXPIRE_TIME, TimeUnit.SECONDS);
            }
            // 更新排行榜和热度
            onCommentCountChanged(postId);
            log.debug("Incremented comment count for post {}", postId);
        } catch (Exception e) {
            log.error("Failed to increment comment count for post {}", postId, e);
        }
    }

    /**
     * 减少评论数
     * 
     * @param postId 帖子ID
     */
    @Override
    public void decrementCommentCount(Long postId) {
        if (postId == null) {
            return;
        }
        try {
            // 更新数据库
            Post post = postMapper.selectById(postId);
            if (post != null && post.getCommentCount() != null && post.getCommentCount() > 0) {
                post.setCommentCount(post.getCommentCount() - 1);
                postMapper.update(post);
                // 更新缓存
                boundUtil.set(POST_PREFIX + postId, post, POST_EXPIRE_TIME, TimeUnit.SECONDS);
            }
            // 更新排行榜和热度
            onCommentCountChanged(postId);
            log.debug("Decremented comment count for post {}", postId);
        } catch (Exception e) {
            log.error("Failed to decrement comment count for post {}", postId, e);
        }
    }

    /**
     * 删除帖子
     * 
     * 业界常见做法：采用懒加载策略，删除后不主动补充帖子
     * 原因：
     * 1. 删除操作已经比较重（需要更新数据库和缓存）
     * 2. 补充帖子需要额外的数据库查询，增加延迟
     * 3. 用户可能不会立即查看列表，提前补充浪费资源
     * 4. 列表有过期时间，过期后会自动重新加载最新数据
     * 5. 如果列表为空，会在下次查询时从数据库重新加载
     * 
     * 如果需要主动补充（适用于热点数据场景）：
     * - 可以在删除后异步从数据库加载一条新帖子补充到列表尾部
     * - 但会增加系统复杂度，需要权衡性能和一致性
     */
    @Override
    public int delete(Long userId, Long postId) {
        // 1. 从数据库删除（逻辑删除）
        int rowsAffected = postMapper.delete(postId);
        if (rowsAffected <= 0) {
            log.warn("Post {} not found or already deleted", postId);
            return rowsAffected;
        }

        // 2. 使用Lua脚本原子性地执行Redis操作
        // 采用懒加载策略：删除后不主动补充帖子，等下次查询时从数据库加载
        List<String> keyList = new ArrayList<>();
        keyList.add(POST_PREFIX + postId);
        keyList.add(USER_POST_REL_PREFIX + userId);
        keyList.add(USER_POST_EMPTY_SET);

        Long removedCount = timeLineScripts.executeScript(TimeLineScripts.DELETE_POST,
                keyList,
                postId, userId, USER_POST_EMPTY_EXPIRE_TIME);

        if (removedCount != null && removedCount > 0) {
            log.debug("Deleted post {} from cache, removed count from user {} post list: {}",
                    postId, userId, removedCount);
        } else {
            log.warn("Post {} not found in user {} post list cache", postId, userId);
        }

        // 新增：从时间线移除帖子
        try {
            timelineSortedSetService.removeFromTimeline(userId, postId);
            log.debug("Removed post {} from timeline", postId);
        } catch (Exception e) {
            log.error("Failed to remove post {} from timeline", postId, e);
            // 不影响主流程，继续执行
        }

        // 新增：从排行榜移除（可选，如果需要）
        // 删除的帖子通常不需要立即从排行榜移除，可以等定期清理时自然过期
        // 如果需要立即移除，可以在这里调用相应的服务

        return rowsAffected;
    }
}
