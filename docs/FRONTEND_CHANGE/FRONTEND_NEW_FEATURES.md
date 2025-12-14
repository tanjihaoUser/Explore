# 前端新功能实现说明

## 概述

本次更新为前端添加了以下新功能：
1. 流量走势图页面 - 使用 ECharts 绘制统计数据图表
2. 帖子浏览量显示 - 在帖子详情页和列表页显示浏览量
3. 通知中心 - Header 右上角添加通知图标，支持查看和管理通知
4. 发现页面 - 展示推荐用户，支持关注和查看主页

## 新增文件

### 后端文件
- `RecommendationController.java` - 推荐服务控制器
- `StatisticsController.java` - 统计服务控制器（已存在，已确认HTTP接口）

### 前端文件
- `frontend/src/api/statistics.js` - 统计API
- `frontend/src/api/notification.js` - 通知API
- `frontend/src/api/recommendation.js` - 推荐API
- `frontend/src/views/StatisticsView.vue` - 流量走势图页面
- `frontend/src/views/DiscoverView.vue` - 发现页面

### 修改的文件
- `PostController.java` - 添加了记录浏览量的功能
- `RelationServiceImpl.java` - 添加了统计记录功能
- `MyHeader.vue` - 添加了通知图标和通知中心
- `PostDetail.vue` - 添加了浏览量显示
- `HomeSimple.vue` - 添加了浏览量显示
- `MyAside.vue` - 添加了统计和发现页面的菜单项
- `router/index.js` - 添加了新页面的路由
- `package.json` - 添加了 echarts 依赖

## HTTP接口清单

### 统计接口
- `GET /api/statistics/post/{postId}/view?hours=24` - 获取帖子浏览量统计
- `GET /api/statistics/homepage/view?hours=24` - 获取主页访问量统计
- `GET /api/statistics/post/{postId}/like?hours=24` - 获取点赞变化曲线
- `GET /api/statistics/post/{postId}/favorite?hours=24` - 获取收藏变化曲线
- `GET /api/statistics/post/{postId}/comprehensive?hours=24` - 获取综合统计

### 通知接口
- `GET /api/notifications/user/{userId}?page=1&pageSize=20` - 获取通知列表
- `GET /api/notifications/user/{userId}/unread?limit=50` - 获取未读通知
- `GET /api/notifications/user/{userId}/unread/count` - 获取未读通知数量
- `PUT /api/notifications/user/{userId}/read/{notificationId}` - 标记单个通知为已读
- `PUT /api/notifications/user/{userId}/read/batch` - 批量标记为已读
- `PUT /api/notifications/user/{userId}/read/all` - 标记所有通知为已读

### 推荐接口
- `GET /api/recommendations/user/{userId}?count=10` - 获取推荐用户列表
- `GET /api/recommendations/user/{userId}/preview?count=10` - 预览推荐用户（不标记）
- `PUT /api/recommendations/user/{userId}/clear` - 清除推荐历史
- `GET /api/recommendations/user/{userId}/candidate-count` - 获取候选用户数量

## 功能说明

### 1. 流量走势图页面

**路由**: `/statistics`

**功能**:
- 支持查看主页访问量、帖子浏览量、点赞变化、收藏变化、综合统计
- 可选择时间范围（6小时、12小时、24小时、48小时、7天）
- 使用 ECharts 绘制折线图和面积图
- 支持综合统计的多指标对比

**使用方式**:
1. 从侧边栏点击"流量走势"菜单
2. 选择要查看的指标类型
3. 选择时间范围
4. 如果是帖子相关统计，输入帖子ID（可选）
5. 点击刷新按钮查看图表

### 2. 帖子浏览量显示

**位置**:
- 帖子详情页：在点赞、收藏按钮旁边显示浏览量
- 帖子列表页：在点赞、收藏、评论按钮旁边显示浏览量

**实现**:
- 查看帖子详情时自动记录浏览量
- 列表页异步加载每个帖子的浏览量（不阻塞主流程）
- 显示最近24小时的总浏览量

### 3. 通知中心

**位置**: Header 右上角，个人中心左侧

**功能**:
- 显示未读通知数量（红色徽章）
- 鼠标悬浮显示最近5条未读通知预览
- 点击通知图标打开通知中心对话框
- 支持查看所有通知（分页）
- 支持单个通知标记为已读
- 支持批量标记所有通知为已读
- 每30秒自动刷新未读通知数量

**通知类型**:
- 点赞通知
- 收藏通知
- 关注通知
- 评论通知

### 4. 发现页面

**路由**: `/discover`

**功能**:
- 展示随机推荐的用户（默认12个）
- 显示用户头像、用户名、粉丝数、关注数
- 支持关注/取消关注推荐用户
- 支持查看用户主页
- 支持刷新推荐列表
- 支持清除推荐历史（重新推荐之前推荐过的用户）

**使用方式**:
1. 从侧边栏点击"发现"菜单
2. 浏览推荐用户
3. 点击"关注"按钮关注用户
4. 点击"查看主页"查看用户详情
5. 点击"刷新推荐"获取新的推荐用户
6. 点击"清除历史"清除推荐历史

## 安装依赖

前端需要安装 ECharts 依赖：

```bash
cd frontend
npm install echarts
```

## 使用说明

### 查看流量走势图

1. 登录系统
2. 点击侧边栏"流量走势"菜单
3. 选择要查看的指标和时间范围
4. 查看图表

### 查看通知

1. 登录系统
2. 查看 Header 右上角的通知图标
3. 红色数字表示未读通知数量
4. 鼠标悬浮查看通知预览
5. 点击图标打开通知中心

### 发现新用户

1. 登录系统
2. 点击侧边栏"发现"菜单
3. 浏览推荐用户
4. 关注感兴趣的用户

## 注意事项

1. **ECharts 依赖**: 需要运行 `npm install` 安装 echarts
2. **浏览量统计**: 只有在查看帖子详情时才会记录浏览量
3. **通知轮询**: 通知数量每30秒自动刷新一次
4. **推荐用户**: 推荐用户需要后端先添加候选用户到推荐池

## 后续优化建议

1. **实时通知**: 可以使用 WebSocket 实现实时通知推送
2. **图表优化**: 可以添加更多图表类型（柱状图、饼图等）
3. **推荐算法**: 可以根据用户行为优化推荐算法
4. **浏览量缓存**: 可以缓存浏览量数据，减少API调用

