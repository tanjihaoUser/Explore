package com.wait.entity.param;

import lombok.Data;

/**
 * 发送通知请求
 */
@Data
public class NotificationRequest {
    private Long userId;
    private String notificationType; // 通知类型（如：like, comment, follow, system）
    private String content; // 通知内容
    private Long relatedId; // 相关ID（如：帖子ID、评论ID等）
}

