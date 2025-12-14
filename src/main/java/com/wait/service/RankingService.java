package com.wait.service;

import java.util.List;

/**
 * 单项排行榜服务
 * 实现按点赞数、收藏数、评论数排序的排行榜
 */
public interface RankingService {

    /**
     * 点赞时更新点赞排行榜
     * 
     * @param postId 帖子ID
     */
    void onLike(Long postId);

    /**
     * 取消点赞时更新排行榜
     * 
     * @param postId 帖子ID
     */
    void onUnlike(Long postId);

    /**
     * 收藏时更新收藏排行榜
     * 
     * @param postId 帖子ID
     */
    void onFavorite(Long postId);

    /**
     * 取消收藏时更新排行榜
     * 
     * @param postId 帖子ID
     */
    void onUnfavorite(Long postId);

    /**
     * 评论时更新评论排行榜
     * 
     * @param postId 帖子ID
     */
    void onComment(Long postId);

    /**
     * 删除评论时更新评论排行榜
     * 
     * @param postId 帖子ID
     */
    void onUncomment(Long postId);

    /**
     * 获取点赞数排行榜
     * 
     * @param page     页码（从1开始）
     * @param pageSize 每页大小
     * @return 帖子ID列表
     */
    List<Long> getLikesRanking(int page, int pageSize);

    /**
     * 获取收藏数排行榜
     * 
     * @param page     页码（从1开始）
     * @param pageSize 每页大小
     * @return 帖子ID列表
     */
    List<Long> getFavoritesRanking(int page, int pageSize);

    /**
     * 获取评论数排行榜
     * 
     * @param page     页码（从1开始）
     * @param pageSize 每页大小
     * @return 帖子ID列表
     */
    List<Long> getCommentsRanking(int page, int pageSize);

    /**
     * 获取帖子的点赞数（从Redis Sorted Set获取）
     * 
     * @param postId 帖子ID
     * @return 点赞数，如果不存在返回0
     */
    Long getLikeCount(Long postId);

    /**
     * 获取帖子的收藏数（从Redis Sorted Set获取）
     * 
     * @param postId 帖子ID
     * @return 收藏数，如果不存在返回0
     */
    Long getFavoriteCount(Long postId);

    /**
     * 获取帖子的评论数（从Redis Sorted Set获取）
     * 
     * @param postId 帖子ID
     * @return 评论数，如果不存在返回0
     */
    Long getCommentCount(Long postId);
}
