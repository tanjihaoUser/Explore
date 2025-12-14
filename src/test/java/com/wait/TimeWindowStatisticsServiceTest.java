package com.wait;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import com.wait.service.TimeWindowStatisticsService;

import lombok.extern.slf4j.Slf4j;

/**
 * 时间窗口统计服务测试
 * 测试时间窗口统计功能的基础功能、并发性能和数据一致性
 */
@Slf4j
@SpringBootTest
public class TimeWindowStatisticsServiceTest {

    @Autowired
    private TimeWindowStatisticsService timeWindowStatisticsService;

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
     * 基础功能测试：添加数据点和查询
     */
    @Test
    public void testBasicAddAndQuery() {
        log.info("========== 基础功能测试：添加数据点和查询 ==========");

        String metric = "test:metric:basic";
        long currentTime = System.currentTimeMillis();

        // 添加数据点
        for (int i = 0; i < 10; i++) {
            Boolean added = timeWindowStatisticsService.addDataPoint(metric, String.valueOf(i), currentTime + i);
            log.debug("添加数据点: metric={}, value={}, timestamp={}, added={}",
                    metric, i, currentTime + i, added);
        }

        // 查询数据点
        long startTime = currentTime - 1000;
        long endTime = currentTime + 10000;
        List<String> dataPoints = timeWindowStatisticsService.getDataPoints(metric, startTime, endTime);
        log.info("查询数据点: metric={}, count={}", metric, dataPoints.size());

        // 验证结果
        assert dataPoints != null : "数据点列表不应为null";
        assert dataPoints.size() == 10 : String.format("应该返回10个数据点，实际为%d", dataPoints.size());

        // 统计数量
        Long count = timeWindowStatisticsService.countDataPoints(metric, startTime, endTime);
        assert count != null && count == 10 : String.format("统计数量应该为10，实际为%d", count);

        log.info("✓ 基础功能测试通过");
    }

    /**
     * 基础功能测试：获取最近N天的数据
     */
    @Test
    public void testGetRecentDataPoints() {
        log.info("========== 基础功能测试：获取最近N天的数据 ==========");

        String metric = "test:metric:recent";
        long currentTime = System.currentTimeMillis();

        // 添加不同时间的数据点
        for (int i = 0; i < 5; i++) {
            long timestamp = currentTime - (i * 24 * 60 * 60 * 1000L); // 每天一个
            timeWindowStatisticsService.addDataPoint(metric, "value_" + i, timestamp);
        }

        // 获取最近3天的数据
        List<String> recentData = timeWindowStatisticsService.getRecentDataPoints(metric, 3);
        log.info("最近3天数据: metric={}, count={}", metric, recentData.size());

        // 验证结果（应该包含最近3天的数据）
        assert recentData != null : "数据点列表不应为null";
        assert recentData.size() >= 3 : String.format("应该至少返回3个数据点，实际为%d", recentData.size());

        log.info("✓ 获取最近数据测试通过");
    }

    /**
     * 基础功能测试：获取最近N小时的数据
     */
    @Test
    public void testGetRecentDataPointsByHours() {
        log.info("========== 基础功能测试：获取最近N小时的数据 ==========");

        String metric = "test:metric:hours";
        long currentTime = System.currentTimeMillis();

        // 添加不同时间的数据点（每小时一个）
        for (int i = 0; i < 5; i++) {
            long timestamp = currentTime - (i * 60 * 60 * 1000L);
            timeWindowStatisticsService.addDataPoint(metric, "value_" + i, timestamp);
        }

        // 获取最近3小时的数据
        List<String> recentData = timeWindowStatisticsService.getRecentDataPointsByHours(metric, 3);
        log.info("最近3小时数据: metric={}, count={}", metric, recentData.size());

        // 验证结果
        assert recentData != null : "数据点列表不应为null";
        assert recentData.size() >= 3 : String.format("应该至少返回3个数据点，实际为%d", recentData.size());

        log.info("✓ 获取最近小时数据测试通过");
    }

    /**
     * 基础功能测试：统计计算
     */
    @Test
    public void testCalculateStatistics() {
        log.info("========== 基础功能测试：统计计算 ==========");

        String metric = "test:metric:stats";
        long currentTime = System.currentTimeMillis();
        long startTime = currentTime - 1000;
        long endTime = currentTime + 10000;

        // 添加数值数据点
        double[] values = { 10.5, 20.3, 15.7, 30.2, 25.8 };
        for (int i = 0; i < values.length; i++) {
            timeWindowStatisticsService.addDataPoint(metric, String.valueOf(values[i]), currentTime + i);
        }

        // 计算统计值
        Map<String, Double> stats = timeWindowStatisticsService.calculateStatistics(metric, startTime, endTime);
        log.info("统计结果: metric={}, stats={}", metric, stats);

        // 验证结果
        assert stats != null : "统计结果不应为null";
        assert stats.containsKey("sum") : "应该包含sum";
        assert stats.containsKey("avg") : "应该包含avg";
        assert stats.containsKey("max") : "应该包含max";
        assert stats.containsKey("min") : "应该包含min";
        assert stats.containsKey("count") : "应该包含count";

        double expectedSum = 10.5 + 20.3 + 15.7 + 30.2 + 25.8;
        double actualSum = stats.get("sum");
        assert Math.abs(actualSum - expectedSum) < 0.01 : String.format("sum应该等于%.2f，实际为%.2f", expectedSum, actualSum);

        log.info("✓ 统计计算测试通过");
    }

    /**
     * 基础功能测试：清理过期数据
     */
    @Test
    public void testCleanExpiredData() {
        log.info("========== 基础功能测试：清理过期数据 ==========");

        String metric = "test:metric:clean";
        long currentTime = System.currentTimeMillis();

        // 添加过期和未过期的数据
        long expireTime = currentTime - (2 * 24 * 60 * 60 * 1000L); // 2天前
        for (int i = 0; i < 5; i++) {
            long timestamp = currentTime - (i * 24 * 60 * 60 * 1000L);
            timeWindowStatisticsService.addDataPoint(metric, "value_" + i, timestamp);
        }

        // 获取总数
        Long totalBefore = timeWindowStatisticsService.getTotalCount(metric);
        log.info("清理前总数: {}", totalBefore);

        // 清理过期数据
        Long deletedCount = timeWindowStatisticsService.cleanExpiredData(metric, expireTime);
        log.info("清理过期数据: deleted={}", deletedCount);

        // 获取清理后总数
        Long totalAfter = timeWindowStatisticsService.getTotalCount(metric);
        log.info("清理后总数: {}", totalAfter);

        // 验证结果
        assert deletedCount != null && deletedCount > 0 : "应该删除一些过期数据";
        assert totalAfter != null && totalAfter < totalBefore
                : String.format("清理后总数应该小于清理前，before=%d, after=%d", totalBefore, totalAfter);

        log.info("✓ 清理过期数据测试通过");
    }

    /**
     * 并发测试：高并发添加数据点
     */
    @Test
    public void testConcurrentAddDataPoint() throws InterruptedException {
        log.info("========== 并发测试：高并发添加数据点 ==========");

        String metric = "test:metric:concurrent";
        int threadCount = 50;
        int operationsPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();
        long baseTime = System.currentTimeMillis();

        // 并发添加数据点
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String value = "value_" + threadIndex + "_" + j;
                        long timestamp = baseTime + (threadIndex * operationsPerThread + j);
                        Boolean added = timeWindowStatisticsService.addDataPoint(metric, value, timestamp);
                        if (Boolean.TRUE.equals(added)) {
                            successCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    log.error("并发添加数据点失败", e);
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
        Long totalCount = timeWindowStatisticsService.getTotalCount(metric);
        log.info("========== 并发测试结果 ==========");
        log.info("总耗时: {} ms", duration);
        log.info("成功操作: {}", successCount.get());
        log.info("失败操作: {}", failureCount.get());
        log.info("总数据点数: {}", totalCount);
        log.info("QPS: {}", successCount.get() * 1000.0 / duration);

        // 验证数据一致性
        assert totalCount != null : "总数据点数不应为null";
        assert totalCount >= threadCount * operationsPerThread : String.format("总数据点数应该至少等于%d，实际为%d",
                threadCount * operationsPerThread, totalCount);

        log.info("✓ 并发测试通过，数据一致性验证成功");
    }

    /**
     * 数据一致性测试：查询和统计的一致性
     */
    @Test
    public void testDataConsistency() {
        log.info("========== 数据一致性测试 ==========");

        String metric = "test:metric:consistency";
        long currentTime = System.currentTimeMillis();
        long startTime = currentTime - 1000;
        long endTime = currentTime + 10000;

        // 添加数据点
        int expectedCount = 20;
        for (int i = 0; i < expectedCount; i++) {
            timeWindowStatisticsService.addDataPoint(metric, "value_" + i, currentTime + i);
        }

        // 等待一段时间，确保数据写入
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 查询数据点
        List<String> dataPoints = timeWindowStatisticsService.getDataPoints(metric, startTime, endTime);
        Long count = timeWindowStatisticsService.countDataPoints(metric, startTime, endTime);

        log.info("数据一致性检查: metric={}, dataPoints.size()={}, count={}",
                metric, dataPoints.size(), count);

        // 验证结果
        assert dataPoints != null : "数据点列表不应为null";
        assert count != null : "统计数量不应为null";
        assert dataPoints.size() == count.intValue() : String.format("查询结果和统计结果应该一致，dataPoints=%d, count=%d",
                dataPoints.size(), count);
        assert dataPoints.size() == expectedCount
                : String.format("数据点数量应该等于%d，实际为%d", expectedCount, dataPoints.size());

        log.info("✓ 数据一致性测试通过");
    }

    /**
     * 压力测试：大量数据点
     */
    @Test
    public void testHighVolumeDataPoints() {
        log.info("========== 压力测试：大量数据点 ==========");

        String metric = "test:metric:pressure";
        int dataPointCount = 1000;
        long currentTime = System.currentTimeMillis();

        long startTime = System.currentTimeMillis();

        // 添加大量数据点
        for (int i = 0; i < dataPointCount; i++) {
            timeWindowStatisticsService.addDataPoint(metric, "value_" + i, currentTime + i);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 获取总数
        Long totalCount = timeWindowStatisticsService.getTotalCount(metric);
        log.info("========== 压力测试结果 ==========");
        log.info("数据点数量: {}", dataPointCount);
        log.info("总耗时: {} ms", duration);
        log.info("总数据点数: {}", totalCount);
        log.info("QPS: {}", dataPointCount * 1000.0 / duration);

        // 验证结果
        assert totalCount != null : "总数据点数不应为null";
        assert totalCount >= dataPointCount : String.format("总数据点数应该至少等于%d，实际为%d", dataPointCount, totalCount);

        log.info("✓ 压力测试通过");
    }
}

