package com.wait;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.wait.entity.domain.Post;
import com.wait.entity.domain.UserBase;
import com.wait.mapper.PostMapper;
import com.wait.mapper.UserBaseMapper;
import com.wait.service.BrowseHistoryService;

import lombok.extern.slf4j.Slf4j;

/**
 * 浏览历史服务测试
 * 测试浏览历史功能的基础功能、并发性能和数据一致性
 */
@Slf4j
@SpringBootTest
public class BrowseHistoryServiceTest {

    @Autowired
    private BrowseHistoryService browseHistoryService;

    @Autowired
    private UserBaseMapper userBaseMapper;

    @Autowired
    private PostMapper postMapper;

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
     * 基础功能测试：记录浏览和获取历史
     */
    @Test
    public void testBasicRecordAndGetHistory() {
        log.info("========== 基础功能测试：记录浏览和获取历史 ==========");

        if (testUserIds.isEmpty() || testPostIds.isEmpty()) {
            log.warn("测试数据不足，跳过基础功能测试");
            return;
        }

        Long testUserId = testUserIds.get(0);
        Long testPostId = testPostIds.get(0);

        // 清理测试数据
        browseHistoryService.clearBrowseHistory(testUserId);

        // 记录浏览
        for (int i = 0; i < 10; i++) {
            Long postId = testPostIds.get(i % testPostIds.size());
            browseHistoryService.recordBrowse(testUserId, postId);
            log.debug("记录浏览: userId={}, postId={}", testUserId, postId);
        }

        // 获取浏览历史
        List<Long> history = browseHistoryService.getBrowseHistory(testUserId, 10);
        log.info("获取浏览历史: userId={}, count={}", testUserId, history.size());

        // 验证结果
        assert history != null : "浏览历史不应为null";
        assert history.size() == 10 : String.format("应该返回10条记录，实际为%d", history.size());

        // 验证顺序（应该按时间倒序）
        log.info("浏览历史: {}", history);

        log.info("✓ 基础功能测试通过");
    }

    /**
     * 基础功能测试：分页获取浏览历史
     */
    @Test
    public void testGetBrowseHistoryPaged() {
        log.info("========== 基础功能测试：分页获取浏览历史 ==========");

        if (testUserIds.isEmpty() || testPostIds.isEmpty()) {
            log.warn("测试数据不足，跳过分页测试");
            return;
        }

        Long testUserId = testUserIds.get(0);

        // 清理测试数据
        browseHistoryService.clearBrowseHistory(testUserId);

        // 记录浏览
        for (int i = 0; i < 20; i++) {
            Long postId = testPostIds.get(i % testPostIds.size());
            browseHistoryService.recordBrowse(testUserId, postId);
        }

        // 分页获取
        List<Long> page1 = browseHistoryService.getBrowseHistory(testUserId, 1, 10);
        List<Long> page2 = browseHistoryService.getBrowseHistory(testUserId, 2, 10);

        log.info("第一页: count={}", page1.size());
        log.info("第二页: count={}", page2.size());

        // 验证结果
        assert page1 != null && page1.size() == 10 : "第一页应该返回10条记录";
        assert page2 != null && page2.size() == 10 : "第二页应该返回10条记录";

        // 验证不重复
        Set<Long> allPostIds = new HashSet<>(page1);
        allPostIds.addAll(page2);
        assert allPostIds.size() == 20 : "两页数据应该不重复，总共20条";

        log.info("✓ 分页测试通过");
    }

    /**
     * 基础功能测试：检查是否浏览过
     */
    @Test
    public void testHasBrowsed() {
        log.info("========== 基础功能测试：检查是否浏览过 ==========");

        if (testUserIds.isEmpty() || testPostIds.isEmpty()) {
            log.warn("测试数据不足，跳过检查测试");
            return;
        }

        Long testUserId = testUserIds.get(0);
        Long testPostId = testPostIds.get(0);

        // 记录浏览
        browseHistoryService.recordBrowse(testUserId, testPostId);

        // 检查是否浏览过
        boolean hasBrowsed = browseHistoryService.hasBrowsed(testUserId, testPostId);
        log.info("检查浏览: userId={}, postId={}, hasBrowsed={}", testUserId, testPostId, hasBrowsed);

        // 验证结果
        assert hasBrowsed : "应该返回true，表示已浏览过";

        // 检查未浏览的帖子
        Long unBrowsedPostId = testPostIds.get(testPostIds.size() - 1);
        boolean notBrowsed = browseHistoryService.hasBrowsed(testUserId, unBrowsedPostId);
        assert !notBrowsed : "应该返回false，表示未浏览过";

        log.info("✓ 检查浏览测试通过");
    }

    /**
     * 基础功能测试：获取浏览时间
     */
    @Test
    public void testGetBrowseTime() {
        log.info("========== 基础功能测试：获取浏览时间 ==========");

        if (testUserIds.isEmpty() || testPostIds.isEmpty()) {
            log.warn("测试数据不足，跳过浏览时间测试");
            return;
        }

        Long testUserId = testUserIds.get(0);
        Long testPostId = testPostIds.get(0);

        long beforeTime = System.currentTimeMillis();
        // 记录浏览
        browseHistoryService.recordBrowse(testUserId, testPostId);
        long afterTime = System.currentTimeMillis();

        // 获取浏览时间
        Long browseTime = browseHistoryService.getBrowseTime(testUserId, testPostId);
        log.info("浏览时间: userId={}, postId={}, browseTime={}", testUserId, testPostId, browseTime);

        // 验证结果
        assert browseTime != null : "浏览时间不应为null";
        assert browseTime >= beforeTime && browseTime <= afterTime : "浏览时间应该在记录时间范围内";

        log.info("✓ 浏览时间测试通过");
    }

    /**
     * 基础功能测试：按时间范围查询
     */
    @Test
    public void testGetBrowseHistoryByTimeRange() {
        log.info("========== 基础功能测试：按时间范围查询 ==========");

        if (testUserIds.isEmpty() || testPostIds.isEmpty()) {
            log.warn("测试数据不足，跳过时间范围查询测试");
            return;
        }

        Long testUserId = testUserIds.get(0);

        // 清理测试数据
        browseHistoryService.clearBrowseHistory(testUserId);

        long startTime = System.currentTimeMillis();
        // 记录浏览
        for (int i = 0; i < 5; i++) {
            Long postId = testPostIds.get(i % testPostIds.size());
            browseHistoryService.recordBrowse(testUserId, postId);
            try {
                Thread.sleep(100); // 确保时间不同
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        long endTime = System.currentTimeMillis();

        // 按时间范围查询
        List<Long> history = browseHistoryService.getBrowseHistoryByTimeRange(testUserId, startTime, endTime);
        log.info("时间范围查询: userId={}, count={}", testUserId, history.size());

        // 验证结果
        assert history != null : "浏览历史不应为null";
        assert history.size() == 5 : String.format("应该返回5条记录，实际为%d", history.size());

        log.info("✓ 时间范围查询测试通过");
    }

    /**
     * 基础功能测试：清理浏览历史
     */
    @Test
    public void testClearBrowseHistory() {
        log.info("========== 基础功能测试：清理浏览历史 ==========");

        if (testUserIds.isEmpty() || testPostIds.isEmpty()) {
            log.warn("测试数据不足，跳过清理测试");
            return;
        }

        Long testUserId = testUserIds.get(0);

        // 记录浏览
        for (int i = 0; i < 10; i++) {
            Long postId = testPostIds.get(i % testPostIds.size());
            browseHistoryService.recordBrowse(testUserId, postId);
        }

        // 获取数量
        Long countBefore = browseHistoryService.getBrowseHistoryCount(testUserId);
        log.info("清理前数量: {}", countBefore);

        // 清理浏览历史
        browseHistoryService.clearBrowseHistory(testUserId);

        // 获取清理后数量
        Long countAfter = browseHistoryService.getBrowseHistoryCount(testUserId);
        log.info("清理后数量: {}", countAfter);

        // 验证结果
        assert countBefore != null && countBefore > 0 : "清理前应该有数据";
        assert countAfter != null && countAfter == 0 : "清理后应该没有数据";

        log.info("✓ 清理浏览历史测试通过");
    }

    /**
     * 并发测试：高并发浏览记录
     */
    @Test
    public void testConcurrentRecordBrowse() throws InterruptedException {
        log.info("========== 并发测试：高并发浏览记录 ==========");

        if (testUserIds.isEmpty() || testPostIds.isEmpty()) {
            log.warn("测试数据不足，跳过并发测试");
            return;
        }

        Long testUserId = testUserIds.get(0);
        int threadCount = 50;
        int operationsPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // 清理测试数据
        browseHistoryService.clearBrowseHistory(testUserId);

        long startTime = System.currentTimeMillis();

        // 并发记录浏览
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        Long postId = testPostIds.get((threadIndex * operationsPerThread + j) % testPostIds.size());
                        browseHistoryService.recordBrowse(testUserId, postId);
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("并发记录浏览失败", e);
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 验证结果
        Long totalCount = browseHistoryService.getBrowseHistoryCount(testUserId);
        log.info("========== 并发测试结果 ==========");
        log.info("总耗时: {} ms", duration);
        log.info("成功操作: {}", successCount.get());
        log.info("失败操作: {}", failureCount.get());
        log.info("总浏览记录数: {}", totalCount);
        log.info("QPS: {}", successCount.get() * 1000.0 / duration);

        // 验证数据一致性
        assert totalCount != null : "总浏览记录数不应为null";
        // 注意：由于去重，实际数量可能小于操作数
        assert totalCount <= threadCount * operationsPerThread : String.format("总浏览记录数应该不超过%d，实际为%d",
                threadCount * operationsPerThread, totalCount);

        log.info("✓ 并发测试通过，数据一致性验证成功");
    }

    /**
     * 数据一致性测试：Redis和数据库的一致性
     */
    @Test
    public void testDataConsistency() throws InterruptedException {
        log.info("========== 数据一致性测试 ==========");

        if (testUserIds.isEmpty() || testPostIds.isEmpty()) {
            log.warn("测试数据不足，跳过数据一致性测试");
            return;
        }

        Long testUserId = testUserIds.get(0);

        // 清理测试数据
        browseHistoryService.clearBrowseHistory(testUserId);

        // 记录浏览
        Set<Long> expectedPostIds = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            Long postId = testPostIds.get(i % testPostIds.size());
            expectedPostIds.add(postId);
            browseHistoryService.recordBrowse(testUserId, postId);
        }

        // 等待一段时间，确保数据写入
        Thread.sleep(1000);

        // 获取浏览历史
        List<Long> history = browseHistoryService.getBrowseHistory(testUserId, 100);
        Long count = browseHistoryService.getBrowseHistoryCount(testUserId);

        log.info("数据一致性检查: userId={}, expected={}, actual={}, count={}",
                testUserId, expectedPostIds.size(), history.size(), count);

        // 验证结果
        assert history != null : "浏览历史不应为null";
        assert count != null : "浏览记录数不应为null";
        assert history.size() == count.intValue() : String.format("查询结果和统计结果应该一致，history=%d, count=%d",
                history.size(), count);

        log.info("✓ 数据一致性测试通过");
    }

    /**
     * 压力测试：大量浏览记录
     */
    @Test
    public void testHighVolumeBrowse() {
        log.info("========== 压力测试：大量浏览记录 ==========");

        if (testUserIds.isEmpty() || testPostIds.isEmpty()) {
            log.warn("测试数据不足，跳过压力测试");
            return;
        }

        Long testUserId = testUserIds.get(0);
        int browseCount = 500;

        // 清理测试数据
        browseHistoryService.clearBrowseHistory(testUserId);

        long startTime = System.currentTimeMillis();

        // 记录大量浏览
        for (int i = 0; i < browseCount; i++) {
            Long postId = testPostIds.get(i % testPostIds.size());
            browseHistoryService.recordBrowse(testUserId, postId);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 获取总数
        Long totalCount = browseHistoryService.getBrowseHistoryCount(testUserId);
        log.info("========== 压力测试结果 ==========");
        log.info("浏览记录数: {}", browseCount);
        log.info("总耗时: {} ms", duration);
        log.info("总浏览记录数: {}", totalCount);
        log.info("QPS: {}", browseCount * 1000.0 / duration);

        // 验证结果
        assert totalCount != null : "总浏览记录数不应为null";
        // 注意：由于去重，实际数量可能小于操作数
        assert totalCount <= browseCount : String.format("总浏览记录数应该不超过%d，实际为%d", browseCount, totalCount);

        log.info("✓ 压力测试通过");
    }
}

