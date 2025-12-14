import request from "@/utils/request.js";

// 获取用户通知列表（分页）
export function getNotifications(userId, page = 1, pageSize = 20) {
  return request({
    url: `/api/notifications/user/${userId}`,
    method: "get",
    params: { page, pageSize },
  });
}

// 获取用户未读通知列表
export function getUnreadNotifications(userId, limit = 50) {
  return request({
    url: `/api/notifications/user/${userId}/unread`,
    method: "get",
    params: { limit },
  });
}

// 获取未读通知数量
export function getUnreadCount(userId) {
  return request({
    url: `/api/notifications/user/${userId}/unread/count`,
    method: "get",
  });
}

// 标记通知为已读
export function markAsRead(userId, notificationId) {
  return request({
    url: `/api/notifications/user/${userId}/read/${notificationId}`,
    method: "put",
  });
}

// 批量标记通知为已读
export function markAsReadBatch(userId, notificationIds) {
  return request({
    url: `/api/notifications/user/${userId}/read/batch`,
    method: "put",
    data: { notificationIds },
  });
}

// 标记所有通知为已读
export function markAllAsRead(userId) {
  return request({
    url: `/api/notifications/user/${userId}/read/all`,
    method: "put",
  });
}

// 删除通知
export function deleteNotification(userId, notificationId) {
  return request({
    url: `/api/notifications/user/${userId}/${notificationId}`,
    method: "delete",
  });
}

