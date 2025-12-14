package com.wait.service;

import java.util.List;

/**
 * 浏览记录服务
 * 使用 Redis Sorted Set 记录用户浏览历史，支持去重和时间范围查询
 * 
 * 实现策略：
 * - 最近3天的浏览记录存储在Redis中（热数据，快速查询）
 * - 3天前的浏览记录持久化到数据库（冷数据，全量查询）
 * - 使用Sorted Set自动去重（相同帖子只保留最新浏览时间）
 */
public interface BrowseHistoryService {

    /**
     * 记录浏览历史（自动去重，更新时间为最新）
     * 
     * @param userId 用户ID
     * @param postId 帖子ID
     */
    void recordBrowse(Long userId, Long postId);

    /**
     * 获取浏览历史（按时间倒序，最新在前）
     * 优先从Redis查询，如果Redis中没有则从数据库查询
     * 
     * @param userId 用户ID
     * @param limit 返回数量限制
     * @return 浏览的帖子ID列表（从新到旧）
     */
    List<Long> getBrowseHistory(Long userId, int limit);

    /**
     * 获取浏览历史（分页）
     * 
     * @param userId 用户ID
     * @param page 页码（从1开始）
     * @param pageSize 每页数量
     * @return 浏览的帖子ID列表（从新到旧）
     */
    List<Long> getBrowseHistory(Long userId, int page, int pageSize);

    /**
     * 查询指定时间范围内的浏览记录
     * 
     * @param userId 用户ID
     * @param startTime 开始时间戳（毫秒）
     * @param endTime 结束时间戳（毫秒）
     * @return 浏览的帖子ID列表
     */
    List<Long> getBrowseHistoryByTimeRange(Long userId, long startTime, long endTime);

    /**
     * 检查是否浏览过指定帖子
     * 
     * @param userId 用户ID
     * @param postId 帖子ID
     * @return 是否浏览过
     */
    boolean hasBrowsed(Long userId, Long postId);

    /**
     * 获取浏览时间
     * 
     * @param userId 用户ID
     * @param postId 帖子ID
     * @return 浏览时间戳（毫秒），如果未浏览过返回null
     */
    Long getBrowseTime(Long userId, Long postId);

    /**
     * 清除浏览历史（清除Redis和数据库中的记录）
     * 
     * @param userId 用户ID
     */
    void clearBrowseHistory(Long userId);

    /**
     * 清除指定时间之前的浏览记录
     * 
     * @param userId 用户ID
     * @param expireTime 过期时间戳（毫秒），删除此时间之前的记录
     */
    void clearOldBrowseHistory(Long userId, long expireTime);

    /**
     * 获取浏览历史总数
     * 
     * @param userId 用户ID
     * @return 浏览记录总数（Redis + 数据库）
     */
    Long getBrowseHistoryCount(Long userId);
}

