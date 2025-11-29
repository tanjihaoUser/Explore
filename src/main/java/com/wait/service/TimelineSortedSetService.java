package com.wait.service;

import java.util.List;

/**
 * 时间线排序服务
 * 使用 Sorted Set 实现按发布时间排序的时间线功能
 */
public interface TimelineSortedSetService {

    /**
     * 发布新帖子到时间线
     */
    void publishToTimeline(Long userId, Long postId, long publishTime);

    /**
     * 获取用户时间线（最新发布的在前）
     */
    List<Long> getUserTimeline(Long userId, int page, int pageSize);

    /**
     * 获取全局时间线（最新发布的在前）
     */
    List<Long> getGlobalTimeline(int page, int pageSize);

    /**
     * 获取指定时间范围内的帖子
     */
    List<Long> getPostsByTimeRange(Long userId, long startTime, long endTime);

    /**
     * 删除帖子时从时间线移除
     */
    void removeFromTimeline(Long userId, Long postId);

    /**
     * 获取我的时间线（聚合关注用户的帖子，按发布时间排序）
     * 会自动过滤黑名单用户的帖子
     */
    List<Long> getMyTimeline(Long userId, int page, int pageSize);
}

