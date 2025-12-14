package com.wait.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.wait.entity.domain.TimeWindowStatistics;

/**
 * 时间窗口统计数据 Mapper
 */
@Mapper
public interface TimeWindowStatisticsMapper {

    /**
     * 批量插入统计数据
     * 
     * @param statistics 统计数据列表
     * @return 插入的记录数
     */
    int batchInsert(@Param("statistics") List<TimeWindowStatistics> statistics);

    /**
     * 查询指定指标和时间范围内的统计数据
     * 
     * @param metric 指标名称
     * @param startTime 开始时间戳（毫秒）
     * @param endTime 结束时间戳（毫秒）
     * @return 统计数据列表
     */
    List<TimeWindowStatistics> selectByMetricAndTimeRange(
            @Param("metric") String metric,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime);

    /**
     * 查询指定时间范围内的所有统计数据
     * 
     * @param startTime 开始时间戳（毫秒）
     * @param endTime 结束时间戳（毫秒）
     * @return 统计数据列表
     */
    List<TimeWindowStatistics> selectByTimeRange(
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime);

    /**
     * 删除指定时间之前的数据（用于清理过期数据）
     * 
     * @param expireTime 过期时间戳（毫秒），删除此时间之前的数据
     * @return 删除的记录数
     */
    int deleteByExpireTime(@Param("expireTime") Long expireTime);

    /**
     * 统计指定指标和时间范围内的数据数量
     * 
     * @param metric 指标名称
     * @param startTime 开始时间戳（毫秒）
     * @param endTime 结束时间戳（毫秒）
     * @return 数据数量
     */
    Long countByMetricAndTimeRange(
            @Param("metric") String metric,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime);
}

