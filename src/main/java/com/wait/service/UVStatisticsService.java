package com.wait.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.wait.entity.type.ResourceType;

/**
 * 独立访客统计服务（UV Statistics）
 * 使用 Redis Set 统计独立访客数量
 * 
 * 数据存储策略：
 * - Redis：存储最近7天的 UV 数据（热数据，快速访问）
 * - 数据库：存储7天前的 UV 数据（冷数据，持久化存储）
 */
public interface UVStatisticsService {

    /**
     * 记录访问（自动记录当日访问）
     * 
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @param visitorId    访客ID（用户ID或IP地址）
     * @return 是否是新访客（true表示首次访问）
     */
    Boolean recordVisit(ResourceType resourceType, Long resourceId, String visitorId);

    /**
     * 获取总独立访客数（从 Redis 和数据库合并计算）
     * 
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @return 总独立访客数
     */
    Long getUV(ResourceType resourceType, Long resourceId);

    /**
     * 获取日独立访客数
     * 
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @param date         日期（格式：yyyyMMdd）
     * @return 日独立访客数
     */
    Long getDailyUV(String resourceType, Long resourceId, String date);

    /**
     * 记录日访问
     * 
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @param date         日期（格式：yyyyMMdd）
     * @param visitorId    访客ID
     * @return 是否是新访客
     */
    Boolean recordDailyVisit(String resourceType, Long resourceId, String date, String visitorId);

    /**
     * 合并多天的UV数据
     * 
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @param dates        日期列表
     * @return 合并后的独立访客数
     */
    Long mergeUV(String resourceType, Long resourceId, Set<String> dates);

    /**
     * 检查访客是否已访问
     * 
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @param visitorId    访客ID
     * @return 是否已访问
     */
    Boolean hasVisited(String resourceType, Long resourceId, String visitorId);

    /**
     * 获取用户所有帖子的UV统计
     * 
     * @param userId 用户ID
     * @return 帖子UV统计列表，包含帖子ID、标题、总UV、点赞数、收藏数、评论数等信息
     */
    List<Map<String, Object>> getUserPostsUV(Long userId);
}
