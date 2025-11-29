package com.wait;

import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import com.wait.entity.domain.Post;
import com.wait.entity.domain.PostFavorite;
import com.wait.entity.domain.PostLike;
import com.wait.entity.domain.UserBase;
import com.wait.mapper.CommentMapper;
import com.wait.mapper.FollowMapper;
import com.wait.mapper.PostFavoriteMapper;
import com.wait.mapper.PostLikeMapper;
import com.wait.mapper.PostMapper;
import com.wait.mapper.UserBaseMapper;
import com.wait.mapper.UserBlockMapper;
import com.wait.service.RelationService;
import com.wait.service.TimelineSortedSetService;

import lombok.extern.slf4j.Slf4j;

/**
 * 数据加载测试 - 将数据库中的所有数据加载到Redis
 */
@Slf4j
@SpringBootTest
public class DataLoadTest {

    @Autowired
    private UserBaseMapper userBaseMapper;
    @Autowired
    private PostMapper postMapper;
    @Autowired
    private FollowMapper followMapper;
    @Autowired
    private PostLikeMapper postLikeMapper;
    @Autowired
    private PostFavoriteMapper postFavoriteMapper;
    @Autowired
    private UserBlockMapper userBlockMapper;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private RelationService relationService;
    @Autowired
    private TimelineSortedSetService timelineService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    public void loadAllDataToRedis() {
        log.info("========== 开始加载数据库数据到Redis ==========");

        AtomicInteger userCount = new AtomicInteger(0);
        AtomicInteger postCount = new AtomicInteger(0);
        AtomicInteger followCount = new AtomicInteger(0);
        AtomicInteger likeCount = new AtomicInteger(0);
        AtomicInteger favoriteCount = new AtomicInteger(0);
        AtomicInteger blockCount = new AtomicInteger(0);
        AtomicInteger commentCount = new AtomicInteger(0);
        AtomicInteger timelineCount = new AtomicInteger(0);

        // 用于存储所有帖子，供后续步骤使用
        List<Post> allPosts = new java.util.ArrayList<>();

        try {
            // 1. 加载用户数据（通过查询触发缓存）
            log.info("1. 加载用户数据...");
            try {
                List<UserBase> users = userBaseMapper.selectAll();
                if (users != null) {
                    for (UserBase user : users) {
                        userBaseMapper.selectById(user.getId());
                        userCount.incrementAndGet();
                    }
                }
                log.info("✓ 已加载 {} 个用户到缓存", userCount.get());
            } catch (Exception e) {
                log.warn("加载用户数据失败: {}", e.getMessage());
            }

            // 2. 加载帖子数据
            log.info("2. 加载帖子数据...");
            try {
                List<UserBase> users = userBaseMapper.selectAll();
                if (users != null) {
                    // 通过用户ID查询所有帖子
                    for (UserBase user : users) {
                        List<Post> userPosts = postMapper.selectByUserId(user.getId());
                        if (userPosts != null) {
                            allPosts.addAll(userPosts);
                        }
                    }
                }
                for (Post post : allPosts) {
                    postMapper.selectById(post.getId());
                    postCount.incrementAndGet();
                }
                log.info("✓ 已加载 {} 个帖子到缓存", postCount.get());
            } catch (Exception e) {
                log.warn("加载帖子数据失败: {}", e.getMessage());
            }

            // 3. 加载关注关系
            log.info("3. 加载关注关系...");
            try {
                List<UserBase> users = userBaseMapper.selectAll();
                if (users != null) {
                    for (UserBase user : users) {
                        List<Long> followedIds = followMapper.selectFollowedIds(user.getId());
                        if (followedIds != null) {
                            for (Long followedId : followedIds) {
                                // 使用服务方法加载，会自动写入Redis
                                relationService.follow(user.getId(), followedId);
                                followCount.incrementAndGet();
                            }
                        }
                    }
                }
                log.info("✓ 已加载 {} 条关注关系到Redis", followCount.get());
            } catch (Exception e) {
                log.warn("加载关注关系失败（可能表不存在）: {}", e.getMessage());
            }

            // 4. 加载点赞关系
            log.info("4. 加载点赞关系...");
            try {
                List<PostLike> allLikes = postLikeMapper.selectAll();
                if (allLikes != null) {
                    for (PostLike like : allLikes) {
                        // 使用服务方法加载，会自动写入Redis
                        relationService.likePost(like.getUserId(), like.getPostId());
                        likeCount.incrementAndGet();
                    }
                }
                log.info("✓ 已加载 {} 条点赞关系到Redis", likeCount.get());
            } catch (Exception e) {
                log.warn("加载点赞关系失败（可能表不存在）: {}", e.getMessage());
            }

            // 5. 加载收藏关系
            log.info("5. 加载收藏关系...");
            try {
                List<PostFavorite> allFavorites = postFavoriteMapper.selectAll();
                if (allFavorites != null) {
                    for (PostFavorite favorite : allFavorites) {
                        // 使用服务方法加载，会自动写入Redis
                        relationService.favoritePost(favorite.getUserId(), favorite.getPostId());
                        favoriteCount.incrementAndGet();
                    }
                }
                log.info("✓ 已加载 {} 条收藏关系到Redis", favoriteCount.get());
            } catch (Exception e) {
                log.warn("加载收藏关系失败（可能表不存在）: {}", e.getMessage());
            }

            // 6. 加载黑名单关系
            log.info("6. 加载黑名单关系...");
            try {
                List<UserBase> users = userBaseMapper.selectAll();
                if (users != null) {
                    for (UserBase user : users) {
                        List<Long> blockedUserIds = userBlockMapper.selectBlockedUserIds(user.getId());
                        if (blockedUserIds != null) {
                            for (Long blockedUserId : blockedUserIds) {
                                relationService.blockUser(user.getId(), blockedUserId);
                                blockCount.incrementAndGet();
                            }
                        }
                    }
                }
                log.info("✓ 已加载 {} 条黑名单关系到Redis", blockCount.get());
            } catch (Exception e) {
                log.warn("加载黑名单关系失败（可能表不存在）: {}", e.getMessage());
            }

            // 7. 加载时间线数据
            log.info("7. 加载时间线数据...");
            try {
                for (Post post : allPosts) {
                    if (post.getCreatedAt() != null) {
                        long publishTime = post.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant()
                                .toEpochMilli();
                        timelineService.publishToTimeline(post.getUserId(), post.getId(), publishTime);
                        timelineCount.incrementAndGet();
                    }
                }
                log.info("✓ 已加载 {} 条帖子到时间线", timelineCount.get());
            } catch (Exception e) {
                log.warn("加载时间线数据失败: {}", e.getMessage());
            }

            // 8. 加载评论数据（评论数统计）
            log.info("8. 加载评论数据...");
            try {
                for (Post post : allPosts) {
                    int commentCountForPost = commentMapper.countByPostId(post.getId());
                    if (commentCountForPost > 0) {
                        commentCount.addAndGet(commentCountForPost);
                    }
                }
                log.info("✓ 已统计 {} 条评论", commentCount.get());
            } catch (Exception e) {
                log.warn("加载评论数据失败（可能表不存在）: {}", e.getMessage());
            }

            log.info("========== 数据加载完成 ==========");
            log.info("统计信息：");
            log.info("  - 用户数: {}", userCount.get());
            log.info("  - 帖子数: {}", postCount.get());
            log.info("  - 关注关系: {}", followCount.get());
            log.info("  - 点赞关系: {}", likeCount.get());
            log.info("  - 收藏关系: {}", favoriteCount.get());
            log.info("  - 黑名单关系: {}", blockCount.get());
            log.info("  - 时间线条目: {}", timelineCount.get());
            log.info("  - 评论数: {}", commentCount.get());

        } catch (Exception e) {
            log.error("数据加载过程中发生错误", e);
            log.warn("部分数据可能未加载成功，请检查数据库表是否存在");
        }
    }

    /**
     * 清理Redis测试数据（可选）
     */
    @Test
    public void clearRedisTestData() {
        log.info("========== 清理Redis测试数据 ==========");
        try {
            Set<String> keys = redisTemplate.keys("*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("✓ 已清理 {} 个Redis key", keys.size());
            } else {
                log.info("✓ Redis中没有数据需要清理");
            }
        } catch (Exception e) {
            log.error("清理Redis数据时发生错误", e);
        }
    }
}
