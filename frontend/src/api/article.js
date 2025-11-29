import request from "@/utils/request.js";

// 查询所有博客
export function getAll(userId) {
  return request({
    url: "/article/all/" + userId,
    method: "get",
  });
}

// 查询用户下所有博客
export function getByUser(userId) {
  return request({
    url: "/article/user/" + userId,
    method: "get",
  });
}

// 通过标题模糊查询，type不同表示不同条件。0表示没有条件，1表示我发表的
// 2表示回收站中的博客
export function getByTitle(userId, title, type) {
  return request({
    url: "/article/title/" + userId,
    method: "get",
    params: { title: title, type: type },
  });
}

// 我点赞的页面中根据标题模糊查询
export function getByTitleLike(userId, title) {
  return request({
    url: "/article/title/like/" + userId,
    method: "get",
    params: { title: title },
  });
}

// 我收藏的页面中根据标题模糊查询
export function getByTitleCollect(userId, title) {
  return request({
    url: "/article/title/collect/" + userId,
    method: "get",
    params: { title: title },
  });
}

// 根据用户名模糊查询
export function getByUsername(userId, username) {
  return request({
    url: "/article/username/" + userId,
    method: "get",
    params: { username: username },
  });
}

// 实现回收站功能，查询隐藏的博客
export function getDeleted(userId) {
  return request({
    url: "/article/deleted/" + userId,
    method: "get",
  });
}

// 获取我点赞的博客
export function getLike(userId) {
  return request({
    url: "/article/like/" + userId,
    method: "get",
  });
}

// 获取我收藏的博客
export function getCollect(userId) {
  return request({
    url: "/article/collect/" + userId,
    method: "get",
  });
}

// 获取博客详细信息
export function getArticleDetail(userId, articleId) {
  return request({
    url: "/article/detailAll/" + userId + "/" + articleId,
    method: "get",
  });
}

// 用于修改博客时获取初始值，查询出内容及评论展示到前端
export function getUpdateDetail(articleId) {
  return request({
    url: "/article/detail/" + articleId,
    method: "get",
  });
}

// 添加博客
export function insert(data) {
  return request({
    url: "/article",
    method: "post",
    data: data,
  });
}

// 修改详细信息，标题和具体内容
export function changeDetail(data) {
  return request({
    url: "/article/detail",
    method: "put",
    data: data,
  });
}

// 博客的（取消）点赞和收藏
export function addArticleLike(data) {
  return request({
    url: "/article/addLike",
    method: "put",
    data: data,
  });
}

export function subArticleLike(data) {
  return request({
    url: "/article/subLike",
    method: "put",
    data: data,
  });
}

export function addCollect(data) {
  return request({
    url: "/article/addCollect",
    method: "put",
    data: data,
  });
}

export function subCollect(data) {
  return request({
    url: "/article/subCollect",
    method: "put",
    data: data,
  });
}

// 隐藏博客（删除）
export function hideArticle(id) {
  return request({
    url: "/article/" + id,
    method: "put",
  });
}

// 从回收站恢复博客
export function recover(id) {
  return request({
    url: "/article/recover/" + id,
    method: "put",
  });
}

// 彻底删除对应博客
export function deleteArticle(id) {
  return request({
    url: "/article/" + id,
    method: "delete",
  });
}
