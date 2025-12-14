import request from "@/utils/request.js";

// 获取推荐用户列表
export function getRecommendedUsers(userId, count = 10) {
  return request({
    url: `/api/recommendations/user/${userId}`,
    method: "get",
    params: { count },
  });
}

// 预览推荐用户列表（不标记为已推荐）
export function previewRecommendedUsers(userId, count = 10) {
  return request({
    url: `/api/recommendations/user/${userId}/preview`,
    method: "get",
    params: { count },
  });
}

// 清除推荐历史
export function clearRecommendedHistory(userId) {
  return request({
    url: `/api/recommendations/user/${userId}/clear`,
    method: "put",
  });
}

// 获取候选用户数量
export function getCandidateCount(userId) {
  return request({
    url: `/api/recommendations/user/${userId}/candidate-count`,
    method: "get",
  });
}

