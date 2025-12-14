package com.wait.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.wait.config.script.RelationScripts;
import com.wait.entity.domain.UserBase;
import com.wait.service.HotRankingService;
import com.wait.service.NotificationService;
import com.wait.service.PostService;
import com.wait.service.RankingService;
import com.wait.service.RelationPersistenceService;
import com.wait.service.RelationService;
import com.wait.service.StatisticsService;
import com.wait.service.UserService;
import com.wait.util.BoundUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 关系服务实现 - 使用 Redis Set 实现关注、点赞、收藏等功能
 * 使用 Lua 脚本确保多条 Redis 命令的原子性
 */
@Slf4j
@Service
public class RelationServiceImpl implements RelationService {

    private final BoundUtil boundUtil;
    private final RelationScripts relationScripts;
    private final RelationPersistenceService persistenceService;
    private final RankingService rankingService;
    private final HotRankingService hotRankingService;
    private final NotificationService notificationService;
    private final PostService postService;
    private final UserService userService;
    private final StatisticsService statisticsService;

    public RelationServiceImpl(BoundUtil boundUtil, RelationScripts relationScripts,
            RelationPersistenceService persistenceService, RankingService rankingService,
            HotRankingService hotRankingService, NotificationService notificationService,
            @Lazy PostService postService, UserService userService,
            StatisticsService statisticsService) {
        this.boundUtil = boundUtil;
        this.relationScripts = relationScripts;
        this.persistenceService = persistenceService;
        this.rankingService = rankingService;
        this.hotRankingService = hotRankingService;
        this.notificationService = notificationService;
        this.postService = postService;
        this.userService = userService;
        this.statisticsService = statisticsService;
    }

    // Redis Key 前缀
    private static final String USER_FOLLOW_PREFIX = "user:follow:";
    private static final String USER_FOLLOWER_PREFIX = "user:follower:";
    private static final String POST_LIKE_PREFIX = "post:like:";
    private static final String USER_LIKE_PREFIX = "user:like:";
    private static final String POST_LIKE_COUNT_PREFIX = "post:like_count:";
    private static final String USER_FAVORITE_PREFIX = "user:favorite:";
    private static final String POST_FAVORITED_BY_PREFIX = "post:favorited_by:";
    private static final String POST_FAVORITE_COUNT_PREFIX = "post:favorite_count:";
    private static final String USER_BLACKLIST_PREFIX = "user:blacklist:";
    private static final String USER_BLOCKED_BY_PREFIX = "user:blocked_by:";

    // ==================== 关注相关 ====================

    @Override
    public boolean follow(Long followerId, Long followedId) {
        if (followerId == null || followedId == null) {
            throw new IllegalArgumentException("user id is null");
        }
        if (followerId.equals(followedId)) {
            log.warn("user {} cannot follow itself", followerId);
            return false; // 不能关注自己
        }

        // 使用 Lua 脚本原子性地执行关注操作
        List<String> keys = new ArrayList<>();
        keys.add(USER_FOLLOW_PREFIX + followerId);
        keys.add(USER_FOLLOWER_PREFIX + followedId);

        Long added = relationScripts.executeScript(RelationScripts.FOLLOW, keys, followedId, followerId);

        if (added != null && added > 0) {
            log.info("user {} follows user {}", followerId, followedId);

            // Write-Through: 立即持久化到数据库
            try {
                persistenceService.persistFollow(followerId, followedId, true);
            } catch (Exception e) {
                // 不抛出异常，Redis已成功，数据库写入失败可以通过补偿机制处理
                log.error("Failed to persist follow to DB, but Redis operation succeeded", e);
            }

            // 异步发送关注通知给被关注的用户
            try {
                UserBase follower = userService.findById(followerId);
                String followerName = follower != null && follower.getUsername() != null
                        ? follower.getUsername()
                        : "用户" + followerId;
                notificationService.sendNotificationAsync(
                        followedId,
                        "follow",
                        String.format("%s关注了你", followerName),
                        followerId);
            } catch (Exception e) {
                // 通知发送失败不影响主流程
                log.error("Failed to send follow notification: follower={}, followed={}",
                        followerId, followedId, e);
            }

            return true;
        }
        log.debug("user {} already follows user {} or operation failed", followerId, followedId);
        return false;
    }

    @Override
    public boolean unfollow(Long followerId, Long followedId) {
        if (followerId == null || followedId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        // 使用 Lua 脚本原子性地执行取消关注操作
        List<String> keys = new ArrayList<>();
        keys.add(USER_FOLLOW_PREFIX + followerId);
        keys.add(USER_FOLLOWER_PREFIX + followedId);

        Long removed = relationScripts.executeScript(RelationScripts.UNFOLLOW, keys, followedId, followerId);

        if (removed != null && removed > 0) {
            log.info("user {} unfollows user {}", followerId, followedId);

            // Write-Through: 立即持久化到数据库
            try {
                persistenceService.persistFollow(followerId, followedId, false);
            } catch (Exception e) {
                log.error("Failed to persist unfollow to DB, but Redis operation succeeded", e);
            }

            return true;
        }
        log.debug("user {} does not follow user {}, no need to unfollow", followerId, followedId);
        return false;
    }

    @Override
    public boolean isFollowing(Long followerId, Long followedId) {
        if (followerId == null || followedId == null) {
            return false;
        }
        return Boolean.TRUE.equals(
                boundUtil.sIsMember(USER_FOLLOW_PREFIX + followerId, followedId));
    }

    @Override
    public Set<Long> getFollowing(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        return boundUtil.sMembers(USER_FOLLOW_PREFIX + userId, Long.class);
    }

    @Override
    public Set<Long> getFollowers(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        return boundUtil.sMembers(USER_FOLLOWER_PREFIX + userId, Long.class);
    }

    @Override
    public Long getFollowingCount(Long userId) {
        if (userId == null) {
            return 0L;
        }
        return boundUtil.sCard(USER_FOLLOW_PREFIX + userId);
    }

    @Override
    public Long getFollowerCount(Long userId) {
        if (userId == null) {
            return 0L;
        }
        return boundUtil.sCard(USER_FOLLOWER_PREFIX + userId);
    }

    @Override
    public Set<Long> getMutualFollowing(Long userId1, Long userId2) {
        if (userId1 == null || userId2 == null) {
            return Collections.emptySet();
        }
        // 使用交集运算获取共同关注
        return boundUtil.sIntersect(
                USER_FOLLOW_PREFIX + userId1,
                USER_FOLLOW_PREFIX + userId2,
                Long.class);
    }

    @Override
    public boolean isMutualFollowing(Long userId1, Long userId2) {
        return isFollowing(userId1, userId2) && isFollowing(userId2, userId1);
    }

    @Override
    public Set<Long> getMutualFollowingMultiple(List<Long> userIds) {
        if (userIds == null || userIds.size() < 2) {
            return Collections.emptySet();
        }

        // 使用 SINTER 获取多个用户的共同关注
        List<String> keys = userIds.stream()
                .map(userId -> USER_FOLLOW_PREFIX + userId)
                .collect(Collectors.toList());

        return boundUtil.sIntersect(keys, Long.class);
    }

    @Override
    public Set<Long> getRecommendedFollowing(Long userId1, Long userId2) {
        if (userId1 == null || userId2 == null) {
            return Collections.emptySet();
        }

        // 使用 SDIFF 获取用户1关注但用户2未关注的用户
        return boundUtil.sDifference(
                USER_FOLLOW_PREFIX + userId1,
                USER_FOLLOW_PREFIX + userId2,
                Long.class);
    }

    @Override
    public Set<Long> getFollowingUnion(Long userId1, Long userId2) {
        if (userId1 == null || userId2 == null) {
            return Collections.emptySet();
        }

        // 使用 SUNION 获取两个用户的所有关注
        return boundUtil.sUnion(
                USER_FOLLOW_PREFIX + userId1,
                USER_FOLLOW_PREFIX + userId2,
                Long.class);
    }

    @Override
    public Set<Long> getMutualFollowers(Long userId1, Long userId2) {
        if (userId1 == null || userId2 == null) {
            return Collections.emptySet();
        }

        // 使用 SINTER 获取两个用户的共同粉丝
        return boundUtil.sIntersect(
                USER_FOLLOWER_PREFIX + userId1,
                USER_FOLLOWER_PREFIX + userId2,
                Long.class);
    }

    // ==================== 点赞相关 ====================

    @Override
    public boolean likePost(Long userId, Long postId) {
        if (userId == null || postId == null) {
            throw new IllegalArgumentException("user id or post id is null");
        }

        // 使用 Lua 脚本原子性地执行点赞操作
        // 存储帖子点赞用户是为了展示头像，提供朋友共同点赞功能
        List<String> keys = new ArrayList<>();
        keys.add(POST_LIKE_PREFIX + postId);
        keys.add(USER_LIKE_PREFIX + userId);
        keys.add(POST_LIKE_COUNT_PREFIX + postId);

        Long added = relationScripts.executeScript(RelationScripts.LIKE_POST, keys, userId, postId);

        if (added != null && added > 0) {
            log.info("user {} likes post {}", userId, postId);

            // Write-Behind: 异步持久化到数据库（不阻塞主流程）
            persistenceService.persistLike(userId, postId, true)
                    .exceptionally(ex -> {
                        log.error("Failed to persist like to DB (async), user {} likes post {}", userId, postId, ex);
                        return null;
                    });

            // 新增：更新排行榜和热度分数
            try {
                rankingService.onLike(postId);
                hotRankingService.onLike(postId);
                log.debug("Updated ranking and hot score for post {} due to like", postId);
            } catch (Exception e) {
                log.error("Failed to update ranking/hot score for post {}", postId, e);
                // 不影响主流程，继续执行
            }

            // 记录点赞统计
            try {
                statisticsService.recordLike(postId, true);
            } catch (Exception e) {
                log.error("Failed to record like statistics: postId={}", postId, e);
            }

            // 异步发送点赞通知给帖子作者
            try {
                com.wait.entity.domain.Post post = postService.getPostById(postId);
                if (post != null) {
                    Long postAuthorId = post.getUserId();
                    // 如果不是自己点赞的，发送通知
                    if (!postAuthorId.equals(userId)) {
                        com.wait.entity.domain.UserBase liker = userService.findById(userId);
                        String likerName = liker != null && liker.getUsername() != null
                                ? liker.getUsername()
                                : "用户" + userId;
                        notificationService.sendNotificationAsync(
                                postAuthorId,
                                "like",
                                String.format("%s点赞了你的帖子", likerName),
                                postId);
                    }
                }
            } catch (Exception e) {
                // 通知发送失败不影响主流程
                log.error("Failed to send like notification: user={}, post={}", userId, postId, e);
            }

            return true;
        }
        log.debug("user {} already likes post {}", userId, postId);
        return false;
    }

    @Override
    public boolean unlikePost(Long userId, Long postId) {
        if (userId == null || postId == null) {
            throw new IllegalArgumentException("user id or post id is null");
        }

        // 使用 Lua 脚本原子性地执行取消点赞操作
        List<String> keys = new ArrayList<>();
        keys.add(POST_LIKE_PREFIX + postId);
        keys.add(USER_LIKE_PREFIX + userId);
        keys.add(POST_LIKE_COUNT_PREFIX + postId);

        Long removed = relationScripts.executeScript(RelationScripts.UNLIKE_POST, keys, userId, postId);

        if (removed != null && removed > 0) {
            log.info("user {} unlikes post {}", userId, postId);

            // Write-Behind: 异步持久化到数据库
            persistenceService.persistLike(userId, postId, false)
                    .exceptionally(ex -> {
                        log.error("Failed to persist unlike to DB (async), user {} unlikes post {}", userId, postId,
                                ex);
                        return null;
                    });

            // 新增：更新排行榜和热度分数
            try {
                rankingService.onUnlike(postId);
                hotRankingService.onUnlike(postId);
                log.debug("Updated ranking and hot score for post {} due to unlike", postId);
            } catch (Exception e) {
                log.error("Failed to update ranking/hot score for post {}", postId, e);
                // 不影响主流程，继续执行
            }

            // 记录取消点赞统计
            try {
                statisticsService.recordLike(postId, false);
            } catch (Exception e) {
                log.error("Failed to record unlike statistics: postId={}", postId, e);
            }

            return true;
        }
        log.debug("user {} already unlikes post {}", userId, postId);
        return false;
    }

    @Override
    public boolean isLiked(Long userId, Long postId) {
        if (userId == null || postId == null) {
            return false;
        }
        return Boolean.TRUE.equals(
                boundUtil.sIsMember(POST_LIKE_PREFIX + postId, userId));
    }

    @Override
    public Set<Long> getLikers(Long postId) {
        if (postId == null) {
            return Collections.emptySet();
        }
        return boundUtil.sMembers(POST_LIKE_PREFIX + postId, Long.class);
    }

    @Override
    public Long getLikeCount(Long postId) {
        if (postId == null) {
            return 0L;
        }
        // 优先从 Set 获取准确计数
        return boundUtil.sCard(POST_LIKE_PREFIX + postId);
    }

    @Override
    public Map<Long, Boolean> batchCheckLiked(Long userId, List<Long> postIds) {
        if (userId == null || postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Boolean> result = new HashMap<>();
        for (Long postId : postIds) {
            result.put(postId, isLiked(userId, postId));
        }
        return result;
    }

    @Override
    public Set<Long> getUserLikedPosts(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        return boundUtil.sMembers(USER_LIKE_PREFIX + userId, Long.class);
    }

    // ==================== 收藏相关 ====================

    @Override
    public boolean favoritePost(Long userId, Long postId) {
        if (userId == null || postId == null) {
            throw new IllegalArgumentException("user id or post id is null");
        }

        // 使用 Lua 脚本原子性地执行收藏操作
        List<String> keys = new ArrayList<>();
        keys.add(USER_FAVORITE_PREFIX + userId);
        keys.add(POST_FAVORITED_BY_PREFIX + postId);
        keys.add(POST_FAVORITE_COUNT_PREFIX + postId);

        Long added = relationScripts.executeScript(RelationScripts.FAVORITE_POST, keys, userId, postId);

        if (added != null && added > 0) {
            log.info("user {} favorites post {}", userId, postId);

            // Write-Behind: 异步持久化到数据库
            persistenceService.persistFavorite(userId, postId, true)
                    .exceptionally(ex -> {
                        log.error("Failed to persist favorite to DB (async), user {} favorites post {}", userId, postId,
                                ex);
                        return null;
                    });

            // 新增：更新排行榜和热度分数
            try {
                rankingService.onFavorite(postId);
                hotRankingService.onFavorite(postId);
                log.debug("Updated ranking and hot score for post {} due to favorite", postId);
            } catch (Exception e) {
                log.error("Failed to update ranking/hot score for post {}", postId, e);
                // 不影响主流程，继续执行
            }

            // 记录收藏统计
            try {
                statisticsService.recordFavorite(postId, true);
            } catch (Exception e) {
                log.error("Failed to record favorite statistics: postId={}", postId, e);
            }

            // 异步发送收藏通知给帖子作者（可选，通常收藏不发送通知，这里提供选项）
            // 如果需要发送收藏通知，取消下面的注释
            /*
             * try {
             * com.wait.entity.domain.Post post = postService.getPostById(postId);
             * if (post != null) {
             * Long postAuthorId = post.getUserId();
             * // 如果不是自己收藏的，发送通知
             * if (!postAuthorId.equals(userId)) {
             * com.wait.entity.domain.UserBase favoriter = userService.findById(userId);
             * String favoriterName = favoriter != null && favoriter.getUsername() != null
             * ? favoriter.getUsername() : "用户" + userId;
             * notificationService.sendNotificationAsync(
             * postAuthorId,
             * "favorite",
             * String.format("%s收藏了你的帖子", favoriterName),
             * postId
             * );
             * }
             * }
             * } catch (Exception e) {
             * // 通知发送失败不影响主流程
             * log.error("Failed to send favorite notification: user={}, post={}", userId,
             * postId, e);
             * }
             */

            return true;
        }
        log.debug("user {} already favorites post {}", userId, postId);
        return false;
    }

    @Override
    public boolean unfavoritePost(Long userId, Long postId) {
        if (userId == null || postId == null) {
            throw new IllegalArgumentException("user id or post id is null");
        }

        // 使用 Lua 脚本原子性地执行取消收藏操作
        List<String> keys = new ArrayList<>();
        keys.add(USER_FAVORITE_PREFIX + userId);
        keys.add(POST_FAVORITED_BY_PREFIX + postId);
        keys.add(POST_FAVORITE_COUNT_PREFIX + postId);

        Long removed = relationScripts.executeScript(RelationScripts.UNFAVORITE_POST, keys, userId, postId);

        if (removed != null && removed > 0) {
            log.info("user {} unfavorites post {}", userId, postId);

            // Write-Behind: 异步持久化到数据库
            persistenceService.persistFavorite(userId, postId, false)
                    .exceptionally(ex -> {
                        log.error("Failed to persist unfavorite to DB (async), user {} unfavorites post {}", userId,
                                postId, ex);
                        return null;
                    });

            // 新增：更新排行榜和热度分数
            try {
                rankingService.onUnfavorite(postId);
                hotRankingService.onUnfavorite(postId);
                log.debug("Updated ranking and hot score for post {} due to unfavorite", postId);
            } catch (Exception e) {
                log.error("Failed to update ranking/hot score for post {}", postId, e);
                // 不影响主流程，继续执行
            }

            // 记录取消收藏统计
            try {
                statisticsService.recordFavorite(postId, false);
            } catch (Exception e) {
                log.error("Failed to record unfavorite statistics: postId={}", postId, e);
            }

            return true;
        }
        log.debug("user {} already unfavorites post {}", userId, postId);
        return false;
    }

    @Override
    public boolean isFavorited(Long userId, Long postId) {
        if (userId == null || postId == null) {
            return false;
        }
        return Boolean.TRUE.equals(
                boundUtil.sIsMember(USER_FAVORITE_PREFIX + userId, postId));
    }

    @Override
    public Set<Long> getUserFavorites(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        return boundUtil.sMembers(USER_FAVORITE_PREFIX + userId, Long.class);
    }

    @Override
    public Long getFavoriteCount(Long postId) {
        if (postId == null) {
            return 0L;
        }
        // 优先从 Set 获取准确计数
        return boundUtil.sCard(POST_FAVORITED_BY_PREFIX + postId);
    }

    @Override
    public Map<Long, Boolean> batchCheckFavorited(Long userId, List<Long> postIds) {
        if (userId == null || postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Boolean> result = new HashMap<>();
        for (Long postId : postIds) {
            result.put(postId, isFavorited(userId, postId));
        }
        return result;
    }

    // ==================== 黑名单相关 ====================

    @Override
    public boolean blockUser(Long userId, Long blockedUserId) {
        if (userId == null || blockedUserId == null) {
            throw new IllegalArgumentException("user id or blocked user id is null");
        }
        if (userId.equals(blockedUserId)) {
            log.warn("user {} cannot block itself", userId);
            return false;
        }

        // 使用 Lua 脚本原子性地执行拉黑操作
        List<String> keys = new ArrayList<>();
        keys.add(USER_BLACKLIST_PREFIX + userId);
        keys.add(USER_BLOCKED_BY_PREFIX + blockedUserId);

        Long added = relationScripts.executeScript(RelationScripts.BLOCK_USER, keys, blockedUserId, userId);

        if (added != null && added > 0) {
            log.info("user {} blocks user {}", userId, blockedUserId);

            // Write-Through: 立即持久化到数据库
            try {
                persistenceService.persistBlock(userId, blockedUserId, true);
            } catch (Exception e) {
                log.error("Failed to persist block to DB, but Redis operation succeeded", e);
            }

            return true;
        }
        log.debug("user {} already blocks user {}", userId, blockedUserId);
        return false;
    }

    @Override
    public boolean unblockUser(Long userId, Long blockedUserId) {
        if (userId == null || blockedUserId == null) {
            throw new IllegalArgumentException("user id or blocked user id is null");
        }

        // 使用 Lua 脚本原子性地执行取消拉黑操作
        List<String> keys = new ArrayList<>();
        keys.add(USER_BLACKLIST_PREFIX + userId);
        keys.add(USER_BLOCKED_BY_PREFIX + blockedUserId);

        Long removed = relationScripts.executeScript(RelationScripts.UNBLOCK_USER, keys, blockedUserId, userId);

        if (removed != null && removed > 0) {
            log.info("user {} unblocks user {}", userId, blockedUserId);

            // Write-Through: 立即持久化到数据库
            try {
                persistenceService.persistBlock(userId, blockedUserId, false);
            } catch (Exception e) {
                log.error("Failed to persist unblock to DB, but Redis operation succeeded", e);
            }

            return true;
        }
        log.debug("user {} already unblocks user {}", userId, blockedUserId);
        return false;
    }

    @Override
    public boolean isBlocked(Long userId, Long blockedUserId) {
        if (userId == null || blockedUserId == null) {
            return false;
        }
        return Boolean.TRUE.equals(
                boundUtil.sIsMember(USER_BLACKLIST_PREFIX + userId, blockedUserId));
    }

    @Override
    public Set<Long> getBlacklist(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        return boundUtil.sMembers(USER_BLACKLIST_PREFIX + userId, Long.class);
    }

    @Override
    public List<Long> filterBlacklisted(Long userId, List<Long> userIds) {
        if (userId == null || userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> blacklist = getBlacklist(userId);
        if (blacklist.isEmpty()) {
            return new ArrayList<>(userIds);
        }

        return userIds.stream()
                .filter(id -> !blacklist.contains(id))
                .collect(Collectors.toList());
    }
}
