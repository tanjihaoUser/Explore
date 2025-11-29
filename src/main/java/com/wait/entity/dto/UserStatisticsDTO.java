package com.wait.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户统计数据DTO
 * 用于展示用户的帖子数、点赞总数、评论总数等信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatisticsDTO {
    private Long userId;
    private String username;
    private Long postCount;        // 帖子数
    private Long totalLikeCount;   // 所有帖子的点赞总数
    private Long totalFavoriteCount; // 所有帖子的收藏总数
    private Long totalCommentCount; // 所有帖子的评论总数
    private Double score;           // 综合评分（用于排序）
    private Boolean isFollowing;   // 当前用户是否关注了该用户（可选）
}

