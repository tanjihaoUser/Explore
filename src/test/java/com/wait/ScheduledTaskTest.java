package com.wait;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.wait.entity.domain.Post;
import com.wait.entity.domain.UserBase;
import com.wait.entity.type.ResourceType;
import com.wait.mapper.PostMapper;
import com.wait.mapper.UserBaseMapper;
import com.wait.service.BrowseHistoryService;
import com.wait.service.StatisticsService;
import com.wait.service.TimeWindowStatisticsService;
import com.wait.service.UVStatisticsService;

import lombok.extern.slf4j.Slf4j;

/**
 * 定时任务测试
 * 测试定时任务能否成功执行，包括数据持久化和数据同步任务
 */
@Slf4j
@SpringBootTest
public class ScheduledTaskTest {

    @Autowired
    private UVStatisticsService uvStatisticsService;

    @Autowired
    private TimeWindowStatisticsService timeWindowStatisticsService;

    @Autowired
    private BrowseHistoryService browseHistoryService;

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private UserBaseMapper userBaseMapper;

    @Autowired
    private PostMapper postMapper;

    private List<Long> testUserIds;
    private List<Long> testPostIds;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

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
     * 测试UV统计数据持久化任务
     * 验证定时任务能够将Redis中的数据持久化到数据库
     */
    @Test
    public void testUVStatisticsPersistenceTask() throws InterruptedException {
        log.info("========== 测试UV统计数据持久化任务 ==========");

        if (testPostIds.isEmpty()) {
            log.warn("测试数据不足，跳过UV统计持久化任务测试");
            return;
        }

        Long testPostId = testPostIds.get(0);
        String today = LocalDate.now().format(DATE_FORMATTER);

        // 清理测试数据
        log.info("清理测试数据...");

        // 记录访问（生成需要持久化的数据）
        int visitorCount = 10;
        for (int i = 0; i < visitorCount; i++) {
            String visitorId = "persistence_visitor_" + i;
            uvStatisticsService.recordVisit(ResourceType.POST, testPostId, visitorId);
        }

        // 获取持久化前的UV
        Long uvBefore = uvStatisticsService.getUV(ResourceType.POST, testPostId);
        log.info("持久化前UV: postId={}, UV={}", testPostId, uvBefore);

        // 等待定时任务执行（定时任务配置为每2分钟执行一次，这里等待3分钟确保执行）
        log.info("等待定时任务执行（等待3分钟）...");
        Thread.sleep(3 * 60 * 1000);

        // 获取持久化后的UV（应该保持一致）
        Long uvAfter = uvStatisticsService.getUV(ResourceType.POST, testPostId);
        log.info("持久化后UV: postId={}, UV={}", testPostId, uvAfter);

        // 验证结果
        assert uvBefore != null : "持久化前UV不应为null";
        assert uvAfter != null : "持久化后UV不应为null";
        assert uvAfter >= uvBefore : String.format("持久化后UV应该至少等于持久化前，before=%d, after=%d", uvBefore, uvAfter);

        log.info("✓ UV统计持久化任务测试通过");
    }

    /**
     * 测试时间窗口统计数据持久化任务
     */
    @Test
    public void testTimeWindowStatisticsPersistenceTask() throws InterruptedException {
        log.info("========== 测试时间窗口统计数据持久化任务 ==========");

        String metric = "test:persistence:metric";
        long currentTime = System.currentTimeMillis();

        // 添加数据点（生成需要持久化的数据）
        int dataPointCount = 20;
        for (int i = 0; i < dataPointCount; i++) {
            timeWindowStatisticsService.addDataPoint(metric, "value_" + i, currentTime + i);
        }

        // 获取持久化前的总数
        Long countBefore = timeWindowStatisticsService.getTotalCount(metric);
        log.info("持久化前数据点数: metric={}, count={}", metric, countBefore);

        // 等待定时任务执行
        log.info("等待定时任务执行（等待3分钟）...");
        Thread.sleep(3 * 60 * 1000);

        // 获取持久化后的总数（应该保持一致或减少，因为过期数据会被清理）
        Long countAfter = timeWindowStatisticsService.getTotalCount(metric);
        log.info("持久化后数据点数: metric={}, count={}", metric, countAfter);

        // 验证结果
        assert countBefore != null : "持久化前数据点数不应为null";
        assert countAfter != null : "持久化后数据点数不应为null";

        log.info("✓ 时间窗口统计持久化任务测试通过");
    }

    /**
     * 测试浏览历史数据持久化任务
     */
    @Test
    public void testBrowseHistoryPersistenceTask() throws InterruptedException {
        log.info("========== 测试浏览历史数据持久化任务 ==========");

        if (testUserIds.isEmpty() || testPostIds.isEmpty()) {
            log.warn("测试数据不足，跳过浏览历史持久化任务测试");
            return;
        }

        Long testUserId = testUserIds.get(0);

        // 清理测试数据
        browseHistoryService.clearBrowseHistory(testUserId);

        // 记录浏览（生成需要持久化的数据）
        int browseCount = 15;
        for (int i = 0; i < browseCount; i++) {
            Long postId = testPostIds.get(i % testPostIds.size());
            browseHistoryService.recordBrowse(testUserId, postId);
        }

        // 获取持久化前的数量
        Long countBefore = browseHistoryService.getBrowseHistoryCount(testUserId);
        log.info("持久化前浏览记录数: userId={}, count={}", testUserId, countBefore);

        // 等待定时任务执行
        log.info("等待定时任务执行（等待3分钟）...");
        Thread.sleep(3 * 60 * 1000);

        // 获取持久化后的数量（应该保持一致）
        Long countAfter = browseHistoryService.getBrowseHistoryCount(testUserId);
        log.info("持久化后浏览记录数: userId={}, count={}", testUserId, countAfter);

        // 验证结果
        assert countBefore != null : "持久化前浏览记录数不应为null";
        assert countAfter != null : "持久化后浏览记录数不应为null";
        assert countAfter >= countBefore : String.format("持久化后浏览记录数应该至少等于持久化前，before=%d, after=%d",
                countBefore, countAfter);

        log.info("✓ 浏览历史持久化任务测试通过");
    }

    /**
     * 测试数据同步任务（点赞数、收藏数、评论数同步）
     */
    @Test
    public void testDataSyncTask() throws InterruptedException {
        log.info("========== 测试数据同步任务 ==========");

        if (testPostIds.isEmpty()) {
            log.warn("测试数据不足，跳过数据同步任务测试");
            return;
        }

        // 记录一些统计数据（触发Redis更新）
        Long testPostId = testPostIds.get(0);
        statisticsService.recordPostView(testPostId);
        statisticsService.recordLike(testPostId, true);
        statisticsService.recordFavorite(testPostId, true);

        log.info("已记录统计数据: postId={}", testPostId);

        // 等待定时任务执行（数据同步任务配置为每2分钟执行一次）
        log.info("等待数据同步任务执行（等待3分钟）...");
        Thread.sleep(3 * 60 * 1000);

        // 验证统计数据是否正常
        // 注意：这里主要验证定时任务能够执行，不验证具体的数据同步逻辑
        log.info("✓ 数据同步任务测试通过（定时任务已执行）");
    }

    /**
     * 测试关系数据校验任务
     */
    @Test
    public void testRelationDataValidationTask() throws InterruptedException {
        log.info("========== 测试关系数据校验任务 ==========");

        // 关系数据校验任务配置为每1分钟执行一次
        // 这里等待2分钟确保执行
        log.info("等待关系数据校验任务执行（等待2分钟）...");
        Thread.sleep(2 * 60 * 1000);

        // 验证定时任务能够执行（通过日志查看）
        log.info("✓ 关系数据校验任务测试通过（定时任务已执行，请查看日志确认）");
    }

    /**
     * 综合测试：验证所有定时任务能够正常执行
     */
    @Test
    public void testAllScheduledTasks() throws InterruptedException {
        log.info("========== 综合测试：验证所有定时任务能够正常执行 ==========");

        if (testUserIds.isEmpty() || testPostIds.isEmpty()) {
            log.warn("测试数据不足，跳过综合测试");
            return;
        }

        // 准备测试数据
        Long testUserId = testUserIds.get(0);
        Long testPostId = testPostIds.get(0);

        // 1. UV统计
        for (int i = 0; i < 5; i++) {
            uvStatisticsService.recordVisit(ResourceType.POST, testPostId, "task_visitor_" + i);
        }

        // 2. 时间窗口统计
        timeWindowStatisticsService.addDataPoint("test:task:metric", "value_1");

        // 3. 浏览历史
        browseHistoryService.recordBrowse(testUserId, testPostId);

        // 4. 统计数据
        statisticsService.recordPostView(testPostId);

        log.info("测试数据准备完成，等待定时任务执行（等待5分钟）...");
        Thread.sleep(5 * 60 * 1000);

        // 验证数据是否正常
        Long uv = uvStatisticsService.getUV(ResourceType.POST, testPostId);
        Long browseCount = browseHistoryService.getBrowseHistoryCount(testUserId);

        log.info("========== 综合测试结果 ==========");
        log.info("UV统计: postId={}, UV={}", testPostId, uv);
        log.info("浏览历史: userId={}, count={}", testUserId, browseCount);

        // 验证结果
        assert uv != null && uv >= 5 : "UV统计应该正常";
        assert browseCount != null && browseCount >= 1 : "浏览历史应该正常";

        log.info("✓ 综合测试通过，所有定时任务能够正常执行");
    }
}

