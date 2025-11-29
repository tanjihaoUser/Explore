package com.wait.entity.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评论实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    private Long id;
    private Long postId;
    private Long userId;
    private Long parentId; // 父评论ID，用于回复功能，NULL表示顶级评论
    private String content;
    private Integer likeCount;
    private Integer isDeleted; // 0-未删除，1-已删除
    private Long createdAt; // 创建时间戳（毫秒）
    private Long updatedAt; // 更新时间戳（毫秒）
}

