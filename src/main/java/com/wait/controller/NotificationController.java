package com.wait.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wait.entity.dto.NotificationDTO;
import com.wait.entity.param.NotificationRequest;
import com.wait.service.NotificationService;
import com.wait.util.ResponseUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 通知控制器
 * 提供通知相关的REST API接口
 * 
 * 数据流程：
 * 1. Service层返回通知ID列表或通知字符串列表
 * 2. Controller层解析通知字符串，构建NotificationDTO对象
 * 3. 返回结构化的通知数据给前端
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 发送通知
     * POST /api/notifications
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> sendNotification(@RequestBody NotificationRequest request) {
        log.info("发送通知: userId={}, type={}, content={}, relatedId={}",
                request.getUserId(), request.getNotificationType(), request.getContent(), request.getRelatedId());

        String notificationId = notificationService.sendNotification(
                request.getUserId(),
                request.getNotificationType(),
                request.getContent(),
                request.getRelatedId());

        Map<String, Object> data = new HashMap<>();
        data.put("notificationId", notificationId);
        data.put("userId", request.getUserId());
        data.put("notificationType", request.getNotificationType());

        return ResponseUtil.success("通知发送成功", data);
    }

    /**
     * 获取用户通知列表（分页）
     * GET /api/notifications/user/{userId}?page=1&pageSize=20
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        log.info("获取用户通知列表: userId={}, page={}, pageSize={}", userId, page, pageSize);

        // 获取通知字符串列表（格式：notificationId:type:content:relatedId）
        List<String> notificationStrings = notificationService.getNotifications(userId, page, pageSize);

        // 获取未读通知ID集合
        Set<String> unreadIds = notificationService.getUnreadNotifications(userId, 0);

        // 解析通知字符串，构建NotificationDTO列表
        List<NotificationDTO> notifications = parseNotifications(notificationStrings, unreadIds);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("notifications", notifications);
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("total", notifications.size());

        return ResponseUtil.success(data);
    }

    /**
     * 获取用户未读通知列表
     * GET /api/notifications/user/{userId}/unread?limit=50
     */
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<Map<String, Object>> getUnreadNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "50") int limit) {
        log.info("获取用户未读通知列表: userId={}, limit={}", userId, limit);

        // 获取未读通知ID集合
        Set<String> unreadIds = notificationService.getUnreadNotifications(userId, limit);

        if (unreadIds.isEmpty()) {
            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("unreadNotifications", new ArrayList<>());
            data.put("count", 0);
            return ResponseUtil.success(data);
        }

        // 获取通知列表（获取足够多的通知以确保包含所有未读通知）
        // 由于通知是按时间倒序排列的，未读通知通常在前面，所以获取前limit*2条应该足够
        int fetchSize = Math.max(limit * 2, 100);
        List<String> notifications = notificationService.getNotifications(userId, 1, fetchSize);

        // 过滤出未读通知并解析
        List<NotificationDTO> unreadNotifications = notifications.stream()
                .filter(notification -> {
                    String notificationId = extractNotificationId(notification);
                    return unreadIds.contains(notificationId);
                })
                .map(notification -> parseNotification(notification, true))
                .limit(limit > 0 ? limit : Long.MAX_VALUE)
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("unreadNotifications", unreadNotifications);
        data.put("count", unreadNotifications.size());

        return ResponseUtil.success(data);
    }

    /**
     * 获取未读通知数量
     * GET /api/notifications/user/{userId}/unread/count
     */
    @GetMapping("/user/{userId}/unread/count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(@PathVariable Long userId) {
        log.info("获取用户未读通知数量: userId={}", userId);

        Long count = notificationService.getUnreadCount(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("unreadCount", count != null ? count : 0L);

        return ResponseUtil.success(data);
    }

    /**
     * 标记通知为已读
     * PUT /api/notifications/user/{userId}/read/{notificationId}
     */
    @PutMapping("/user/{userId}/read/{notificationId}")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable Long userId,
            @PathVariable String notificationId) {
        log.info("标记通知为已读: userId={}, notificationId={}", userId, notificationId);

        Boolean success = notificationService.markAsRead(userId, notificationId);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("notificationId", notificationId);
        data.put("success", success);

        return ResponseUtil.success(success ? "标记为已读成功" : "通知不存在或已读", data);
    }

    /**
     * 批量标记通知为已读
     * PUT /api/notifications/user/{userId}/read/batch
     */
    @PutMapping("/user/{userId}/read/batch")
    public ResponseEntity<Map<String, Object>> markAsReadBatch(
            @PathVariable Long userId,
            @RequestBody Map<String, List<String>> request) {
        List<String> notificationIds = request.get("notificationIds");
        if (notificationIds == null || notificationIds.isEmpty()) {
            return ResponseUtil.badRequest("notificationIds不能为空");
        }

        log.info("批量标记通知为已读: userId={}, count={}", userId, notificationIds.size());

        Long count = notificationService.markAsReadBatch(userId, notificationIds);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("markedCount", count);
        data.put("requestedCount", notificationIds.size());

        return ResponseUtil.success("批量标记为已读成功", data);
    }

    /**
     * 标记所有通知为已读
     * PUT /api/notifications/user/{userId}/read/all
     */
    @PutMapping("/user/{userId}/read/all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(@PathVariable Long userId) {
        log.info("标记所有通知为已读: userId={}", userId);

        Long count = notificationService.markAllAsRead(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("markedCount", count);

        return ResponseUtil.success("所有通知已标记为已读", data);
    }

    /**
     * 删除通知
     * DELETE /api/notifications/user/{userId}/{notificationId}
     */
    @DeleteMapping("/user/{userId}/{notificationId}")
    public ResponseEntity<Map<String, Object>> deleteNotification(
            @PathVariable Long userId,
            @PathVariable String notificationId) {
        log.info("删除通知: userId={}, notificationId={}", userId, notificationId);

        Boolean success = notificationService.deleteNotification(userId, notificationId);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("notificationId", notificationId);
        data.put("success", success);

        return ResponseUtil.success(success ? "删除成功" : "通知不存在", data);
    }

    /**
     * 解析通知字符串列表为NotificationDTO列表
     * 通知格式：notificationId:type:content:relatedId
     */
    private List<NotificationDTO> parseNotifications(List<String> notificationStrings, Set<String> unreadIds) {
        if (notificationStrings == null || notificationStrings.isEmpty()) {
            return new ArrayList<>();
        }

        return notificationStrings.stream()
                .map(notification -> {
                    String notificationId = extractNotificationId(notification);
                    boolean isRead = !unreadIds.contains(notificationId);
                    return parseNotification(notification, isRead);
                })
                .collect(Collectors.toList());
    }

    /**
     * 解析单个通知字符串为NotificationDTO
     * 通知格式：notificationId:type:content:relatedId
     */
    private NotificationDTO parseNotification(String notification, boolean isRead) {
        if (notification == null || notification.isEmpty()) {
            return null;
        }

        String[] parts = notification.split(":", 4);
        if (parts.length < 3) {
            log.warn("通知格式错误: {}", notification);
            return NotificationDTO.builder()
                    .notificationId("")
                    .notificationType("unknown")
                    .content(notification)
                    .isRead(isRead)
                    .build();
        }

        String notificationId = parts[0];
        String notificationType = parts[1];
        String content = parts.length > 2 ? parts[2] : "";
        Long relatedId = null;

        // 解析relatedId（可能为空）
        if (parts.length > 3 && !parts[3].isEmpty()) {
            try {
                relatedId = Long.parseLong(parts[3]);
            } catch (NumberFormatException e) {
                log.warn("通知relatedId格式错误: {}", parts[3]);
            }
        }

        return NotificationDTO.builder()
                .notificationId(notificationId)
                .notificationType(notificationType)
                .content(content)
                .relatedId(relatedId)
                .isRead(isRead)
                .build();
    }

    /**
     * 从通知字符串中提取通知ID
     * 通知格式：notificationId:type:content:relatedId
     */
    private String extractNotificationId(String notification) {
        if (notification == null || notification.isEmpty()) {
            return "";
        }

        int index = notification.indexOf(':');
        if (index > 0) {
            return notification.substring(0, index);
        }

        return notification;
    }
}
