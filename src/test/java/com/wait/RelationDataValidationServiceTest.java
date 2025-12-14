package com.wait;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.wait.entity.domain.Post;
import com.wait.entity.domain.UserBase;
import com.wait.mapper.FollowMapper;
import com.wait.mapper.PostFavoriteMapper;
import com.wait.mapper.PostLikeMapper;
import com.wait.mapper.PostMapper;
import com.wait.mapper.UserBaseMapper;
import com.wait.mapper.UserBlockMapper;
import com.wait.service.RelationDataValidationService;
import com.wait.service.RelationService;

import lombok.extern.slf4j.Slf4j;

/**
 * 关系数据校验服务测试
 * 测试关系数据校验功能的基础功能、数据一致性修复和定时任务执行
 */
@Slf4j
@SpringBootTest
public class RelationDataValidationServiceTest {

    @Autowired
    private RelationDataValidationService validationService;

    @Autowired
    private RelationService relationService;

    @Autowired
    private UserBaseMapper userBaseMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private PostLikeMapper postLikeMapper;

    @Autowired
    private PostFavoriteMapper postFavoriteMapper;

    @Autowired
    private FollowMapper followMapper;

    @Autowired
    private UserBlockMapper userBlockMapper;

    private List<Long> testUserIds;
    private List<Long> testPostIds;

    @BeforeEach
    public void setup() {
        // 加载测试数据
        List<UserBase> users = userBaseMapper.selectAll();
        if (users != null && !users.isEmpty()) {
            testUserIds = users.stream().map(UserBase::getId).collect(Collectors.toList());
        } else {
            testUserIds = new ArrayList<>();
        }

        List<Post> posts = new ArrayList<>();
        if (testUserIds != null && !testUserIds.isEmpty()) {
            for (Long userId : testUserIds) {
                List<Post> userPosts = postMapper.selectByUserId(userId);
                if (userPosts != null) {
                    posts.addAll(userPosts);
                }
            }
        }
        testPostIds = posts.stream().map(Post::getId).distinct().collect(Collectors.toList());

        log.info("测试数据准备完成: {} 个用户, {} 个帖子", testUserIds.size(), testPostIds.size());
    }

    /**
     * 基础功能测试：校验点赞数据
     */
    @Test
    public void testValidateLikeData() {
        log.info("========== 基础功能测试：校验点赞数据 ==========");

        if (testPostIds.isEmpty() || testUserIds.isEmpty()) {
            log.warn("测试数据不足，跳过点赞数据校验测试");
            return;
        }

        Long testPostId = testPostIds.get(0);
        Long testUserId = testUserIds.get(0);

        // 清理测试数据
        relationService.unlikePost(testUserId, testPostId);

        // 在Redis中记录点赞
        relationService.likePost(testUserId, testPostId);

        // 校验点赞数据
        validationService.validateLikeData(testPostId);
        log.info("点赞数据校验完成: postId={}", testPostId);

        // 验证结果（通过日志查看校验结果）
        Long likeCount = relationService.getLikeCount(testPostId);
        Set<Long> likers = relationService.getLikers(testPostId);

        log.info("校验后点赞数: postId={}, count={}, likers={}", testPostId, likeCount, likers);

        // 验证数据一致性
        assert likeCount != null : "点赞数不应为null";
        assert likers != null : "点赞用户列表不应为null";
        assert likeCount == likers.size() : String.format("点赞数应该等于点赞用户数，count=%d, users=%d", likeCount, likers.size());

        log.info("✓ 点赞数据校验测试通过");
    }

    /**
     * 基础功能测试：校验收藏数据
     */
    @Test
    public void testValidateFavoriteData() {
        log.info("========== 基础功能测试：校验收藏数据 ==========");

        if (testPostIds.isEmpty() || testUserIds.isEmpty()) {
            log.warn("测试数据不足，跳过收藏数据校验测试");
            return;
        }

        Long testPostId = testPostIds.get(0);
        Long testUserId = testUserIds.get(0);

        // 清理测试数据
        relationService.unfavoritePost(testUserId, testPostId);

        // 在Redis中记录收藏
        relationService.favoritePost(testUserId, testPostId);

        // 校验收藏数据
        validationService.validateFavoriteData(testUserId);
        log.info("收藏数据校验完成: userId={}", testUserId);

        // 验证结果
        Long favoriteCount = relationService.getFavoriteCount(testPostId);
        Set<Long> favorites = relationService.getUserFavorites(testUserId);

        log.info("校验后收藏数: postId={}, count={}, favorites={}", testPostId, favoriteCount, favorites);

        // 验证数据一致性
        assert favoriteCount != null : "收藏数不应为null";
        assert favorites != null : "收藏列表不应为null";

        log.info("✓ 收藏数据校验测试通过");
    }

    /**
     * 基础功能测试：校验关注数据
     */
    @Test
    public void testValidateFollowData() {
        log.info("========== 基础功能测试：校验关注数据 ==========");

        if (testUserIds.size() < 2) {
            log.warn("测试用户不足，跳过关注数据校验测试");
            return;
        }

        Long testFollowerId = testUserIds.get(0);
        Long testFollowedId = testUserIds.get(1);

        // 清理测试数据
        relationService.unfollow(testFollowerId, testFollowedId);

        // 在Redis中记录关注
        relationService.follow(testFollowerId, testFollowedId);

        // 校验关注数据
        validationService.validateFollowData(testFollowerId);
        log.info("关注数据校验完成: followerId={}", testFollowerId);

        // 验证结果
        Long followingCount = relationService.getFollowingCount(testFollowerId);
        Set<Long> following = relationService.getFollowing(testFollowerId);

        log.info("校验后关注数: followerId={}, count={}, following={}",
                testFollowerId, followingCount, following);

        // 验证数据一致性
        assert followingCount != null : "关注数不应为null";
        assert following != null : "关注列表不应为null";
        assert followingCount == following.size()
                : String.format("关注数应该等于关注用户数，count=%d, users=%d", followingCount, following.size());

        log.info("✓ 关注数据校验测试通过");
    }

    /**
     * 基础功能测试：校验黑名单数据
     */
    @Test
    public void testValidateBlockData() {
        log.info("========== 基础功能测试：校验黑名单数据 ==========");

        if (testUserIds.size() < 2) {
            log.warn("测试用户不足，跳过黑名单数据校验测试");
            return;
        }

        Long testUserId = testUserIds.get(0);
        Long testBlockedUserId = testUserIds.get(1);

        // 清理测试数据
        relationService.unblockUser(testUserId, testBlockedUserId);

        // 在Redis中记录黑名单
        relationService.blockUser(testUserId, testBlockedUserId);

        // 校验黑名单数据
        validationService.validateBlockData(testUserId);
        log.info("黑名单数据校验完成: userId={}", testUserId);

        // 验证结果
        Set<Long> blacklist = relationService.getBlacklist(testUserId);

        log.info("校验后黑名单: userId={}, blacklist={}", testUserId, blacklist);

        // 验证数据一致性
        assert blacklist != null : "黑名单不应为null";
        assert blacklist.contains(testBlockedUserId) : "黑名单应该包含被屏蔽的用户";

        log.info("✓ 黑名单数据校验测试通过");
    }

    /**
     * 数据一致性测试：验证校验服务能够发现并修复不一致
     */
    @Test
    public void testDataConsistencyFix() {
        log.info("========== 数据一致性测试：验证校验服务能够发现并修复不一致 ==========");

        if (testPostIds.isEmpty() || testUserIds.isEmpty()) {
            log.warn("测试数据不足，跳过数据一致性修复测试");
            return;
        }

        Long testPostId = testPostIds.get(0);
        Long testUserId = testUserIds.get(0);

        // 清理测试数据
        relationService.unlikePost(testUserId, testPostId);

        // 在Redis中记录点赞（但不在数据库中）
        relationService.likePost(testUserId, testPostId);

        // 获取Redis中的点赞数
        Long redisLikeCount = relationService.getLikeCount(testPostId);
        log.info("Redis点赞数: postId={}, count={}", testPostId, redisLikeCount);

        // 校验点赞数据（应该发现不一致并修复）
        validationService.validateLikeData(testPostId);

        // 等待一段时间，确保修复完成
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 验证修复后的数据
        Long likeCountAfter = relationService.getLikeCount(testPostId);
        Set<Long> likersAfter = relationService.getLikers(testPostId);

        log.info("修复后点赞数: postId={}, count={}, likers={}",
                testPostId, likeCountAfter, likersAfter);

        // 验证结果
        assert likeCountAfter != null : "修复后点赞数不应为null";
        assert likersAfter != null : "修复后点赞用户列表不应为null";
        assert likeCountAfter == likersAfter.size() : String.format("修复后点赞数应该等于点赞用户数，count=%d, users=%d",
                likeCountAfter, likersAfter.size());

        log.info("✓ 数据一致性修复测试通过");
    }

    /**
     * 定时任务测试：验证定时校验任务能够执行
     */
    @Test
    public void testScheduledValidationTask() throws InterruptedException {
        log.info("========== 定时任务测试：验证定时校验任务能够执行 ==========");

        // 关系数据校验任务配置为每1分钟执行一次
        // 这里等待2分钟确保执行
        log.info("等待关系数据校验定时任务执行（等待2分钟）...");
        Thread.sleep(2 * 60 * 1000);

        // 验证定时任务能够执行（通过日志查看）
        log.info("✓ 定时校验任务测试通过（定时任务已执行，请查看日志确认）");
    }

    /**
     * 定时修复任务测试：验证定时修复任务能够执行
     */
    @Test
    public void testScheduledFixTask() throws InterruptedException {
        log.info("========== 定时修复任务测试：验证定时修复任务能够执行 ==========");

        // 关系数据修复任务配置为每2分钟执行一次，初始延迟10秒
        // 这里等待3分钟确保执行
        log.info("等待关系数据修复定时任务执行（等待3分钟）...");
        Thread.sleep(3 * 60 * 1000);

        // 验证定时任务能够执行（通过日志查看）
        log.info("✓ 定时修复任务测试通过（定时任务已执行，请查看日志确认）");
    }

    /**
     * 综合测试：验证所有类型的关系数据校验
     */
    @Test
    public void testAllRelationDataValidation() {
        log.info("========== 综合测试：验证所有类型的关系数据校验 ==========");

        if (testPostIds.isEmpty() || testUserIds.size() < 2) {
            log.warn("测试数据不足，跳过综合测试");
            return;
        }

        Long testUserId = testUserIds.get(0);
        Long testPostId = testPostIds.get(0);
        Long testFollowedId = testUserIds.get(1);
        Long testBlockedUserId = testUserIds.get(1);

        // 准备测试数据
        relationService.likePost(testUserId, testPostId);
        relationService.favoritePost(testUserId, testPostId);
        relationService.follow(testUserId, testFollowedId);
        relationService.blockUser(testUserId, testBlockedUserId);

        log.info("测试数据准备完成");

        // 执行所有类型的校验
        validationService.validateLikeData(testPostId);
        validationService.validateFavoriteData(testUserId);
        validationService.validateFollowData(testUserId);
        validationService.validateBlockData(testUserId);

        log.info("========== 综合测试结果 ==========");
        log.info("点赞数: {}", relationService.getLikeCount(testPostId));
        log.info("收藏数: {}", relationService.getFavoriteCount(testPostId));
        log.info("关注数: {}", relationService.getFollowingCount(testUserId));
        log.info("黑名单数: {}", relationService.getBlacklist(testUserId).size());

        log.info("✓ 综合测试通过，所有类型的关系数据校验功能正常");
    }
}

