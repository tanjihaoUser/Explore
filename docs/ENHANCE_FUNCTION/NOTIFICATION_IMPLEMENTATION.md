# 通知系统实现说明

## 1. 概述

通知系统使用 Redis List 和 Set 数据结构实现，提供用户通知的发送、查询、标记和删除功能。所有关键操作使用 Lua 脚本确保原子性。

## 2. Redis 存储结构

### 2.1 数据结构设计

#### 2.1.1 通知列表（List）
- **Key 格式**: `notification:user:{userId}`
- **数据结构**: Redis List
- **存储内容**: 通知字符串列表（按时间倒序，最新在前）
- **存储格式**: `notificationId:type:content:relatedId`
- **限制**: 默认最多保留 500 条通知（通过 `LTRIM` 限制）

#### 2.1.2 未读通知集合（Set）
- **Key 格式**: `notification:unread:{userId}`
- **数据结构**: Redis Set
- **存储内容**: 未读通知ID集合
- **用途**: 快速查询未读通知数量和列表

### 2.2 存储格式说明

#### 当前实现格式
```
notificationId:type:content:relatedId
```

**示例**:
```
abc-123-uuid:like:用户张三点赞了你的帖子:456
def-456-uuid:comment:用户李四评论了你的帖子:789
```

**优点**:
- 简单直接，存储空间小
- 解析速度快
- 适合简单的通知场景

**缺点**:
- 扩展性差，新增字段需要修改格式
- 内容中不能包含冒号（`:`），需要转义
- 无法存储复杂结构（如嵌套对象、数组等）

#### 业界常见做法

##### 方案一：JSON 格式（推荐）
```json
{
  "notificationId": "abc-123-uuid",
  "type": "like",
  "content": "用户张三点赞了你的帖子",
  "relatedId": 456,
  "timestamp": 1703123456789,
  "extra": {
    "actorId": 123,
    "actorName": "张三",
    "postTitle": "我的帖子"
  }
}
```

**优点**:
- 结构清晰，易于扩展
- 支持复杂数据结构
- 便于序列化和反序列化
- 业界主流做法

**缺点**:
- 存储空间稍大
- 需要 JSON 解析（性能影响可忽略）

##### 方案二：Hash 结构
```
Key: notification:detail:{notificationId}
Hash Fields:
  - id: notificationId
  - type: like
  - content: 用户张三点赞了你的帖子
  - relatedId: 456
  - timestamp: 1703123456789
  - actorId: 123
  - actorName: 张三
```

**优点**:
- 可以单独更新某个字段
- 支持部分查询（HGET）
- 结构清晰

**缺点**:
- 需要额外的 Hash 查询
- 存储空间较大（每个字段都有 key）

##### 方案三：混合方案（最佳实践）
- **List 存储**: 仅存储 `notificationId`（节省空间）
- **Hash 存储**: 存储通知详情 `notification:detail:{notificationId}`
- **Set 存储**: 未读通知ID集合

**优点**:
- 空间效率高（List 只存 ID）
- 查询灵活（可以只查 ID 列表，也可以查详情）
- 支持通知详情的单独更新
- 业界大型系统常用方案

**示例**:
```redis
# List: 只存储通知ID
LPUSH notification:user:1 "abc-123-uuid"
LPUSH notification:user:1 "def-456-uuid"

# Hash: 存储通知详情
HSET notification:detail:abc-123-uuid id "abc-123-uuid" type "like" content "用户张三点赞了你的帖子" relatedId 456 timestamp 1703123456789

# Set: 未读通知ID
SADD notification:unread:1 "abc-123-uuid"
```

### 2.3 本项目存储格式选择

**当前实现**: 字符串格式 `notificationId:type:content:relatedId`

**选择原因**:
- 项目初期，通知功能简单
- 减少存储空间
- 降低实现复杂度

**未来优化方向**:
- 如果通知内容需要扩展（如添加头像、链接等），建议迁移到 JSON 格式
- 如果通知数量很大，建议采用混合方案（List 存 ID + Hash 存详情）

## 3. 核心功能实现

### 3.1 发送通知

**Lua 脚本**: `send_notification.lua`

**原子操作**:
1. `LPUSH` - 将通知添加到列表头部
2. `SADD` - 添加到未读通知集合
3. `LTRIM` - 限制通知数量（防止无限增长）

**代码示例**:
```java
String notificationId = notificationService.sendNotification(
    userId,           // 接收通知的用户ID
    "like",           // 通知类型
    "用户张三点赞了你的帖子",  // 通知内容
    456L              // 相关ID（帖子ID）
);
```

### 3.2 获取通知列表

**实现方式**:
- 使用 `LRANGE` 获取分页通知
- 解析通知字符串为 `NotificationDTO` 对象
- 根据未读集合设置 `isRead` 状态

**代码示例**:
```java
List<String> notifications = notificationService.getNotifications(userId, 1, 20);
// 返回格式: ["abc-123:like:用户张三点赞了你的帖子:456", ...]
```

### 3.3 标记为已读

**单个标记**: `SREM` 从未读集合中移除
**批量标记**: `SREM` 批量移除
**全部标记**: `DEL` 删除整个未读集合（使用 Lua 脚本先获取数量）

### 3.4 删除通知

**Lua 脚本**: `delete_notification.lua`

**原子操作**:
1. 遍历列表，找到匹配的通知ID
2. `LREM` - 从列表中删除
3. `SREM` - 从未读集合中删除

## 4. 通知触发场景

### 4.1 常见通知场景

#### 4.1.1 评论通知
**触发时机**: 用户评论帖子或回复评论时
**接收者**: 帖子作者、被回复的评论作者
**通知类型**: `comment`、`reply`

**示例**:
```java
// 在 CommentServiceImpl.createComment() 中
Comment comment = commentService.createComment(userId, postId, content, parentId);

// 获取帖子作者
Post post = postMapper.selectById(postId);
Long postAuthorId = post.getUserId();

// 发送通知给帖子作者（如果不是自己评论的）
if (!postAuthorId.equals(userId)) {
    notificationService.sendNotification(
        postAuthorId,
        "comment",
        String.format("用户%s评论了你的帖子", getUsername(userId)),
        postId
    );
}

// 如果是回复评论，还要通知被回复的用户
if (parentId != null) {
    Comment parentComment = commentMapper.selectById(parentId);
    Long parentCommentAuthorId = parentComment.getUserId();
    if (!parentCommentAuthorId.equals(userId)) {
        notificationService.sendNotification(
            parentCommentAuthorId,
            "reply",
            String.format("用户%s回复了你的评论", getUsername(userId)),
            parentId
        );
    }
}
```

#### 4.1.2 点赞通知
**触发时机**: 用户点赞帖子时
**接收者**: 帖子作者
**通知类型**: `like`

**示例**:
```java
// 在 RelationServiceImpl.likePost() 中
public boolean likePost(Long userId, Long postId) {
    // ... 点赞逻辑 ...
    
    if (success) {
        // 获取帖子作者
        Post post = postService.getPostById(postId);
        Long postAuthorId = post.getUserId();
        
        // 发送通知（如果不是自己点赞的）
        if (!postAuthorId.equals(userId)) {
            notificationService.sendNotification(
                postAuthorId,
                "like",
                String.format("用户%s点赞了你的帖子", getUsername(userId)),
                postId
            );
        }
    }
    
    return success;
}
```

#### 4.1.3 关注通知
**触发时机**: 用户关注其他用户时
**接收者**: 被关注的用户
**通知类型**: `follow`

**示例**:
```java
// 在 RelationServiceImpl.follow() 中
public boolean follow(Long followerId, Long followedId) {
    // ... 关注逻辑 ...
    
    if (success) {
        // 发送通知给被关注的用户
        notificationService.sendNotification(
            followedId,
            "follow",
            String.format("用户%s关注了你", getUsername(followerId)),
            followerId
        );
    }
    
    return success;
}
```

#### 4.1.4 收藏通知（可选）
**触发时机**: 用户收藏帖子时
**接收者**: 帖子作者
**通知类型**: `favorite`

**注意**: 收藏通知通常不是必须的，因为收藏是用户自己的行为，不会直接影响帖子作者。但有些平台会发送，用于让作者知道自己的内容被收藏。

#### 4.1.5 系统通知
**触发时机**: 系统操作（如账号异常、内容审核等）
**接收者**: 相关用户
**通知类型**: `system`

### 4.2 通知发送策略

#### 4.2.1 实时发送（当前实现）
- 业务操作完成后立即发送通知
- 优点: 及时性好
- 缺点: 可能影响业务操作性能

#### 4.2.2 异步发送（推荐）
- 业务操作完成后，通过消息队列异步发送通知
- 优点: 不影响业务性能，可批量处理
- 缺点: 可能有延迟

**示例**:
```java
// 使用消息队列异步发送
@Async
public void sendNotificationAsync(Long userId, String type, String content, Long relatedId) {
    notificationService.sendNotification(userId, type, content, relatedId);
}

// 在业务代码中
if (success) {
    notificationService.sendNotificationAsync(postAuthorId, "like", content, postId);
}
```

#### 4.2.3 批量发送
- 将多个通知合并为一条（如"3个用户点赞了你的帖子"）
- 优点: 减少通知数量，提升用户体验
- 缺点: 实现复杂度较高

### 4.3 通知去重

**场景**: 同一用户短时间内多次操作（如连续点赞、取消点赞、再点赞）

**实现方式**:
1. 使用 Redis Set 记录最近发送的通知（设置过期时间）
2. 发送前检查是否已存在
3. 如果存在，更新现有通知或跳过

**示例**:
```java
// 使用 Set 记录最近1小时内的通知
String recentKey = "notification:recent:" + userId + ":" + type + ":" + relatedId;
Boolean exists = boundUtil.sIsMember(recentKey, userId);
if (exists) {
    // 已发送过，跳过或更新
    return;
}

// 发送通知
notificationService.sendNotification(userId, type, content, relatedId);

// 记录已发送（1小时过期）
boundUtil.sAdd(recentKey, userId);
boundUtil.expire(recentKey, 3600);
```

## 5. API 接口说明

### 5.1 接口列表

| 方法 | HTTP | 路径 | 功能 |
|------|------|------|------|
| `sendNotification` | POST | `/api/notifications` | 发送通知 |
| `getNotifications` | GET | `/api/notifications/user/{userId}` | 获取通知列表（分页） |
| `getUnreadNotifications` | GET | `/api/notifications/user/{userId}/unread` | 获取未读通知列表 |
| `getUnreadCount` | GET | `/api/notifications/user/{userId}/unread/count` | 获取未读通知数量 |
| `markAsRead` | PUT | `/api/notifications/user/{userId}/read/{notificationId}` | 标记单个通知为已读 |
| `markAsReadBatch` | PUT | `/api/notifications/user/{userId}/read/batch` | 批量标记为已读 |
| `markAllAsRead` | PUT | `/api/notifications/user/{userId}/read/all` | 标记所有为已读 |
| `deleteNotification` | DELETE | `/api/notifications/user/{userId}/{notificationId}` | 删除通知 |

### 5.2 使用场景

#### 场景一：用户查看通知列表
```bash
# 获取第一页通知（每页20条）
GET /api/notifications/user/1?page=1&pageSize=20

# 响应
{
  "success": true,
  "data": {
    "userId": 1,
    "notifications": [
      {
        "notificationId": "abc-123-uuid",
        "notificationType": "like",
        "content": "用户张三点赞了你的帖子",
        "relatedId": 456,
        "isRead": false
      }
    ],
    "page": 1,
    "pageSize": 20,
    "total": 10
  }
}
```

#### 场景二：显示未读通知数量（小红点）
```bash
# 获取未读数量
GET /api/notifications/user/1/unread/count

# 响应
{
  "success": true,
  "data": {
    "userId": 1,
    "unreadCount": 5
  }
}
```

#### 场景三：用户点击通知后标记为已读
```bash
# 标记单个为已读
PUT /api/notifications/user/1/read/abc-123-uuid

# 标记所有为已读
PUT /api/notifications/user/1/read/all
```

#### 场景四：用户删除通知
```bash
# 删除通知
DELETE /api/notifications/user/1/abc-123-uuid
```

## 6. 性能优化建议

### 6.1 列表大小限制
- 使用 `LTRIM` 限制通知列表大小（默认500条）
- 超出部分自动删除，避免内存无限增长

### 6.2 分页查询
- 使用 `LRANGE` 实现分页，避免一次性加载所有通知
- 建议每页20-50条

### 6.3 未读集合优化
- 定期清理已读通知的ID（避免Set过大）
- 或使用过期时间自动清理

### 6.4 批量操作
- 使用 `SREM` 批量标记已读
- 使用 `DEL` 一次性标记所有为已读

### 6.5 异步处理
- 通知发送使用异步方式，避免阻塞业务操作
- 使用消息队列批量处理通知

## 7. 扩展功能

### 7.1 通知分类
- 按类型分类显示（点赞、评论、关注等）
- 使用不同的 Redis Key 存储不同类型通知

### 7.2 通知推送
- 集成 WebSocket 实现实时推送
- 集成第三方推送服务（如极光推送、个推等）

### 7.3 通知设置
- 允许用户设置通知偏好（哪些类型接收，哪些不接收）
- 使用 Redis Hash 存储用户通知设置

### 7.4 通知统计
- 统计各类通知的数量
- 统计通知的阅读率

## 8. 总结

### 8.1 当前实现特点
- ✅ 使用 Redis List + Set 实现基础通知功能
- ✅ 使用 Lua 脚本确保原子性
- ✅ 支持分页查询和未读标记
- ✅ 提供完整的 REST API 接口

### 8.2 存储格式
- **当前**: 字符串格式 `notificationId:type:content:relatedId`
- **适用场景**: 简单通知，内容固定
- **未来优化**: 如需扩展，建议迁移到 JSON 格式或混合方案

### 8.3 通知触发
- **当前状态**: 通知功能已实现，但尚未集成到业务逻辑中
- **建议集成**: 在 `RelationServiceImpl`、`CommentServiceImpl` 等业务服务中调用 `NotificationService`
- **推荐方式**: 使用异步方式发送通知，避免影响业务性能

### 8.4 最佳实践
1. **通知发送**: 使用异步方式，通过消息队列处理
2. **通知去重**: 避免短时间内重复通知
3. **通知合并**: 对于批量操作，考虑合并通知（如"3个用户点赞了你的帖子"）
4. **存储优化**: 根据业务规模选择合适的存储格式（简单场景用字符串，复杂场景用JSON或混合方案）

