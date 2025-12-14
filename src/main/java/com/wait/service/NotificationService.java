package com.wait.service;

import java.util.List;
import java.util.Set;

/**
 * 系统通知服务
 * 使用 Redis List 存储通知列表，Set 存储未读通知
 */
public interface NotificationService {

    /**
     * 发送通知给用户（同步）
     * @param userId 用户ID
     * @param notificationType 通知类型（如：like, comment, follow, system）
     * @param content 通知内容
     * @param relatedId 相关ID（如：帖子ID、评论ID等）
     * @return 通知ID
     */
    String sendNotification(Long userId, String notificationType, String content, Long relatedId);

    /**
     * 异步发送通知给用户（不阻塞主流程）
     * @param userId 用户ID
     * @param notificationType 通知类型（如：like, comment, follow, system）
     * @param content 通知内容
     * @param relatedId 相关ID（如：帖子ID、评论ID等）
     */
    void sendNotificationAsync(Long userId, String notificationType, String content, Long relatedId);

    /**
     * 获取用户通知列表
     * @param userId 用户ID
     * @param page 页码（从1开始）
     * @param pageSize 每页数量
     * @return 通知列表（从新到旧）
     */
    List<String> getNotifications(Long userId, int page, int pageSize);

    /**
     * 获取用户未读通知列表
     * @param userId 用户ID
     * @param limit 返回数量限制
     * @return 未读通知ID列表
     */
    Set<String> getUnreadNotifications(Long userId, int limit);

    /**
     * 标记通知为已读
     * @param userId 用户ID
     * @param notificationId 通知ID
     * @return 是否成功
     */
    Boolean markAsRead(Long userId, String notificationId);

    /**
     * 批量标记通知为已读
     * @param userId 用户ID
     * @param notificationIds 通知ID列表
     * @return 标记成功的数量
     */
    Long markAsReadBatch(Long userId, List<String> notificationIds);

    /**
     * 标记所有通知为已读
     * @param userId 用户ID
     * @return 标记成功的数量
     */
    Long markAllAsRead(Long userId);

    /**
     * 获取未读通知数量
     * @param userId 用户ID
     * @return 未读通知数量
     */
    Long getUnreadCount(Long userId);

    /**
     * 删除通知
     * @param userId 用户ID
     * @param notificationId 通知ID
     * @return 是否成功
     */
    Boolean deleteNotification(Long userId, String notificationId);
}

