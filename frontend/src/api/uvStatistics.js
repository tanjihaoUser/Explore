import request from "@/utils/request.js";

/**
 * 获取资源的 UV 统计
 * @param {string} resourceType 资源类型（post, page, user_profile, category, search）
 * @param {number} resourceId 资源ID
 */
export function getUV(resourceType, resourceId) {
  return request({
    url: "/api/uv-statistics/uv",
    method: "get",
    params: { resourceType, resourceId },
  });
}

/**
 * 获取一段时间内每天的 UV 统计
 * @param {string} resourceType 资源类型
 * @param {number} resourceId 资源ID
 * @param {string} startDate 开始日期（格式：yyyyMMdd）
 * @param {string} endDate 结束日期（格式：yyyyMMdd）
 */
export function getDailyUVInRange(resourceType, resourceId, startDate, endDate) {
  return request({
    url: "/api/uv-statistics/daily-uv",
    method: "get",
    params: { resourceType, resourceId, startDate, endDate },
  });
}

/**
 * 获取用户所有帖子的 UV 统计
 * @param {number} userId 用户ID
 */
export function getUserPostsUV(userId) {
  return request({
    url: "/api/uv-statistics/user-posts",
    method: "get",
    params: { userId },
  });
}

/**
 * 获取帖子在一段时间内的统计数据（UV、点赞、收藏、评论）
 * @param {number} postId 帖子ID
 * @param {string} startDate 开始日期（格式：yyyyMMdd）
 * @param {string} endDate 结束日期（格式：yyyyMMdd）
 */
export function getPostStatisticsInRange(postId, startDate, endDate) {
  return request({
    url: "/api/uv-statistics/post-statistics",
    method: "get",
    params: { postId, startDate, endDate },
  });
}

