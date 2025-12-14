package com.wait.service;

import java.util.List;
import java.util.Map;

import com.wait.entity.type.ResourceType;

/**
 * 统计服务接口
 * 提供各种业务统计功能，基于 TimeWindowStatisticsService 实现
 */
public interface StatisticsService {

    /**
     * 记录帖子浏览量
     * 
     * @param postId 帖子ID
     */
    void recordPostView(Long postId);

    /**
     * 记录主页访问量
     * 
     * @param userId 用户ID（可选，null表示匿名访问）
     */
    void recordHomePageView(Long userId);

    /**
     * 记录点赞操作
     * 
     * @param postId 帖子ID
     * @param isLike true表示点赞，false表示取消点赞
     */
    void recordLike(Long postId, boolean isLike);

    /**
     * 记录收藏操作
     * 
     * @param postId     帖子ID
     * @param isFavorite true表示收藏，false表示取消收藏
     */
    void recordFavorite(Long postId, boolean isFavorite);

    /**
     * 获取帖子浏览量统计（时间序列数据，适合绘制图表）
     * 
     * @param postId 帖子ID
     * @param hours  最近N小时
     * @return 时间序列数据 [{time: timestamp, value: count}, ...]
     */
    List<Map<String, Object>> getPostViewStatistics(Long postId, int hours);

    /**
     * 获取主页访问量统计（时间序列数据）
     * 
     * @param hours 最近N小时
     * @return 时间序列数据
     */
    List<Map<String, Object>> getHomePageViewStatistics(int hours);

    /**
     * 获取点赞变化曲线（时间序列数据）
     * 
     * @param postId 帖子ID（可选，null表示所有帖子）
     * @param hours  最近N小时
     * @return 时间序列数据
     */
    List<Map<String, Object>> getLikeStatistics(Long postId, int hours);

    /**
     * 获取收藏变化曲线（时间序列数据）
     * 
     * @param postId 帖子ID（可选，null表示所有帖子）
     * @param hours  最近N小时
     * @return 时间序列数据
     */
    List<Map<String, Object>> getFavoriteStatistics(Long postId, int hours);

    /**
     * 获取综合统计（包含浏览量、点赞、收藏）
     * 
     * @param postId 帖子ID（可选，null表示所有帖子）
     * @param hours  最近N小时
     * @return 综合统计数据
     */
    Map<String, Object> getComprehensiveStatistics(Long postId, int hours);

    /**
     * 获取一段时间内每天的UV统计
     * 
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @param startDate    开始日期（格式：yyyyMMdd）
     * @param endDate      结束日期（格式：yyyyMMdd）
     * @return 每天的UV统计，key为日期（yyyyMMdd），value为UV数
     */
    Map<String, Long> getDailyUVInRange(ResourceType resourceType, Long resourceId,
            String startDate, String endDate);

    /**
     * 获取帖子在一段时间内的统计数据（UV、点赞、收藏、评论）
     * 
     * @param postId    帖子ID
     * @param startDate 开始日期（格式：yyyyMMdd）
     * @param endDate   结束日期（格式：yyyyMMdd）
     * @return 统计数据，包含每日的UV、点赞数、收藏数、评论数
     */
    Map<String, Object> getPostStatisticsInRange(Long postId, String startDate, String endDate);
}
