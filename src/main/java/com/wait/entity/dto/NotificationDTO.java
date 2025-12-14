package com.wait.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知数据传输对象
 * 用于API响应，包含解析后的通知信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private String notificationId; // 通知ID
    private String notificationType; // 通知类型（如：like, comment, follow, system）
    private String content; // 通知内容
    private Long relatedId; // 相关ID（如：帖子ID、评论ID等）
    private Boolean isRead; // 是否已读
}

