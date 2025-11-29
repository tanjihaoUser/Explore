package com.wait.entity.param;

import lombok.Data;

/**
 * 评论请求
 */
@Data
public class CommentRequest {
    private Long userId;
    private Long postId;
    private String content;
    private Long parentId; // 父评论ID，可选，用于回复功能
}

