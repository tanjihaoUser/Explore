package com.wait.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.wait.config.TimeWindowStatisticsProperties;
import com.wait.service.TimeWindowStatisticsService;
import com.wait.util.BoundUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 时间窗口统计服务实现
 * 基于 Redis SortedSet 实现滑动窗口数据统计
 * 
 * Redis 命令使用：
 * - ZADD: 添加数据点（分数为时间戳）
 * - ZRANGEBYSCORE: 查询时间范围内的数据
 * - ZREMRANGEBYSCORE: 清理过期数据
 * - ZCOUNT: 统计时间范围内数量
 * - ZCARD: 获取数据点总数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimeWindowStatisticsServiceImpl implements TimeWindowStatisticsService {

    private final BoundUtil boundUtil;
    private final TimeWindowStatisticsProperties properties;

    private static final String STATS_PREFIX = "stats:window:";

    @Override
    public Boolean addDataPoint(String metric, String value, long timestamp) {
        if (metric == null || value == null) {
            throw new IllegalArgumentException("Metric and value cannot be null");
        }

        String key = STATS_PREFIX + metric;
        Boolean added = boundUtil.zAdd(key, value, timestamp);

        if (Boolean.TRUE.equals(added)) {
            log.debug("Data point added: metric={}, value={}, timestamp={}", metric, value, timestamp);
        }

        return added;
    }

    @Override
    public Boolean addDataPoint(String metric, String value) {
        long currentTime = System.currentTimeMillis();
        return addDataPoint(metric, value, currentTime);
    }

    @Override
    public List<String> getDataPoints(String metric, long startTime, long endTime) {
        if (metric == null) {
            throw new IllegalArgumentException("Metric cannot be null");
        }

        String key = STATS_PREFIX + metric;
        List<String> dataPoints = boundUtil.zRangeByScore(key, startTime, endTime, String.class);

        return dataPoints != null ? dataPoints : new ArrayList<>();
    }

    @Override
    public List<String> getRecentDataPoints(String metric, int days) {
        if (metric == null || days <= 0) {
            throw new IllegalArgumentException("Metric cannot be null and days must be positive");
        }

        long endTime = System.currentTimeMillis();
        long startTime = endTime - (days * 24L * 60 * 60 * 1000);

        return getDataPoints(metric, startTime, endTime);
    }

    @Override
    public List<String> getRecentDataPointsByHours(String metric, int hours) {
        if (metric == null || hours <= 0) {
            throw new IllegalArgumentException("Metric cannot be null and hours must be positive");
        }

        long endTime = System.currentTimeMillis();
        long startTime = endTime - (hours * 60L * 60 * 1000);

        return getDataPoints(metric, startTime, endTime);
    }

    @Override
    public Long countDataPoints(String metric, long startTime, long endTime) {
        if (metric == null) {
            throw new IllegalArgumentException("Metric cannot be null");
        }

        String key = STATS_PREFIX + metric;
        // 直接使用 ZCOUNT 命令统计，无需获取列表
        return boundUtil.zCount(key, startTime, endTime);
    }

    @Override
    public Long cleanExpiredData(String metric, long expireTime) {
        if (metric == null) {
            throw new IllegalArgumentException("Metric cannot be null");
        }

        String key = STATS_PREFIX + metric;
        // 先统计要删除的数据数量
        Long count = boundUtil.zCount(key, 0, expireTime);

        if (count != null && count > 0) {
            // 直接使用 ZREMRANGEBYSCORE 批量删除过期数据（分数在 0 到 expireTime 之间的所有成员）
            Long deletedCount = boundUtil.zRemRangeByScore(key, 0, expireTime);
            log.info("Cleaned {} expired data points for metric {} (expireTime: {})",
                    deletedCount, metric, expireTime);
            return deletedCount != null ? deletedCount : 0L;
        }

        return 0L;
    }

    @Override
    public Long cleanExpiredDataByDays(String metric, int keepDays) {
        if (metric == null || keepDays <= 0) {
            throw new IllegalArgumentException("Metric cannot be null and keepDays must be positive");
        }

        long currentTime = System.currentTimeMillis();
        long expireTime = currentTime - (keepDays * 24L * 60 * 60 * 1000);

        return cleanExpiredData(metric, expireTime);
    }

    @Override
    public Long getTotalCount(String metric) {
        if (metric == null) {
            throw new IllegalArgumentException("Metric cannot be null");
        }

        String key = STATS_PREFIX + metric;
        return boundUtil.zCard(key);
    }

    @Override
    public Map<String, Double> calculateStatistics(String metric, long startTime, long endTime) {
        if (metric == null) {
            throw new IllegalArgumentException("Metric cannot be null");
        }

        List<String> dataPoints = getDataPoints(metric, startTime, endTime);

        if (dataPoints.isEmpty()) {
            Map<String, Double> result = new HashMap<>();
            result.put("sum", 0.0);
            result.put("avg", 0.0);
            result.put("max", 0.0);
            result.put("min", 0.0);
            result.put("count", 0.0);
            return result;
        }

        // 计算统计值（假设数据值是数字）
        double sum = 0.0;
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;
        int count = 0;

        for (String value : dataPoints) {
            try {
                double numValue = Double.parseDouble(value);
                sum += numValue;
                max = Math.max(max, numValue);
                min = Math.min(min, numValue);
                count++;
            } catch (NumberFormatException e) {
                log.warn("Invalid numeric value in statistics: {}", value);
            }
        }

        Map<String, Double> result = new HashMap<>();
        result.put("sum", sum);
        result.put("avg", count > 0 ? sum / count : 0.0);
        result.put("max", max == Double.NEGATIVE_INFINITY ? 0.0 : max);
        result.put("min", min == Double.POSITIVE_INFINITY ? 0.0 : min);
        result.put("count", (double) count);

        log.debug("Statistics calculated for metric {}: {}", metric, result);
        return result;
    }

    /**
     * 获取已知的指标名称列表
     * 从 application.yml 配置文件中读取
     * 保留此方法供 TimeWindowStatisticsPersistenceService 使用
     * 
     * @return 指标名称列表
     */
    List<String> getKnownMetrics() {
        List<String> configuredMetrics = properties.getMetrics();

        if (configuredMetrics == null || configuredMetrics.isEmpty()) {
            log.warn("No metrics configured in application.yml (time-window-statistics.metrics), " +
                    "persistence task will be skipped. Please configure metrics in application.yml.");
            return new ArrayList<>();
        }

        log.debug("Loaded {} metrics from configuration: {}", configuredMetrics.size(), configuredMetrics);
        return new ArrayList<>(configuredMetrics);
    }

}
