import request from "@/utils/request.js";

// 发表评论
export function createComment(data) {
  return request({
    url: "/api/comments",
    method: "post",
    data: data,
  });
}

// 删除评论
export function deleteComment(commentId, userId) {
  return request({
    url: `/api/comments/${commentId}`,
    method: "delete",
    params: { userId },
  });
}

// 获取帖子的评论列表
export function getPostComments(postId, page = 1, pageSize = 20) {
  return request({
    url: `/api/comments/post/${postId}`,
    method: "get",
    params: { page, pageSize },
  });
}

// 获取帖子的顶级评论列表
export function getTopLevelComments(postId, page = 1, pageSize = 20) {
  return request({
    url: `/api/comments/post/${postId}/top-level`,
    method: "get",
    params: { page, pageSize },
  });
}

// 获取评论的回复列表
export function getCommentReplies(commentId) {
  return request({
    url: `/api/comments/${commentId}/replies`,
    method: "get",
  });
}

// 获取用户的评论列表
export function getUserComments(userId, page = 1, pageSize = 20) {
  return request({
    url: `/api/comments/user/${userId}`,
    method: "get",
    params: { page, pageSize },
  });
}

// 获取评论详情
export function getCommentById(commentId) {
  return request({
    url: `/api/comments/${commentId}`,
    method: "get",
  });
}

// 获取帖子的评论数
export function getCommentCount(postId) {
  return request({
    url: `/api/comments/post/${postId}/count`,
    method: "get",
  });
}
