package com.wait.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.wait.entity.domain.UVStatistics;

/**
 * UV 统计 Mapper
 */
@Mapper
public interface UVStatisticsMapper {

    /**
     * 批量插入 UV 统计数据
     * 
     * @param statistics UV 统计数据列表
     * @return 插入的记录数
     */
    int batchInsert(@Param("statistics") List<UVStatistics> statistics);

    /**
     * 查询指定资源和日期的 UV 统计数据
     * 
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @param date         日期（格式：yyyyMMdd）
     * @return UV 统计数据列表
     */
    List<UVStatistics> selectByResourceAndDate(@Param("resourceType") String resourceType,
            @Param("resourceId") Long resourceId,
            @Param("date") String date);

    /**
     * 查询指定资源和日期范围的 UV 统计数据
     * 
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @param startDate    开始日期（格式：yyyyMMdd）
     * @param endDate      结束日期（格式：yyyyMMdd）
     * @return UV 统计数据列表
     */
    List<UVStatistics> selectByResourceAndDateRange(@Param("resourceType") String resourceType,
            @Param("resourceId") Long resourceId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    /**
     * 统计指定资源和日期的独立访客数
     * 
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @param date         日期（格式：yyyyMMdd）
     * @return 独立访客数
     */
    Long countDailyUV(@Param("resourceType") String resourceType,
            @Param("resourceId") Long resourceId,
            @Param("date") String date);

    /**
     * 统计指定资源和日期范围的独立访客数（去重）
     * 
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @param startDate    开始日期（格式：yyyyMMdd）
     * @param endDate      结束日期（格式：yyyyMMdd）
     * @return 独立访客数
     */
    Long countUVByDateRange(@Param("resourceType") String resourceType,
            @Param("resourceId") Long resourceId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    /**
     * 删除指定日期之前的数据
     * 
     * @param expireDate 过期日期（格式：yyyyMMdd）
     * @return 删除的记录数
     */
    int deleteByExpireDate(@Param("expireDate") String expireDate);
}
