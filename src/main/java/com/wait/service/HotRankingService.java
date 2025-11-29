package com.wait.service;

import java.util.List;

/**
 * 热度排行榜服务
 * 使用 Sorted Set 实现帖子热度排行榜
 */
public interface HotRankingService {

    /**
     * 更新帖子热度分数（综合评分）
     * @param postId 帖子ID
     */
    void updateHotScore(Long postId);

    /**
     * 点赞时更新热度分数
     * @param postId 帖子ID
     */
    void onLike(Long postId);

    /**
     * 取消点赞时更新热度分数
     * @param postId 帖子ID
     */
    void onUnlike(Long postId);

    /**
     * 收藏时更新热度分数
     * @param postId 帖子ID
     */
    void onFavorite(Long postId);

    /**
     * 取消收藏时更新热度分数
     * @param postId 帖子ID
     */
    void onUnfavorite(Long postId);

    /**
     * 评论时更新热度分数
     * @param postId 帖子ID
     */
    void onComment(Long postId);

    /**
     * 获取热门帖子排行榜（指定时间段）
     * @param period 时间段（daily/weekly/monthly/alltime）
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @return 帖子ID列表
     */
    List<Long> getHotPosts(String period, int page, int pageSize);

    /**
     * 获取帖子在排行榜中的排名
     * @param postId 帖子ID
     * @param period 时间段
     * @return 排名（1-based，如果不在排行榜中返回null）
     */
    Long getPostRank(Long postId, String period);

    /**
     * 获取帖子热度分数
     * @param postId 帖子ID
     * @param period 时间段
     * @return 热度分数
     */
    Double getHotScore(Long postId, String period);
}


