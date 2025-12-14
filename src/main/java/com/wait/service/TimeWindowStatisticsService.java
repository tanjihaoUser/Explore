package com.wait.service;

import java.util.List;
import java.util.Map;

/**
 * 时间窗口统计服务
 * 基于 Redis SortedSet 实现滑动窗口数据统计
 * 
 * 使用场景：
 * - 最近7天访问统计
 * - 最近30天销售额统计
 * - 最近1小时请求数统计
 * - 实时数据监控
 */
public interface TimeWindowStatisticsService {

    /**
     * 添加数据点
     * 
     * @param metric 指标名称
     * @param value 数据值
     * @param timestamp 时间戳（毫秒）
     * @return 是否成功
     */
    Boolean addDataPoint(String metric, String value, long timestamp);

    /**
     * 添加数据点（使用当前时间）
     * 
     * @param metric 指标名称
     * @param value 数据值
     * @return 是否成功
     */
    Boolean addDataPoint(String metric, String value);

    /**
     * 查询时间范围内的数据点
     * 
     * @param metric 指标名称
     * @param startTime 开始时间戳（毫秒）
     * @param endTime 结束时间戳（毫秒）
     * @return 数据值列表
     */
    List<String> getDataPoints(String metric, long startTime, long endTime);

    /**
     * 获取最近N天的数据点
     * 
     * @param metric 指标名称
     * @param days 天数
     * @return 数据值列表
     */
    List<String> getRecentDataPoints(String metric, int days);

    /**
     * 获取最近N小时的数据点
     * 
     * @param metric 指标名称
     * @param hours 小时数
     * @return 数据值列表
     */
    List<String> getRecentDataPointsByHours(String metric, int hours);

    /**
     * 统计时间范围内的数据点数量
     * 
     * @param metric 指标名称
     * @param startTime 开始时间戳（毫秒）
     * @param endTime 结束时间戳（毫秒）
     * @return 数据点数量
     */
    Long countDataPoints(String metric, long startTime, long endTime);

    /**
     * 清理过期数据（删除指定时间之前的数据）
     * 
     * @param metric 指标名称
     * @param expireTime 过期时间戳（毫秒），删除此时间之前的数据
     * @return 删除的数据点数量
     */
    Long cleanExpiredData(String metric, long expireTime);

    /**
     * 清理过期数据（保留最近N天的数据）
     * 
     * @param metric 指标名称
     * @param keepDays 保留天数
     * @return 删除的数据点数量
     */
    Long cleanExpiredDataByDays(String metric, int keepDays);

    /**
     * 获取数据点总数
     * 
     * @param metric 指标名称
     * @return 数据点总数
     */
    Long getTotalCount(String metric);

    /**
     * 计算时间范围内的统计值（求和、平均、最大、最小）
     * 
     * @param metric 指标名称
     * @param startTime 开始时间戳（毫秒）
     * @param endTime 结束时间戳（毫秒）
     * @return 统计结果（sum, avg, max, min）
     */
    Map<String, Double> calculateStatistics(String metric, long startTime, long endTime);
}

