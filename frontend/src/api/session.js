import request from "@/utils/request.js";

// 登录并创建会话
export function login(username, password) {
  return request({
    url: "/api/sessions/login",
    method: "post",
    data: { username, password },
  });
}

// 获取会话信息
export function getSession(sessionId) {
  return request({
    url: `/api/sessions/${sessionId}`,
    method: "get",
  });
}

// 更新会话
export function updateSession(sessionId, sessionData) {
  return request({
    url: `/api/sessions/${sessionId}`,
    method: "put",
    data: sessionData,
  });
}

// 记录用户活动
export function recordActivity(sessionId) {
  return request({
    url: `/api/sessions/${sessionId}/activity`,
    method: "post",
  });
}

// 更新当前页面
export function updateCurrentPage(sessionId, currentPage) {
  return request({
    url: `/api/sessions/${sessionId}/current-page`,
    method: "put",
    data: { currentPage },
  });
}

// 登出并删除会话
export function logout(sessionId) {
  return request({
    url: `/api/sessions/${sessionId}`,
    method: "delete",
  });
}

// 验证会话有效性
export function validateSession(sessionId) {
  return request({
    url: `/api/sessions/${sessionId}/validate`,
    method: "get",
  });
}

