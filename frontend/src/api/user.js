import request from "@/utils/request.js";

// 获取用户信息
export function getUserInfo(userId) {
  return request({
    url: `/api/users/${userId}`,
    method: "get",
  });
}

// 获取用户排行榜
export function getUserRanking(page = 1, pageSize = 20, currentUserId = null) {
  return request({
    url: "/api/users/ranking",
    method: "get",
    params: { page, pageSize, currentUserId },
  });
}

// 修改密码
export function updatePassword(userId, oldPassword, newPassword) {
  return request({
    url: `/api/users/${userId}/password`,
    method: "put",
    data: { oldPassword, newPassword },
  });
}
