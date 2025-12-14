import request from "@/utils/request.js";

// 创建帖子
export function createPost(data) {
  return request({
    url: "/api/posts",
    method: "post",
    data: data,
  });
}

// 获取帖子详情
export function getPostById(postId, userId = null) {
  return request({
    url: `/api/posts/${postId}`,
    method: "get",
    params: userId ? { userId } : {},
  });
}

// 获取用户的帖子列表
export function getPostsByUserId(userId, page = 1, size = 20, currentUserId = null) {
  return request({
    url: `/api/posts/user/${userId}`,
    method: "get",
    params: { page, size, currentUserId },
  });
}

// 更新帖子
export function updatePost(data) {
  return request({
    url: "/api/posts",
    method: "put",
    data: data,
  });
}

// 删除帖子
export function deletePost(userId, postId) {
  return request({
    url: "/api/posts",
    method: "delete",
    params: { userId, postId },
  });
}

