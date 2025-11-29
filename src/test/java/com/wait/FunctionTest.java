package com.wait;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.wait.entity.domain.Post;
import com.wait.entity.domain.UserBase;
import com.wait.mapper.PostMapper;
import com.wait.mapper.UserBaseMapper;
import com.wait.service.HotRankingService;
import com.wait.service.RankingService;
import com.wait.service.RelationService;
import com.wait.service.TimelineSortedSetService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 功能测试 - 测试点赞、收藏、关注等核心功能
 */
@Slf4j
@SpringBootTest
public class FunctionTest {

    @Autowired
    private UserBaseMapper userBaseMapper;
    @Autowired
    private PostMapper postMapper;
    @Autowired
    private RelationService relationService;
    @Autowired
    private TimelineSortedSetService timelineService;
    @Autowired
    private RankingService rankingService;
    @Autowired
    private HotRankingService hotRankingService;

    private Long testUserId1;
    private Long testUserId2;
    private Long testPostId1;
    private Long testPostId2;

    @BeforeEach
    public void setup() {
        // 获取测试用户和帖子
        List<UserBase> users = userBaseMapper.selectAll();
        if (users != null && users.size() >= 2) {
            testUserId1 = users.get(0).getId();
            testUserId2 = users.get(1).getId();
        }

        List<Post> posts = new java.util.ArrayList<>();
        if (testUserId1 != null) {
            List<Post> userPosts = postMapper.selectByUserId(testUserId1);
            if (userPosts != null) {
                posts.addAll(userPosts);
            }
        }
        if (posts != null && !posts.isEmpty()) {
            testPostId1 = posts.get(0).getId();
            if (posts.size() > 1) {
                testPostId2 = posts.get(1).getId();
            }
        }

        log.info("测试数据准备完成: userId1={}, userId2={}, postId1={}, postId2={}",
                testUserId1, testUserId2, testPostId1, testPostId2);
    }

    @Test
    public void testFollowFunction() {
        log.info("========== 测试关注功能 ==========");
        
        if (testUserId1 == null || testUserId2 == null) {
            log.warn("测试用户不足，跳过关注功能测试");
            return;
        }

        try {
            // 1. 测试关注
            boolean followResult = relationService.follow(testUserId1, testUserId2);
            log.info("✓ 关注操作结果: {}", followResult);
            assert followResult : "关注操作应该成功";

            // 2. 验证关注关系
            boolean isFollowing = relationService.isFollowing(testUserId1, testUserId2);
            log.info("✓ 关注关系验证: {}", isFollowing);
            assert isFollowing : "应该存在关注关系";

            // 3. 验证关注列表
            Set<Long> following = relationService.getFollowing(testUserId1);
            log.info("✓ 关注列表: {}", following);
            assert following != null && following.contains(testUserId2) : "关注列表应包含目标用户";

            // 4. 验证粉丝列表
            Set<Long> followers = relationService.getFollowers(testUserId2);
            log.info("✓ 粉丝列表: {}", followers);
            assert followers != null && followers.contains(testUserId1) : "粉丝列表应包含关注者";

            // 5. 验证关注数
            Long followingCount = relationService.getFollowingCount(testUserId1);
            log.info("✓ 关注数: {}", followingCount);
            assert followingCount > 0 : "关注数应该大于0";

            // 6. 验证粉丝数
            Long followerCount = relationService.getFollowerCount(testUserId2);
            log.info("✓ 粉丝数: {}", followerCount);
            assert followerCount > 0 : "粉丝数应该大于0";

            // 7. 测试取消关注
            boolean unfollowResult = relationService.unfollow(testUserId1, testUserId2);
            log.info("✓ 取消关注操作结果: {}", unfollowResult);
            assert unfollowResult : "取消关注操作应该成功";

            // 8. 验证取消关注后关系
            boolean isFollowingAfter = relationService.isFollowing(testUserId1, testUserId2);
            log.info("✓ 取消关注后关系验证: {}", isFollowingAfter);
            assert !isFollowingAfter : "取消关注后应该不存在关注关系";

            log.info("✓ 关注功能测试通过");

        } catch (Exception e) {
            log.error("关注功能测试失败", e);
            throw new RuntimeException("关注功能测试失败", e);
        }
    }

    @Test
    public void testLikeFunction() {
        log.info("========== 测试点赞功能 ==========");
        
        if (testUserId1 == null || testPostId1 == null) {
            log.warn("测试数据不足，跳过点赞功能测试");
            return;
        }

        try {
            // 1. 测试点赞
            boolean likeResult = relationService.likePost(testUserId1, testPostId1);
            log.info("✓ 点赞操作结果: {}", likeResult);

            // 2. 验证点赞关系
            boolean isLiked = relationService.isLiked(testUserId1, testPostId1);
            log.info("✓ 点赞关系验证: {}", isLiked);
            assert isLiked : "应该存在点赞关系";

            // 3. 验证点赞列表
            Set<Long> likers = relationService.getLikers(testPostId1);
            log.info("✓ 点赞用户列表: {}", likers);
            assert likers != null && likers.contains(testUserId1) : "点赞列表应包含当前用户";

            // 4. 验证点赞数
            Long likeCount = relationService.getLikeCount(testPostId1);
            log.info("✓ 点赞数: {}", likeCount);
            assert likeCount > 0 : "点赞数应该大于0";

            // 5. 验证用户点赞的帖子列表
            Set<Long> userLikedPosts = relationService.getUserLikedPosts(testUserId1);
            log.info("✓ 用户点赞的帖子列表: {}", userLikedPosts);
            assert userLikedPosts != null && userLikedPosts.contains(testPostId1) : "用户点赞列表应包含该帖子";

            // 6. 测试批量检查点赞状态
            if (testPostId2 != null) {
                java.util.List<Long> postIds = new java.util.ArrayList<>();
                postIds.add(testPostId1);
                postIds.add(testPostId2);
                Map<Long, Boolean> batchCheck = relationService.batchCheckLiked(testUserId1, postIds);
                log.info("✓ 批量检查点赞状态: {}", batchCheck);
                assert batchCheck.get(testPostId1) : "批量检查应该返回正确的点赞状态";
            }

            // 7. 测试取消点赞
            boolean unlikeResult = relationService.unlikePost(testUserId1, testPostId1);
            log.info("✓ 取消点赞操作结果: {}", unlikeResult);
            assert unlikeResult : "取消点赞操作应该成功";

            // 8. 验证取消点赞后关系
            boolean isLikedAfter = relationService.isLiked(testUserId1, testPostId1);
            log.info("✓ 取消点赞后关系验证: {}", isLikedAfter);
            assert !isLikedAfter : "取消点赞后应该不存在点赞关系";

            log.info("✓ 点赞功能测试通过");

        } catch (Exception e) {
            log.error("点赞功能测试失败", e);
            throw new RuntimeException("点赞功能测试失败", e);
        }
    }

    @Test
    public void testFavoriteFunction() {
        log.info("========== 测试收藏功能 ==========");
        
        if (testUserId1 == null || testPostId1 == null) {
            log.warn("测试数据不足，跳过收藏功能测试");
            return;
        }

        try {
            // 1. 测试收藏
            boolean favoriteResult = relationService.favoritePost(testUserId1, testPostId1);
            log.info("✓ 收藏操作结果: {}", favoriteResult);

            // 2. 验证收藏关系
            boolean isFavorited = relationService.isFavorited(testUserId1, testPostId1);
            log.info("✓ 收藏关系验证: {}", isFavorited);
            assert isFavorited : "应该存在收藏关系";

            // 3. 验证用户收藏列表
            Set<Long> userFavorites = relationService.getUserFavorites(testUserId1);
            log.info("✓ 用户收藏列表: {}", userFavorites);
            assert userFavorites != null && userFavorites.contains(testPostId1) : "用户收藏列表应包含该帖子";

            // 4. 验证收藏数
            Long favoriteCount = relationService.getFavoriteCount(testPostId1);
            log.info("✓ 收藏数: {}", favoriteCount);
            assert favoriteCount > 0 : "收藏数应该大于0";

            // 5. 测试批量检查收藏状态
            if (testPostId2 != null) {
                java.util.List<Long> postIds = new java.util.ArrayList<>();
                postIds.add(testPostId1);
                postIds.add(testPostId2);
                Map<Long, Boolean> batchCheck = relationService.batchCheckFavorited(testUserId1, postIds);
                log.info("✓ 批量检查收藏状态: {}", batchCheck);
                assert batchCheck.get(testPostId1) : "批量检查应该返回正确的收藏状态";
            }

            // 6. 测试取消收藏
            boolean unfavoriteResult = relationService.unfavoritePost(testUserId1, testPostId1);
            log.info("✓ 取消收藏操作结果: {}", unfavoriteResult);
            assert unfavoriteResult : "取消收藏操作应该成功";

            // 7. 验证取消收藏后关系
            boolean isFavoritedAfter = relationService.isFavorited(testUserId1, testPostId1);
            log.info("✓ 取消收藏后关系验证: {}", isFavoritedAfter);
            assert !isFavoritedAfter : "取消收藏后应该不存在收藏关系";

            log.info("✓ 收藏功能测试通过");

        } catch (Exception e) {
            log.error("收藏功能测试失败", e);
            throw new RuntimeException("收藏功能测试失败", e);
        }
    }

    @Test
    public void testBlockFunction() {
        log.info("========== 测试黑名单功能 ==========");
        
        if (testUserId1 == null || testUserId2 == null) {
            log.warn("测试用户不足，跳过黑名单功能测试");
            return;
        }

        try {
            // 1. 测试拉黑
            boolean blockResult = relationService.blockUser(testUserId1, testUserId2);
            log.info("✓ 拉黑操作结果: {}", blockResult);
            assert blockResult : "拉黑操作应该成功";

            // 2. 验证黑名单关系
            boolean isBlocked = relationService.isBlocked(testUserId1, testUserId2);
            log.info("✓ 黑名单关系验证: {}", isBlocked);
            assert isBlocked : "应该存在黑名单关系";

            // 3. 验证黑名单列表
            Set<Long> blacklist = relationService.getBlacklist(testUserId1);
            log.info("✓ 黑名单列表: {}", blacklist);
            assert blacklist != null && blacklist.contains(testUserId2) : "黑名单应包含目标用户";

            // 4. 测试过滤黑名单
            java.util.List<Long> userIds = new java.util.ArrayList<>();
            userIds.add(testUserId2);
            userIds.add(999L);
            List<Long> filtered = relationService.filterBlacklisted(testUserId1, userIds);
            log.info("✓ 过滤黑名单结果: {}", filtered);
            assert !filtered.contains(testUserId2) : "过滤后不应包含黑名单用户";

            // 5. 测试取消拉黑
            boolean unblockResult = relationService.unblockUser(testUserId1, testUserId2);
            log.info("✓ 取消拉黑操作结果: {}", unblockResult);
            assert unblockResult : "取消拉黑操作应该成功";

            // 6. 验证取消拉黑后关系
            boolean isBlockedAfter = relationService.isBlocked(testUserId1, testUserId2);
            log.info("✓ 取消拉黑后关系验证: {}", isBlockedAfter);
            assert !isBlockedAfter : "取消拉黑后应该不存在黑名单关系";

            log.info("✓ 黑名单功能测试通过");

        } catch (Exception e) {
            log.error("黑名单功能测试失败", e);
            throw new RuntimeException("黑名单功能测试失败", e);
        }
    }

    @Test
    public void testTimelineFunction() {
        log.info("========== 测试时间线功能 ==========");
        
        if (testUserId1 == null || testPostId1 == null) {
            log.warn("测试数据不足，跳过时间线功能测试");
            return;
        }

        try {
            // 1. 测试发布到时间线
            Post post = postMapper.selectById(testPostId1);
            if (post != null && post.getCreatedAt() != null) {
                long publishTime = post.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                timelineService.publishToTimeline(post.getUserId(), post.getId(), publishTime);
                log.info("✓ 帖子已发布到时间线: postId={}, time={}", post.getId(), publishTime);
            }

            // 2. 测试获取用户时间线
            List<Long> userTimeline = timelineService.getUserTimeline(testUserId1, 1, 10);
            log.info("✓ 用户时间线: {}", userTimeline);
            assert userTimeline != null : "用户时间线不应为null";

            // 3. 测试获取全局时间线
            List<Long> globalTimeline = timelineService.getGlobalTimeline(1, 10);
            log.info("✓ 全局时间线: {}", globalTimeline);
            assert globalTimeline != null : "全局时间线不应为null";

            // 4. 测试获取我的时间线（关注用户的时间线）
            if (testUserId2 != null) {
                relationService.follow(testUserId1, testUserId2);
                List<Long> myTimeline = timelineService.getMyTimeline(testUserId1, 1, 10);
                log.info("✓ 我的时间线: {}", myTimeline);
                assert myTimeline != null : "我的时间线不应为null";
                relationService.unfollow(testUserId1, testUserId2);
            }

            log.info("✓ 时间线功能测试通过");

        } catch (Exception e) {
            log.error("时间线功能测试失败", e);
            throw new RuntimeException("时间线功能测试失败", e);
        }
    }

    @Test
    public void testRankingFunction() {
        log.info("========== 测试排行榜功能 ==========");
        
        if (testPostId1 == null) {
            log.warn("测试数据不足，跳过排行榜功能测试");
            return;
        }

        try {
            // 1. 测试点赞排行榜
            List<Long> likesRanking = rankingService.getLikesRanking(1, 10);
            log.info("✓ 点赞排行榜: {}", likesRanking);
            assert likesRanking != null : "点赞排行榜不应为null";

            // 2. 测试收藏排行榜
            List<Long> favoritesRanking = rankingService.getFavoritesRanking(1, 10);
            log.info("✓ 收藏排行榜: {}", favoritesRanking);
            assert favoritesRanking != null : "收藏排行榜不应为null";

            // 3. 测试评论排行榜
            List<Long> commentsRanking = rankingService.getCommentsRanking(1, 10);
            log.info("✓ 评论排行榜: {}", commentsRanking);
            assert commentsRanking != null : "评论排行榜不应为null";

            // 4. 测试热度排行榜
            List<Long> hotPosts = hotRankingService.getHotPosts("alltime", 1, 10);
            log.info("✓ 热度排行榜: {}", hotPosts);
            assert hotPosts != null : "热度排行榜不应为null";

            // 5. 测试获取帖子排名
            Long rank = hotRankingService.getPostRank(testPostId1, "alltime");
            log.info("✓ 帖子排名: {}", rank);

            // 6. 测试获取热度分数
            Double hotScore = hotRankingService.getHotScore(testPostId1, "alltime");
            log.info("✓ 热度分数: {}", hotScore);

            log.info("✓ 排行榜功能测试通过");

        } catch (Exception e) {
            log.error("排行榜功能测试失败", e);
            throw new RuntimeException("排行榜功能测试失败", e);
        }
    }
}

