package com.wait.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.redis.connection.RedisZSetCommands.Aggregate;
import org.springframework.stereotype.Service;

import com.wait.config.script.TimeLineScripts;
import com.wait.entity.domain.Post;
import com.wait.mapper.PostMapper;
import com.wait.service.RelationService;
import com.wait.service.TimelineSortedSetService;
import com.wait.util.BoundUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 时间线排序服务实现
 * 使用 Sorted Set 实现按发布时间排序的时间线功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimelineSortedSetServiceImpl implements TimelineSortedSetService {

    private final BoundUtil boundUtil;
    private final TimeLineScripts timeLineScripts;
    private final RelationService relationService;
    private final PostMapper postMapper;

    private static final String TIMELINE_USER_POSTS_PREFIX = "timeline:posts:user:";
    private static final String TIMELINE_GLOBAL_PREFIX = "timeline:posts:global";
    private static final String TIMELINE_MY_PREFIX = "timeline:posts:my:"; // 我的时间线临时key
    private static final int MAX_CACHED_POSTS = 1000; // 最多缓存1000条

    @Override
    public void publishToTimeline(Long userId, Long postId, long publishTime) {
        // 使用 Lua 脚本原子性地执行：添加到用户时间线、全局时间线，并限制大小
        List<String> keys = new ArrayList<>();
        keys.add(TIMELINE_USER_POSTS_PREFIX + userId);
        keys.add(TIMELINE_GLOBAL_PREFIX);

        Long removedCount = timeLineScripts.executeScript(
                TimeLineScripts.PUBLISH_TO_TIMELINE,
                keys,
                String.valueOf(postId),
                String.valueOf(publishTime),
                String.valueOf(MAX_CACHED_POSTS));

        if (removedCount != null && removedCount > 0) {
            log.debug("Published post {} to timeline for user {}, time: {}, removed {} old posts",
                    postId, userId, publishTime, removedCount);
        } else {
            log.debug("Published post {} to timeline for user {}, time: {}", postId, userId, publishTime);
        }
    }

    @Override
    public List<Long> getUserTimeline(Long userId, int page, int pageSize) {
        String key = TIMELINE_USER_POSTS_PREFIX + userId;
        long start = (page - 1) * pageSize;
        long end = start + pageSize - 1;

        // 使用 zReverseRange 获取最新的帖子（分数从高到低，时间戳越大越新）
        Set<Long> postIds = boundUtil.zReverseRange(key, start, end, Long.class);
        return new ArrayList<>(postIds);
    }

    @Override
    public List<Long> getGlobalTimeline(int page, int pageSize) {
        long start = (page - 1) * pageSize;
        long end = start + pageSize - 1;

        Set<Long> postIds = boundUtil.zReverseRange(
                TIMELINE_GLOBAL_PREFIX, start, end, Long.class);
        return new ArrayList<>(postIds);
    }

    @Override
    public List<Long> getPostsByTimeRange(Long userId, long startTime, long endTime) {
        String key = userId != null
                ? TIMELINE_USER_POSTS_PREFIX + userId
                : TIMELINE_GLOBAL_PREFIX;

        // 使用 zRangeByScore 查询时间范围内的帖子（按时间从早到晚）
        Set<Long> postIds = boundUtil.zRangeByScore(key, startTime, endTime, Long.class);
        return new ArrayList<>(postIds);
    }

    @Override
    public void removeFromTimeline(Long userId, Long postId) {
        // 使用 Lua 脚本原子性地执行：从用户时间线和全局时间线移除帖子
        List<String> keys = new ArrayList<>();
        keys.add(TIMELINE_USER_POSTS_PREFIX + userId);
        keys.add(TIMELINE_GLOBAL_PREFIX);

        Long removedCount = timeLineScripts.executeScript(
                TimeLineScripts.REMOVE_FROM_TIMELINE,
                keys,
                String.valueOf(postId));

        if (removedCount != null && removedCount > 0) {
            log.debug("Removed post {} from timeline for user {}, removed from {} timelines",
                    postId, userId, removedCount);
        } else {
            log.debug("Post {} not found in timeline for user {}", postId, userId);
        }
    }

    @Override
    public List<Long> getMyTimeline(Long userId, int page, int pageSize) {
        // 1. 获取用户关注的所有用户ID
        Set<Long> following = relationService.getFollowing(userId);
        if (following == null || following.isEmpty()) {
            log.debug("User {} has no following, returning empty timeline", userId);
            return new ArrayList<>();
        }

        // 2. 获取用户的黑名单列表（我拉黑的用户，用于过滤）
        Set<Long> blacklistSet = relationService.getBlacklist(userId);
        if (blacklistSet == null) {
            blacklistSet = new HashSet<>();
        }
        final Set<Long> blacklist = blacklistSet;

        // 3. 过滤掉黑名单用户和拉黑了我的用户
        // - 过滤掉我拉黑的用户（blacklist）
        // - 过滤掉拉黑了我的用户（使用 isBlocked 检查对方是否拉黑了我）
        List<Long> validFollowing = following.stream()
                .filter(id -> {
                    // 排除我拉黑的用户
                    if (blacklist.contains(id)) {
                        return false;
                    }
                    // 排除拉黑了我的用户（对方拉黑了我，我也看不到对方的帖子）
                    if (relationService.isBlocked(id, userId)) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        if (validFollowing.isEmpty()) {
            log.debug("User {} has no valid following after filtering blacklist", userId);
            return new ArrayList<>();
        }

        // 4. 构建临时key用于聚合时间线
        //
        // 【临时key的作用和逻辑说明】
        // Redis的ZUNIONSTORE命令需要将多个Sorted Set合并到一个目标key中，但该操作不能直接返回结果，
        // 必须先将结果存储到某个key，然后再从这个key中读取数据。
        //
        // 临时key的设计：
        // - Key格式：timeline:posts:my:{userId}:{timestamp}
        // - 使用时间戳确保每次查询的临时key都是唯一的，避免并发请求相互覆盖
        // - 在finally块中删除临时key，确保即使出现异常也不会留下垃圾数据
        //
        // 运算过程：
        // 1. 将所有关注用户的时间线Sorted Set进行并集运算（ZUNIONSTORE）
        // 2. 聚合方式使用MAX：如果同一个postId出现在多个用户的时间线中，取最大的时间戳（最新）
        // 3. 结果存储到临时key
        // 4. 从临时key中按分数倒序（最新在前）获取分页数据
        // 5. 删除临时key
        //
        // 性能说明：
        // - ZUNIONSTORE是Redis服务器端操作，性能高，原子性强
        // - 临时key使用时间戳，避免并发冲突
        // - finally块确保临时key一定会被清理，不会造成内存泄漏
        String tempKey = TIMELINE_MY_PREFIX + userId + ":" + System.currentTimeMillis();
        try {
            // 5. 聚合关注用户的时间线（使用ZUNIONSTORE）
            List<String> sourceKeys = validFollowing.stream()
                    .map(id -> TIMELINE_USER_POSTS_PREFIX + id)
                    .collect(Collectors.toList());

            if (!sourceKeys.isEmpty()) {
                // 执行ZUNIONSTORE：聚合多个Sorted Set，取最大值（相同postId只保留最新的时间戳）
                boundUtil.zUnionAndStore(tempKey, sourceKeys, Aggregate.MAX);

                // 6. 从聚合结果中获取分页数据
                long start = (page - 1) * pageSize;
                long end = start + pageSize - 1;

                Set<Long> postIds = boundUtil.zReverseRange(tempKey, start, end, Long.class);
                List<Long> result = new ArrayList<>(postIds != null ? postIds : new ArrayList<>());

                // 7. 二次过滤：检查每个帖子的作者是否在黑名单中，或是否拉黑了我
                // 因为可能有些帖子是之前关注但后来拉黑的用户发布的，或者对方后来拉黑了我
                List<Long> filteredResult = filterPostsByBlacklistAndBlocked(result, blacklist, userId);

                log.debug("User {} timeline: following={}, blacklist={}, result={}",
                        userId, validFollowing.size(), blacklist.size(), filteredResult.size());

                return filteredResult;
            }
        } finally {
            // 8. 删除临时key，避免内存泄漏
            boundUtil.del(tempKey);
        }

        return new ArrayList<>();
    }

    /**
     * 过滤黑名单用户发布的帖子，以及被对方拉黑的情况
     * 
     * 优化说明：
     * - 使用selectUserIdsByIds方法，只查询id和userId字段，减少数据库传输的数据量
     * - 只构建postId -> userId的映射，不需要完整的Post对象
     * - 提高查询性能，减少内存占用
     * 
     * @param postIds       帖子ID列表
     * @param blacklist     我拉黑的用户列表
     * @param currentUserId 当前用户ID（用于检查是否被对方拉黑）
     * @return 过滤后的帖子ID列表
     */
    private List<Long> filterPostsByBlacklistAndBlocked(List<Long> postIds, Set<Long> blacklist, Long currentUserId) {
        if (postIds == null || postIds.isEmpty()) {
            return postIds;
        }

        // 批量查询帖子的用户ID映射（只查询id和userId字段，优化性能）
        List<Post> posts = postMapper.selectUserIdsByIds(postIds);
        if (posts == null || posts.isEmpty()) {
            return postIds;
        }

        // 构建postId -> userId的映射
        Map<Long, Long> postAuthorMap = posts.stream()
                .filter(post -> post != null && post.getUserId() != null)
                .collect(Collectors.toMap(Post::getId, Post::getUserId));

        // 过滤掉以下情况的帖子：
        // 1. 作者在我拉黑的用户列表中
        // 2. 作者拉黑了我（对方拉黑了我，我也看不到对方的帖子）
        return postIds.stream()
                .filter(postId -> {
                    Long authorId = postAuthorMap.get(postId);
                    // 如果找不到作者信息，保留该帖子（安全起见）
                    if (authorId == null) {
                        return true;
                    }
                    // 排除我拉黑的用户发布的帖子
                    if (blacklist != null && blacklist.contains(authorId)) {
                        return false;
                    }
                    // 排除拉黑了我的用户发布的帖子
                    if (relationService.isBlocked(authorId, currentUserId)) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }
}
