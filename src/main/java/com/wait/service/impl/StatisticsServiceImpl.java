package com.wait.service.impl;

import static java.time.temporal.ChronoUnit.DAYS;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.wait.entity.type.ResourceType;
import com.wait.mapper.UVStatisticsMapper;
import com.wait.service.RankingService;
import com.wait.service.StatisticsService;
import com.wait.service.TimeWindowStatisticsService;
import com.wait.service.UVStatisticsService;
import com.wait.util.BoundUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 统计服务实现
 * 基于 TimeWindowStatisticsService 实现各种业务统计
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final TimeWindowStatisticsService timeWindowStatisticsService;
    private final UVStatisticsService uvStatisticsService;
    private final RankingService rankingService;
    private final BoundUtil boundUtil;
    private final UVStatisticsMapper uvStatisticsMapper;

    // 指标名称常量
    private static final String METRIC_POST_VIEW = "post:view";
    private static final String METRIC_HOME_PAGE_VIEW = "homepage:view";
    private static final String METRIC_LIKE = "post:like";
    private static final String METRIC_FAVORITE = "post:favorite";

    // UV统计相关常量
    private static final String UV_DAILY_PREFIX = "uv:daily:";
    private static final int KEEP_DAYS = 7; // Redis中保留最近7天的数据
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public void recordPostView(Long postId) {
        if (postId == null) {
            return;
        }
        String metric = METRIC_POST_VIEW + ":" + postId;
        timeWindowStatisticsService.addDataPoint(metric, "1");
        log.debug("Recorded post view: postId={}", postId);
    }

    @Override
    public void recordHomePageView(Long userId) {
        String metric = METRIC_HOME_PAGE_VIEW;
        String value = userId != null ? userId.toString() : "anonymous";
        timeWindowStatisticsService.addDataPoint(metric, value);
        log.debug("Recorded homepage view: userId={}", userId);
    }

    @Override
    public void recordLike(Long postId, boolean isLike) {
        if (postId == null) {
            return;
        }
        String metric = METRIC_LIKE + ":" + postId;
        String value = isLike ? "1" : "-1"; // 1表示点赞，-1表示取消点赞
        timeWindowStatisticsService.addDataPoint(metric, value);
        log.debug("Recorded like: postId={}, isLike={}", postId, isLike);
    }

    @Override
    public void recordFavorite(Long postId, boolean isFavorite) {
        if (postId == null) {
            return;
        }
        String metric = METRIC_FAVORITE + ":" + postId;
        String value = isFavorite ? "1" : "-1"; // 1表示收藏，-1表示取消收藏
        timeWindowStatisticsService.addDataPoint(metric, value);
        log.debug("Recorded favorite: postId={}, isFavorite={}", postId, isFavorite);
    }

    @Override
    public List<Map<String, Object>> getPostViewStatistics(Long postId, int hours) {
        if (postId == null) {
            return new ArrayList<>();
        }

        String metric = METRIC_POST_VIEW + ":" + postId;
        List<String> dataPoints = timeWindowStatisticsService.getRecentDataPointsByHours(metric, hours);

        // 转换为时间序列数据（按小时聚合）
        return aggregateByHour(dataPoints, hours);
    }

    @Override
    public List<Map<String, Object>> getHomePageViewStatistics(int hours) {
        String metric = METRIC_HOME_PAGE_VIEW;
        List<String> dataPoints = timeWindowStatisticsService.getRecentDataPointsByHours(metric, hours);

        // 转换为时间序列数据（按小时聚合）
        return aggregateByHour(dataPoints, hours);
    }

    @Override
    public List<Map<String, Object>> getLikeStatistics(Long postId, int hours) {
        String metric;
        if (postId != null) {
            metric = METRIC_LIKE + ":" + postId;
        } else {
            // 获取所有帖子的点赞统计（需要特殊处理）
            metric = METRIC_LIKE;
        }

        List<String> dataPoints = timeWindowStatisticsService.getRecentDataPointsByHours(metric, hours);

        // 计算累计值（1表示点赞，-1表示取消点赞）
        return aggregateCumulativeByHour(dataPoints, hours);
    }

    @Override
    public List<Map<String, Object>> getFavoriteStatistics(Long postId, int hours) {
        String metric;
        if (postId != null) {
            metric = METRIC_FAVORITE + ":" + postId;
        } else {
            metric = METRIC_FAVORITE;
        }

        List<String> dataPoints = timeWindowStatisticsService.getRecentDataPointsByHours(metric, hours);

        // 计算累计值
        return aggregateCumulativeByHour(dataPoints, hours);
    }

    @Override
    public Map<String, Object> getComprehensiveStatistics(Long postId, int hours) {
        Map<String, Object> result = new HashMap<>();

        if (postId != null) {
            result.put("postId", postId);
            result.put("viewStatistics", getPostViewStatistics(postId, hours));
            result.put("likeStatistics", getLikeStatistics(postId, hours));
            result.put("favoriteStatistics", getFavoriteStatistics(postId, hours));
        } else {
            result.put("homePageViewStatistics", getHomePageViewStatistics(hours));
            result.put("likeStatistics", getLikeStatistics(null, hours));
            result.put("favoriteStatistics", getFavoriteStatistics(null, hours));
        }

        return result;
    }

    /**
     * 按小时聚合数据点（简单计数）
     * 由于TimeWindowStatisticsService返回的是value列表，我们按时间窗口平均分配
     */
    private List<Map<String, Object>> aggregateByHour(List<String> dataPoints, int hours) {
        long currentTime = System.currentTimeMillis();

        // 将数据点按时间窗口分配到各个小时
        // 简化处理：假设数据点均匀分布在时间窗口内
        List<Map<String, Object>> result = new ArrayList<>();

        if (dataPoints.isEmpty()) {
            // 如果没有数据，返回空的时间序列
            for (int i = 0; i < hours; i++) {
                long hourTimestamp = currentTime - (hours - i - 1) * 3600 * 1000L;
                Map<String, Object> point = new HashMap<>();
                point.put("time", hourTimestamp);
                point.put("value", 0);
                result.add(point);
            }
            return result;
        }

        // 将数据点分配到各个小时
        int totalPoints = dataPoints.size();
        int pointsPerHour = totalPoints / hours;
        int remainder = totalPoints % hours;

        int dataIndex = 0;
        for (int i = 0; i < hours; i++) {
            long hourTimestamp = currentTime - (hours - i - 1) * 3600 * 1000L;
            int count = pointsPerHour + (i < remainder ? 1 : 0);

            // 统计这个小时的数据点数量
            int hourCount = 0;
            for (int j = 0; j < count && dataIndex < totalPoints; j++) {
                hourCount++;
                dataIndex++;
            }

            Map<String, Object> point = new HashMap<>();
            point.put("time", hourTimestamp);
            point.put("value", hourCount);
            result.add(point);
        }

        return result;
    }

    /**
     * 按小时聚合累计值（用于点赞、收藏等有增减的操作）
     */
    private List<Map<String, Object>> aggregateCumulativeByHour(List<String> dataPoints, int hours) {
        long currentTime = System.currentTimeMillis();

        // 计算累计值
        int cumulative = 0;
        List<Map<String, Object>> result = new ArrayList<>();

        // 简化处理：将数据点平均分配到各个小时
        int pointsPerHour = dataPoints.size() / hours;
        int remainder = dataPoints.size() % hours;

        int dataIndex = 0;
        for (int i = 0; i < hours; i++) {
            long hourTimestamp = currentTime - (hours - i - 1) * 3600 * 1000L;
            int count = pointsPerHour + (i < remainder ? 1 : 0);

            // 计算这个小时的累计值
            for (int j = 0; j < count && dataIndex < dataPoints.size(); j++) {
                try {
                    int value = Integer.parseInt(dataPoints.get(dataIndex));
                    cumulative += value;
                    dataIndex++;
                } catch (NumberFormatException e) {
                    log.warn("Invalid data point value: {}", dataPoints.get(dataIndex));
                    dataIndex++;
                }
            }

            Map<String, Object> point = new HashMap<>();
            point.put("time", hourTimestamp);
            point.put("value", cumulative);
            result.add(point);
        }

        return result;
    }

    @Override
    public Map<String, Long> getDailyUVInRange(ResourceType resourceType, Long resourceId, String startDate,
            String endDate) {
        if (resourceType == null || resourceId == null || startDate == null || endDate == null) {
            return new HashMap<>();
        }

        Map<String, Long> result = new LinkedHashMap<>(); // 保持日期顺序

        try {
            LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
            LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);
            LocalDate today = LocalDate.now();

            // 遍历日期范围
            LocalDate current = start;
            while (!current.isAfter(end)) {
                String dateStr = current.format(DATE_FORMATTER);
                long daysDiff = DAYS.between(current, today);

                if (daysDiff < KEEP_DAYS) {
                    // 在 Redis 中查询
                    String key = UV_DAILY_PREFIX + resourceType.getCode() + ":" + resourceId + ":" + dateStr;
                    Long count = boundUtil.sCard(key);
                    result.put(dateStr, count != null ? count : 0L);
                } else {
                    // 从数据库查询
                    Long count = uvStatisticsMapper.countDailyUV(resourceType.getCode(), resourceId, dateStr);
                    result.put(dateStr, count != null ? count : 0L);
                }

                current = current.plusDays(1);
            }
        } catch (Exception e) {
            log.error("Failed to get daily UV in range: type={}, resourceId={}, startDate={}, endDate={}",
                    resourceType, resourceId, startDate, endDate, e);
        }

        return result;
    }

    @Override
    public Map<String, Object> getPostStatisticsInRange(Long postId, String startDate, String endDate) {
        if (postId == null || startDate == null || endDate == null) {
            return new HashMap<>();
        }

        Map<String, Object> result = new HashMap<>();
        try {
            // 1. 获取每日UV统计
            Map<String, Long> dailyUV = getDailyUVInRange(ResourceType.POST, postId, startDate, endDate);
            result.put("dailyUV", dailyUV);

            // 2. 获取每日点赞数统计（使用 TimeWindowStatisticsService）
            // 指标格式：post:like:123
            Map<String, Long> dailyLikes = getDailyCountInRange("post:like", postId, startDate, endDate);
            result.put("dailyLikes", dailyLikes);

            // 3. 获取每日收藏数统计
            // 指标格式：post:favorite:123
            Map<String, Long> dailyFavorites = getDailyCountInRange("post:favorite", postId, startDate, endDate);
            result.put("dailyFavorites", dailyFavorites);

            // 4. 获取每日评论数统计
            // 注意：评论可能没有使用 TimeWindowStatisticsService，暂时返回空数据
            // 如果需要，可以扩展 StatisticsService 来记录评论
            Map<String, Long> dailyComments = new LinkedHashMap<>();
            // 填充日期范围，值为0（因为评论可能没有按日期统计）
            try {
                LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
                LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);
                LocalDate current = start;
                while (!current.isAfter(end)) {
                    String dateStr = current.format(DATE_FORMATTER);
                    dailyComments.put(dateStr, 0L);
                    current = current.plusDays(1);
                }
            } catch (Exception e) {
                log.warn("Failed to initialize daily comments map", e);
            }
            result.put("dailyComments", dailyComments);

            // 5. 获取总量
            Long totalUV = uvStatisticsService.getUV(ResourceType.POST, postId);
            Long totalLikes = rankingService.getLikeCount(postId);
            Long totalFavorites = rankingService.getFavoriteCount(postId);
            Long totalComments = rankingService.getCommentCount(postId);

            Map<String, Long> totals = new HashMap<>();
            totals.put("totalUV", totalUV != null ? totalUV : 0L);
            totals.put("totalLikes", totalLikes != null ? totalLikes : 0L);
            totals.put("totalFavorites", totalFavorites != null ? totalFavorites : 0L);
            totals.put("totalComments", totalComments != null ? totalComments : 0L);
            result.put("totals", totals);

            // 6. 计算一段时间内的增量
            Map<String, Long> increments = new HashMap<>();
            increments.put("uvIncrement", calculateIncrement(dailyUV));
            increments.put("likesIncrement", calculateIncrement(dailyLikes));
            increments.put("favoritesIncrement", calculateIncrement(dailyFavorites));
            increments.put("commentsIncrement", calculateIncrement(dailyComments));
            result.put("increments", increments);

        } catch (Exception e) {
            log.error("Failed to get post statistics in range: postId={}, startDate={}, endDate={}",
                    postId, startDate, endDate, e);
        }

        return result;
    }

    /**
     * 获取一段时间内每日的数量统计（基于 TimeWindowStatisticsService）
     * 
     * @param metricPrefix 指标前缀（如 "post:like"）
     * @param resourceId   资源ID
     * @param startDate    开始日期（格式：yyyyMMdd）
     * @param endDate      结束日期（格式：yyyyMMdd）
     * @return 每日数量统计
     */
    private Map<String, Long> getDailyCountInRange(String metricPrefix, Long resourceId, String startDate,
            String endDate) {
        Map<String, Long> result = new LinkedHashMap<>();

        try {
            LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
            LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);

            // 遍历日期范围
            LocalDate current = start;
            while (!current.isAfter(end)) {
                String dateStr = current.format(DATE_FORMATTER);

                // 计算该日期的开始和结束时间戳
                long startTimestamp = current.atStartOfDay(ZoneId.systemDefault())
                        .toInstant().toEpochMilli();
                long endTimestamp = current.plusDays(1).atStartOfDay(ZoneId.systemDefault())
                        .toInstant().toEpochMilli() - 1;

                // 构建指标名称：post:like:123 或 post:favorite:123
                String metric = metricPrefix + ":" + resourceId;

                // 使用 TimeWindowStatisticsService 获取该日期的数据点数量
                try {
                    Long count = timeWindowStatisticsService.countDataPoints(metric, startTimestamp, endTimestamp);
                    result.put(dateStr, count != null ? count : 0L);
                } catch (Exception e) {
                    log.debug("Failed to get daily count for metric: {}, date: {}", metric, dateStr, e);
                    result.put(dateStr, 0L);
                }

                current = current.plusDays(1);
            }
        } catch (Exception e) {
            log.error("Failed to get daily count in range: metricPrefix={}, resourceId={}, startDate={}, endDate={}",
                    metricPrefix, resourceId, startDate, endDate, e);
        }

        return result;
    }

    /**
     * 计算一段时间内的增量（最后一天的值 - 第一天的值）
     */
    private Long calculateIncrement(Map<String, Long> dailyData) {
        if (dailyData == null || dailyData.isEmpty()) {
            return 0L;
        }

        List<Long> values = new ArrayList<>(dailyData.values());
        if (values.size() < 2) {
            return 0L;
        }

        Long firstValue = values.get(0);
        Long lastValue = values.get(values.size() - 1);
        return (lastValue != null ? lastValue : 0L) - (firstValue != null ? firstValue : 0L);
    }
}
