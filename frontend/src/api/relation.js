import request from "@/utils/request.js";

// ==================== 关注相关 ====================

// 关注用户
export function follow(followerId, followedId) {
  return request({
    url: "/api/relation/follow",
    method: "post",
    data: { followerId, followedId },
  });
}

// 取消关注
export function unfollow(followerId, followedId) {
  return request({
    url: "/api/relation/follow",
    method: "delete",
    params: { followerId, followedId },
  });
}

// 检查是否关注
export function checkFollowing(followerId, followedId) {
  return request({
    url: "/api/relation/follow/check",
    method: "get",
    params: { followerId, followedId },
  });
}

// 获取关注列表
export function getFollowing(userId) {
  return request({
    url: `/api/relation/follow/${userId}/following`,
    method: "get",
  });
}

// 获取粉丝列表
export function getFollowers(userId) {
  return request({
    url: `/api/relation/follow/${userId}/followers`,
    method: "get",
  });
}

// 获取关注数和粉丝数
export function getFollowCount(userId) {
  return request({
    url: `/api/relation/follow/${userId}/count`,
    method: "get",
  });
}

// ==================== 点赞相关 ====================

// 点赞帖子
export function likePost(userId, postId) {
  return request({
    url: "/api/relation/like",
    method: "post",
    data: { userId, postId },
  });
}

// 取消点赞
export function unlikePost(userId, postId) {
  return request({
    url: "/api/relation/like",
    method: "delete",
    params: { userId, postId },
  });
}

// 检查是否已点赞
export function checkLiked(userId, postId) {
  return request({
    url: "/api/relation/like/check",
    method: "get",
    params: { userId, postId },
  });
}

// 批量检查点赞状态
export function batchCheckLiked(userId, postIds) {
  return request({
    url: "/api/relation/like/batch-check",
    method: "post",
    data: { userId, postIds },
  });
}

// 获取点赞数
export function getLikeCount(postId) {
  return request({
    url: `/api/relation/like/${postId}/count`,
    method: "get",
  });
}

// 获取用户点赞过的帖子列表
export function getUserLikedPosts(userId) {
  return request({
    url: `/api/relation/like/user/${userId}/posts`,
    method: "get",
  });
}

// ==================== 收藏相关 ====================

// 收藏帖子
export function favoritePost(userId, postId) {
  return request({
    url: "/api/relation/favorite",
    method: "post",
    data: { userId, postId },
  });
}

// 取消收藏
export function unfavoritePost(userId, postId) {
  return request({
    url: "/api/relation/favorite",
    method: "delete",
    params: { userId, postId },
  });
}

// 检查是否已收藏
export function checkFavorited(userId, postId) {
  return request({
    url: "/api/relation/favorite/check",
    method: "get",
    params: { userId, postId },
  });
}

// 获取用户收藏列表
export function getUserFavorites(userId) {
  return request({
    url: `/api/relation/favorite/user/${userId}`,
    method: "get",
  });
}

// 获取收藏数
export function getFavoriteCount(postId) {
  return request({
    url: `/api/relation/favorite/${postId}/count`,
    method: "get",
  });
}

// ==================== 黑名单相关 ====================

// 拉黑用户
export function blockUser(userId, blockedUserId) {
  return request({
    url: "/api/relation/block",
    method: "post",
    data: { userId, blockedUserId },
  });
}

// 取消拉黑
export function unblockUser(userId, blockedUserId) {
  return request({
    url: "/api/relation/block",
    method: "delete",
    params: { userId, blockedUserId },
  });
}

// 检查是否在黑名单中
export function checkBlocked(userId, blockedUserId) {
  return request({
    url: "/api/relation/block/check",
    method: "get",
    params: { userId, blockedUserId },
  });
}

// 获取黑名单列表
export function getBlacklist(userId) {
  return request({
    url: `/api/relation/block/user/${userId}`,
    method: "get",
  });
}

