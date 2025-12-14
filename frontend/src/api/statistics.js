import request from "@/utils/request.js";

// 获取帖子浏览量统计
export function getPostViewStatistics(postId, hours = 24) {
  return request({
    url: `/api/statistics/post/${postId}/view`,
    method: "get",
    params: { hours },
  });
}

// 获取主页访问量统计
export function getHomePageViewStatistics(hours = 24) {
  return request({
    url: "/api/statistics/homepage/view",
    method: "get",
    params: { hours },
  });
}

// 获取点赞变化曲线
export function getLikeStatistics(postId = null, hours = 24) {
  const url = postId 
    ? `/api/statistics/post/${postId}/like`
    : "/api/statistics/post/like";
  return request({
    url,
    method: "get",
    params: { hours },
  });
}

// 获取收藏变化曲线
export function getFavoriteStatistics(postId = null, hours = 24) {
  const url = postId 
    ? `/api/statistics/post/${postId}/favorite`
    : "/api/statistics/post/favorite";
  return request({
    url,
    method: "get",
    params: { hours },
  });
}

// 获取综合统计
export function getComprehensiveStatistics(postId = null, hours = 24) {
  const url = postId 
    ? `/api/statistics/post/${postId}/comprehensive`
    : "/api/statistics/post/comprehensive";
  return request({
    url,
    method: "get",
    params: { hours },
  });
}

