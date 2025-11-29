import request from "@/utils/request.js";

// ==================== 时间线相关 ====================

// 获取用户时间线
export function getUserTimeline(userId, page = 1, pageSize = 20, currentUserId = null) {
  return request({
    url: `/api/ranking/timeline/user/${userId}`,
    method: "get",
    params: { page, pageSize, currentUserId },
  });
}

// 获取全局时间线
export function getGlobalTimeline(page = 1, pageSize = 20, currentUserId = null) {
  return request({
    url: "/api/ranking/timeline/global",
    method: "get",
    params: { page, pageSize, currentUserId },
  });
}

// 获取我的时间线（聚合关注用户的帖子）
export function getMyTimeline(userId, page = 1, pageSize = 20, currentUserId = null) {
  return request({
    url: `/api/ranking/timeline/my/${userId}`,
    method: "get",
    params: { page, pageSize, currentUserId },
  });
}

// ==================== 热度排行榜相关 ====================

// 获取热门帖子排行榜
export function getHotPosts(period = "daily", page = 1, pageSize = 20, currentUserId = null) {
  return request({
    url: "/api/ranking/hot",
    method: "get",
    params: { period, page, pageSize, currentUserId },
  });
}

// 获取帖子在排行榜中的排名
export function getPostRank(postId, period = "daily") {
  return request({
    url: `/api/ranking/hot/${postId}/rank`,
    method: "get",
    params: { period },
  });
}

// ==================== 单项排行榜相关 ====================

// 获取点赞数排行榜
export function getLikesRanking(page = 1, pageSize = 20, currentUserId = null) {
  return request({
    url: "/api/ranking/likes",
    method: "get",
    params: { page, pageSize, currentUserId },
  });
}

// 获取收藏数排行榜
export function getFavoritesRanking(page = 1, pageSize = 20, currentUserId = null) {
  return request({
    url: "/api/ranking/favorites",
    method: "get",
    params: { page, pageSize, currentUserId },
  });
}

// 获取评论数排行榜
export function getCommentsRanking(page = 1, pageSize = 20, currentUserId = null) {
  return request({
    url: "/api/ranking/comments",
    method: "get",
    params: { page, pageSize, currentUserId },
  });
}

