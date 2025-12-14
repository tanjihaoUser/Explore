package com.wait.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.wait.config.script.NotificationScripts;
import com.wait.service.NotificationService;
import com.wait.util.BoundUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 系统通知服务实现
 * 使用 Redis List 存储通知列表，Set 存储未读通知，关键操作使用 Lua 脚本确保原子性：
 * Redis 命令使用：
 * - LPUSH: 添加通知到列表（最新在前）
 * - LRANGE: 获取通知列表
 * - LTRIM: 限制通知数量
 * - SADD: 添加到未读通知集合
 * - SREM: 从未读通知集合移除
 * - SCARD: 获取未读通知数量
 * - SMEMBERS: 获取未读通知列表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final BoundUtil boundUtil;
    private final NotificationScripts notificationScripts;

    private static final String NOTIFICATION_PREFIX = "notification:user:";
    private static final String UNREAD_PREFIX = "notification:unread:";
    @SuppressWarnings("unused")
    private static final String NOTIFICATION_DETAIL_PREFIX = "notification:detail:"; // 预留字段，用于未来扩展存储通知详情
    private static final int DEFAULT_MAX_NOTIFICATIONS = 500; // 默认最多保留500条通知

    @Override
    public String sendNotification(Long userId, String notificationType, String content, Long relatedId) {
        if (userId == null || notificationType == null || content == null) {
            throw new IllegalArgumentException("User ID, notification type and content cannot be null");
        }

        // 生成通知ID
        String notificationId = UUID.randomUUID().toString();

        // 构建通知数据（JSON格式或简单格式）
        // 格式：notificationId:notificationType:content:relatedId
        String notification = String.format("%s:%s:%s:%s",
                notificationId, notificationType, content,
                relatedId != null ? relatedId.toString() : "");

        // 使用 Lua 脚本原子性地执行：LPUSH + SADD + LTRIM
        List<String> keys = new ArrayList<>();
        keys.add(NOTIFICATION_PREFIX + userId);
        keys.add(UNREAD_PREFIX + userId);

        String result = notificationScripts.executeScript(
                NotificationScripts.SEND_NOTIFICATION,
                keys,
                notificationId,
                notification,
                String.valueOf(DEFAULT_MAX_NOTIFICATIONS));

        if (result != null && result.equals(notificationId)) {
            log.debug("Notification sent: user={}, type={}, id={}", userId, notificationType, notificationId);
            return notificationId;
        } else {
            log.error("Failed to send notification: user={}, type={}, id={}", userId, notificationType, notificationId);
            throw new RuntimeException("Failed to send notification");
        }
    }

    /**
     * 异步发送通知（不阻塞主流程）
     * 使用专门的线程池执行，避免影响业务性能
     */
    @Override
    @Async("notificationExecutor")
    public void sendNotificationAsync(Long userId, String notificationType, String content, Long relatedId) {
        try {
            sendNotification(userId, notificationType, content, relatedId);
            log.debug("Async notification sent successfully: user={}, type={}", userId, notificationType);
        } catch (Exception e) {
            // 异步发送失败不影响主流程，只记录日志
            log.error("Failed to send async notification: user={}, type={}, content={}",
                    userId, notificationType, content, e);
        }
    }

    /**
     * 获取用户通知列表
     * 
     * 返回格式：通知字符串列表，格式为 "notificationId:type:content:relatedId"
     * Controller层需要解析这些字符串，构建NotificationDTO对象返回给前端
     * 
     * @param userId   用户ID
     * @param page     页码（从1开始）
     * @param pageSize 每页数量
     * @return 通知字符串列表（从新到旧），格式：notificationId:type:content:relatedId
     */
    @Override
    public List<String> getNotifications(Long userId, int page, int pageSize) {
        if (userId == null || page < 1 || pageSize < 1) {
            return new ArrayList<>();
        }

        String key = NOTIFICATION_PREFIX + userId;
        long start = (page - 1) * pageSize;
        long end = start + pageSize - 1;

        List<String> notifications = boundUtil.range(key, start, end, String.class);
        return notifications != null ? notifications : new ArrayList<>();
    }

    /**
     * 获取用户未读通知列表
     * 
     * 返回：未读通知ID集合
     * Controller层需要根据这些ID从通知列表中查找对应的通知详情
     * 
     * @param userId 用户ID
     * @param limit  返回数量限制（0表示不限制）
     * @return 未读通知ID集合
     */
    @Override
    public Set<String> getUnreadNotifications(Long userId, int limit) {
        if (userId == null) {
            return Collections.emptySet();
        }

        String key = UNREAD_PREFIX + userId;
        Set<String> unreadIds = boundUtil.sMembers(key, String.class);

        if (unreadIds == null || unreadIds.isEmpty()) {
            return Collections.emptySet();
        }

        // 如果有限制，只返回前limit个
        if (limit > 0 && unreadIds.size() > limit) {
            List<String> list = new ArrayList<>(unreadIds);
            return new HashSet<>(list.subList(0, limit));
        }

        return unreadIds;
    }

    @Override
    public Boolean markAsRead(Long userId, String notificationId) {
        if (userId == null || notificationId == null) {
            return false;
        }

        String key = UNREAD_PREFIX + userId;
        Long removed = boundUtil.sRem(key, notificationId);
        boolean success = removed != null && removed > 0;

        if (success) {
            log.debug("Notification marked as read: user={}, id={}", userId, notificationId);
        }

        return success;
    }

    @Override
    public Long markAsReadBatch(Long userId, List<String> notificationIds) {
        if (userId == null || notificationIds == null || notificationIds.isEmpty()) {
            return 0L;
        }

        String key = UNREAD_PREFIX + userId;
        String[] ids = notificationIds.toArray(new String[0]);
        Long removed = boundUtil.sRem(key, ids);

        if (removed != null && removed > 0) {
            log.debug("{} notifications marked as read: user={}", removed, userId);
        }

        return removed != null ? removed : 0L;
    }

    @Override
    public Long markAllAsRead(Long userId) {
        if (userId == null) {
            return 0L;
        }

        // 使用 Lua 脚本原子性地执行：SMEMBERS + DEL
        List<String> keys = new ArrayList<>();
        keys.add(UNREAD_PREFIX + userId);

        Long count = notificationScripts.executeScript(
                NotificationScripts.MARK_ALL_AS_READ,
                keys);

        if (count != null && count > 0) {
            log.info("All notifications marked as read: user={}, count={}", userId, count);
        }

        return count != null ? count : 0L;
    }

    @Override
    public Long getUnreadCount(Long userId) {
        if (userId == null) {
            return 0L;
        }

        String key = UNREAD_PREFIX + userId;
        Long count = boundUtil.sCard(key);
        return count != null ? count : 0L;
    }

    @Override
    public Boolean deleteNotification(Long userId, String notificationId) {
        if (userId == null || notificationId == null) {
            return false;
        }

        // 使用 Lua 脚本原子性地执行：LREM + SREM
        List<String> keys = new ArrayList<>();
        keys.add(NOTIFICATION_PREFIX + userId);
        keys.add(UNREAD_PREFIX + userId);

        Long deleted = notificationScripts.executeScript(
                NotificationScripts.DELETE_NOTIFICATION,
                keys,
                notificationId);

        boolean success = deleted != null && deleted > 0;
        if (success) {
            log.debug("Notification deleted: user={}, id={}", userId, notificationId);
        } else {
            log.debug("Notification not found or already deleted: user={}, id={}", userId, notificationId);
        }

        return success;
    }

}
