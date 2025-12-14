package com.wait.task.persist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.wait.config.TimeWindowStatisticsProperties;
import com.wait.entity.domain.TimeWindowStatistics;
import com.wait.mapper.TimeWindowStatisticsMapper;
import com.wait.util.BoundUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 时间窗口统计数据持久化服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimeWindowStatisticsPersistenceService implements DataPersistenceService<TimeWindowStatistics> {

    private final BoundUtil boundUtil;
    private final TimeWindowStatisticsMapper statisticsMapper;
    private final TimeWindowStatisticsProperties properties;

    private static final String STATS_PREFIX = "stats:window:";
    private static final int KEEP_DAYS = 7;

    @Override
    public String getKeyPattern() {
        // 时间窗口统计的 key 模式需要从配置中获取所有指标
        // 这里返回一个通用的模式，实际会在 collectDataFromRedis 中处理
        return STATS_PREFIX + "*";
    }

    @Override
    public int getKeepDays() {
        return KEEP_DAYS;
    }

    @Override
    public Long parseExpireTimeFromKey(String key) {
        // 时间窗口统计的过期时间不是从 key 中解析，而是从 Sorted Set 的 score 中获取
        // 这里返回 null，表示需要从数据中判断
        return null;
    }

    @Override
    public List<TimeWindowStatistics> collectDataFromRedis(String key, long expireTime) {
        List<TimeWindowStatistics> result = new ArrayList<>();
        
        try {
            // 从key中提取实际的指标名称（去掉前缀）
            String actualMetric = key.substring(STATS_PREFIX.length());

            // 使用 ZRANGEBYSCORE WITHSCORES 一次性获取时间范围内的记录及其分数
            // 避免 N+1 查询问题
            Map<String, Double> membersWithScores = boundUtil.zRangeByScoreWithScores(
                    key, 0, expireTime, String.class);

            if (membersWithScores == null || membersWithScores.isEmpty()) {
                return result;
            }

            // 构建统计数据列表
            for (Map.Entry<String, Double> entry : membersWithScores.entrySet()) {
                String value = entry.getKey();
                Double score = entry.getValue();
                if (value != null && score != null && score <= expireTime) {
                    TimeWindowStatistics stat = TimeWindowStatistics.builder()
                            .metric(actualMetric) // 使用实际的指标名称（包含后缀）
                            .value(value)
                            .timestamp(score.longValue())
                            .build();
                    result.add(stat);
                }
            }
        } catch (Exception e) {
            log.error("Failed to collect data from Redis for key: {}", key, e);
        }

        return result;
    }

    @Override
    public int batchInsertToDatabase(List<TimeWindowStatistics> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }
        return statisticsMapper.batchInsert(dataList);
    }

    @Override
    public long deleteFromRedis(String key, long expireTime) {
        try {
            Long deletedCount = boundUtil.zRemRangeByScore(key, 0, expireTime);
            return deletedCount != null ? deletedCount : 0;
        } catch (Exception e) {
            log.error("Failed to delete from Redis for key: {}", key, e);
            return 0;
        }
    }

    @Override
    public String getTaskName() {
        return "Time Window Statistics Persistence";
    }

    /**
     * 获取已知的指标名称列表
     * 从 application.yml 配置文件中读取
     * 
     * @return 指标名称列表
     */
    private List<String> getKnownMetrics() {
        List<String> configuredMetrics = properties.getMetrics();

        if (configuredMetrics == null || configuredMetrics.isEmpty()) {
            log.warn("No metrics configured in application.yml (time-window-statistics.metrics), " +
                    "persistence task will be skipped. Please configure metrics in application.yml.");
            return new ArrayList<>();
        }

        log.debug("Loaded {} metrics from configuration: {}", configuredMetrics.size(), configuredMetrics);
        return new ArrayList<>(configuredMetrics);
    }

    /**
     * 获取所有需要处理的 key 模式
     * 由于时间窗口统计需要从配置中读取指标列表，这里需要特殊处理
     * 
     * @return key 模式列表
     */
    @Override
    public List<String> getKeyPatterns() {
        List<String> metrics = getKnownMetrics();
        List<String> patterns = new ArrayList<>();
        for (String metric : metrics) {
            patterns.add(STATS_PREFIX + metric + "*");
        }
        return patterns;
    }
}

