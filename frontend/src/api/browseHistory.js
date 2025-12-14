import request from "@/utils/request.js";

// 记录浏览历史
export function recordBrowse(userId, postId) {
  return request({
    url: "/api/browse-history",
    method: "post",
    data: { userId, postId },
  });
}

// 获取浏览历史（最近N条）
export function getBrowseHistory(userId, limit = 20) {
  return request({
    url: `/api/browse-history/user/${userId}`,
    method: "get",
    params: { limit },
  });
}

// 分页获取浏览历史
export function getBrowseHistoryPage(userId, page = 1, pageSize = 20) {
  return request({
    url: `/api/browse-history/user/${userId}/page`,
    method: "get",
    params: { page, pageSize },
  });
}

// 按时间范围查询浏览记录
export function getBrowseHistoryByTimeRange(userId, startTime, endTime) {
  return request({
    url: `/api/browse-history/user/${userId}/range`,
    method: "get",
    params: { startTime, endTime },
  });
}

// 检查是否浏览过指定帖子
export function hasBrowsed(userId, postId) {
  return request({
    url: `/api/browse-history/user/${userId}/post/${postId}/has-browsed`,
    method: "get",
  });
}

// 获取浏览时间
export function getBrowseTime(userId, postId) {
  return request({
    url: `/api/browse-history/user/${userId}/post/${postId}/time`,
    method: "get",
  });
}

// 获取浏览历史总数
export function getBrowseHistoryCount(userId) {
  return request({
    url: `/api/browse-history/user/${userId}/count`,
    method: "get",
  });
}

// 清除浏览历史
export function clearBrowseHistory(userId) {
  return request({
    url: `/api/browse-history/user/${userId}`,
    method: "delete",
  });
}

// 清除指定时间之前的浏览记录
export function clearOldBrowseHistory(userId, expireTime) {
  return request({
    url: `/api/browse-history/user/${userId}/old`,
    method: "delete",
    params: { expireTime },
  });
}

