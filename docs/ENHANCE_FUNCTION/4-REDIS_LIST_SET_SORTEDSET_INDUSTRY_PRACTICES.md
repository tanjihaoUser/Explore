# Redis List、Set、SortedSet 业界常见做法

## 概述

本文档总结业界在 Redis List、Set、SortedSet 三种数据结构上的常见做法和最佳实践，为项目功能扩展提供参考。

---

## 一、List（列表）业界常见做法

### 1.1 消息队列（Message Queue）

#### 1.1.1 基础消息队列

**场景**: 异步任务处理、事件通知、日志收集

**实现方式**:
- **生产者**: `LPUSH` 将消息推入队列
- **消费者**: `RPOP` 或 `BRPOP` 从队列取出消息
- **队列命名**: `queue:{queueName}` 或 `mq:{topic}`

**优势**:
- 简单高效，Redis 单线程保证顺序
- 支持阻塞操作（BRPOP），避免轮询
- 支持多消费者（多个客户端同时 BRPOP）

**示例场景**:
- 邮件发送队列
- 短信发送队列
- 图片处理队列
- 数据同步队列

#### 1.1.2 可靠消息队列（Reliable Queue）

**场景**: 需要保证消息不丢失的场景

**实现方式**:
- **主队列**: `queue:task` - 待处理任务
- **处理中队列**: `queue:processing` - 正在处理的任务
- **失败队列**: `queue:failed` - 处理失败的任务

**流程**:
1. 生产者 `LPUSH` 到 `queue:task`
2. 消费者 `BRPOPLPUSH queue:task queue:processing` 原子性移动
3. 处理成功后从 `queue:processing` 删除
4. 处理失败则保留在 `queue:processing`，定时重试或移到 `queue:failed`

**优势**:
- 消息不会丢失（处理中队列保存）
- 支持消息重试
- 支持失败消息追踪

#### 1.1.3 优先级队列（Priority Queue）

**场景**: 不同优先级的任务需要优先处理

**实现方式**:
- **高优先级队列**: `queue:high`
- **中优先级队列**: `queue:medium`
- **低优先级队列**: `queue:low`

**消费策略**:
- 使用 `BRPOP` 多队列模式：`BRPOP queue:high queue:medium queue:low 0`
- Redis 会按顺序检查队列，优先处理高优先级

**优势**:
- 简单实现优先级
- 支持动态调整优先级

### 1.2 最新动态列表（Recent Activity）

**场景**: 社交媒体最新动态、新闻列表、用户行为记录

**实现方式**:
- **添加**: `LPUSH activity:user:{userId} {activityId}`
- **查询**: `LRANGE activity:user:{userId} 0 19` 获取最新20条
- **限制大小**: `LTRIM activity:user:{userId} 0 99` 只保留最新100条

**优势**:
- 自动保持最新数据
- 查询效率高（O(N)）
- 内存可控（LTRIM 限制大小）

**示例场景**:
- 用户最近浏览记录
- 用户最近点赞记录
- 系统通知列表
- 操作日志

### 1.3 任务队列（Task Queue）

**场景**: 分布式任务调度、定时任务、批量处理

**实现方式**:
- **任务队列**: `task:queue:{type}`
- **任务详情**: Hash 存储任务详情 `task:detail:{taskId}`
- **任务状态**: String 存储任务状态 `task:status:{taskId}`

**流程**:
1. 创建任务，详情存入 Hash，任务ID推入队列
2. Worker 从队列取出任务ID
3. 根据任务ID查询详情，执行任务
4. 更新任务状态

**优势**:
- 解耦任务创建和执行
- 支持多 Worker 并发处理
- 支持任务状态追踪

### 1.4 限流队列（Rate Limiting Queue）

**场景**: API 限流、请求频率控制

**实现方式**:
- **滑动窗口**: `LPUSH rate:limit:{userId} {timestamp}`
- **清理过期**: `LTRIM rate:limit:{userId} 0 99` 只保留最近100个时间戳
- **检查频率**: `LLEN rate:limit:{userId}` 获取请求次数

**优势**:
- 精确控制请求频率
- 支持滑动窗口
- 内存占用可控

### 1.5 发布订阅（Pub/Sub）替代方案

**场景**: 简单的发布订阅，不需要 Redis Pub/Sub 的复杂特性

**实现方式**:
- **订阅者列表**: `subscribers:{channel}`
- **消息队列**: `messages:{channel}`
- **发布**: `LPUSH messages:{channel} {message}`
- **订阅**: 轮询 `BRPOP messages:{channel} 0`

**优势**:
- 消息持久化（Redis Pub/Sub 不持久化）
- 支持消息回溯
- 更灵活的控制

---

## 二、Set（集合）业界常见做法

### 2.1 社交关系（Social Relations）

#### 2.1.1 关注关系（Following/Followers）

**场景**: 微博、Twitter、Instagram 等社交平台

**实现方式**:
- **关注列表**: `user:follow:{userId}` - Set 存储关注用户ID
- **粉丝列表**: `user:follower:{userId}` - Set 存储粉丝用户ID
- **互相关注**: `SINTER user:follow:{userId1} user:follow:{userId2}`

**高级功能**:
- **共同关注**: `SINTER user:follow:{userId1} user:follow:{userId2}`
- **推荐关注**: `SDIFF user:follow:{targetUserId} user:follow:{userId}` 找出目标用户关注但当前用户未关注的
- **粉丝交集**: `SINTER user:follower:{userId1} user:follower:{userId2}` 找出共同粉丝

**优化**:
- 大集合使用 `SSCAN` 分页获取
- 使用 `SISMEMBER` 快速判断关系
- 定期同步到数据库

#### 2.1.2 好友关系（Friends）

**场景**: 微信、QQ 等即时通讯应用

**实现方式**:
- **好友列表**: `user:friend:{userId}` - Set 存储好友ID
- **双向关系**: 添加好友时同时更新双方的 Set
- **好友推荐**: `SDIFF user:friend:{userId1} user:friend:{userId2}` 找出共同好友

**特点**:
- 双向关系必须一致
- 支持好友分组（使用多个 Set）
- 支持好友验证（临时 Set 存储待验证请求）

### 2.2 去重统计（Deduplication）

#### 2.2.1 独立访客统计（UV）

**场景**: 网站访问统计、广告点击统计

**实现方式**:
- **日UV**: `uv:date:{date}` - Set 存储用户ID或IP
- **月UV**: `uv:month:{month}` - Set 存储用户ID
- **统计**: `SCARD uv:date:{date}` 获取独立访客数

**优化**:
- 使用 HyperLogLog（PFADD）节省内存（允许误差）
- 定期合并到数据库
- 使用 `SUNIONSTORE` 合并多天数据

#### 2.2.2 点赞去重

**场景**: 防止重复点赞、投票去重

**实现方式**:
- **点赞记录**: `post:like:{postId}` - Set 存储用户ID
- **检查**: `SISMEMBER post:like:{postId} {userId}` 判断是否已点赞
- **统计**: `SCARD post:like:{postId}` 获取点赞数

**优势**:
- 天然去重
- 快速判断
- 支持批量检查

### 2.3 标签系统（Tagging）

**场景**: 文章标签、商品标签、用户兴趣标签

**实现方式**:
- **文章标签**: `post:tag:{postId}` - Set 存储标签ID
- **标签文章**: `tag:post:{tagId}` - Set 存储文章ID
- **用户兴趣**: `user:interest:{userId}` - Set 存储标签ID

**高级功能**:
- **标签推荐**: `SINTER user:interest:{userId1} user:interest:{userId2}` 找出共同兴趣
- **相关文章**: `SINTER tag:post:{tag1} tag:post:{tag2}` 找出同时包含多个标签的文章
- **标签合并**: `SUNION tag:post:{tag1} tag:post:{tag2}` 合并标签文章

### 2.4 随机推荐（Random Recommendation）

**场景**: 随机抽奖、随机推荐、随机匹配

**实现方式**:
- **候选集合**: `candidate:pool` - Set 存储候选ID
- **随机获取**: `SRANDMEMBER candidate:pool` 随机获取一个（不删除）
- **随机弹出**: `SPOP candidate:pool` 随机获取并删除

**应用场景**:
- **抽奖系统**: `SPOP lottery:pool` 随机抽取中奖者
- **随机推荐**: `SRANDMEMBER user:recommend:{userId} 10` 随机推荐10个用户
- **随机匹配**: 从匹配池中随机选择

### 2.5 黑白名单（Blacklist/Whitelist）

**场景**: 内容过滤、权限控制、风控系统

**实现方式**:
- **黑名单**: `blacklist:{type}` - Set 存储被拉黑ID
- **白名单**: `whitelist:{type}` - Set 存储白名单ID
- **检查**: `SISMEMBER blacklist:{type} {id}` 判断是否在黑名单

**高级功能**:
- **批量检查**: `SMISMEMBER blacklist:{type} {id1} {id2} ...` 批量判断
- **名单合并**: `SUNION blacklist:ip blacklist:user` 合并多个黑名单
- **名单过滤**: `SDIFF candidate:list blacklist:user` 过滤黑名单

### 2.6 集合运算（Set Operations）

**场景**: 数据分析、用户画像、推荐系统

**常用操作**:
- **交集（SINTER）**: 找出共同元素
  - 共同关注、共同好友、共同兴趣
- **并集（SUNION）**: 合并多个集合
  - 合并标签、合并权限
- **差集（SDIFF）**: 找出差异元素
  - 推荐关注、推荐商品
- **存储结果（SINTERSTORE/SUNIONSTORE/SDIFFSTORE）**: 将结果存储到新 Set

**应用场景**:
- **用户画像**: `SINTER user:tag:{userId1} user:tag:{userId2}` 找出相似用户
- **内容推荐**: `SDIFF tag:post:{userInterest} user:read:{userId}` 推荐未读内容
- **权限控制**: `SINTER user:role:{userId} role:permission:{resource}` 判断权限

---

## 三、SortedSet（有序集合）业界常见做法

### 3.1 排行榜（Leaderboard）

#### 3.1.1 实时排行榜

**场景**: 游戏积分榜、竞赛排名、销售排行

**实现方式**:
- **排行榜**: `leaderboard:{type}` - SortedSet，分数为积分
- **更新**: `ZINCRBY leaderboard:{type} {score} {userId}` 增加积分
- **查询**: `ZREVRANGE leaderboard:{type} 0 9` 获取前10名
- **排名**: `ZREVRANK leaderboard:{type} {userId}` 获取用户排名

**优化**:
- 定期清理低分用户（`ZREMRANGEBYRANK`）
- 使用多个时间段排行榜（日/周/月）
- 支持多维度排序（多个 SortedSet）

#### 3.1.2 分段排行榜

**场景**: 青铜/白银/黄金等分段系统

**实现方式**:
- **总榜**: `leaderboard:all`
- **分段榜**: `leaderboard:bronze`, `leaderboard:silver`, `leaderboard:gold`
- **分段规则**: 根据分数范围维护多个 SortedSet

**优势**:
- 支持分段查询
- 减少单榜数据量
- 支持分段奖励

### 3.2 延迟队列（Delay Queue）

**场景**: 定时任务、延迟消息、订单超时处理

**实现方式**:
- **延迟队列**: `delay:queue` - SortedSet，分数为执行时间戳
- **添加任务**: `ZADD delay:queue {executeTime} {taskId}`
- **获取任务**: `ZRANGEBYSCORE delay:queue 0 {currentTime} LIMIT 0 1` 获取到期任务
- **执行任务**: 执行后 `ZREM delay:queue {taskId}`

**流程**:
1. 定时扫描（每秒或每几秒）
2. `ZRANGEBYSCORE delay:queue 0 {now}` 获取到期任务
3. 执行任务
4. 从队列删除

**优势**:
- 精确控制执行时间
- 支持大量延迟任务
- 天然排序，优先处理

**应用场景**:
- 订单15分钟未支付自动取消
- 优惠券过期提醒
- 定时推送消息
- 定时数据同步

### 3.3 时间线（Timeline）

#### 3.3.1 用户时间线

**场景**: 微博、Twitter、朋友圈

**实现方式**:
- **用户时间线**: `timeline:user:{userId}` - SortedSet，分数为时间戳
- **发布**: `ZADD timeline:user:{userId} {timestamp} {postId}`
- **查询**: `ZREVRANGE timeline:user:{userId} 0 19` 获取最新20条
- **限制大小**: 使用 Lua 脚本限制时间线长度

**优化**:
- 定期清理旧数据（`ZREMRANGEBYSCORE`）
- 使用分片（按时间分片）
- 支持拉取和推送混合模式

#### 3.3.2 聚合时间线

**场景**: 关注用户的时间线聚合

**实现方式**:
- **关注用户时间线**: `timeline:user:{userId}` 多个
- **聚合时间线**: `ZUNIONSTORE timeline:feed:{userId} {count} timeline:user:{id1} timeline:user:{id2} ... AGGREGATE MAX`
- **查询**: `ZREVRANGE timeline:feed:{userId} 0 19`

**优化**:
- 使用临时 key 避免并发冲突
- 支持增量更新
- 缓存聚合结果

### 3.4 时间窗口统计（Time Window Statistics）

**场景**: 最近N天的数据统计、滑动窗口统计

**实现方式**:
- **时间窗口**: `stats:window:{metric}` - SortedSet，分数为时间戳
- **添加数据**: `ZADD stats:window:{metric} {timestamp} {value}`
- **查询范围**: `ZRANGEBYSCORE stats:window:{metric} {startTime} {endTime}`
- **清理过期**: `ZREMRANGEBYSCORE stats:window:{metric} 0 {expireTime}`

**应用场景**:
- 最近7天访问统计
- 最近30天销售额
- 滑动窗口限流
- 实时数据监控

### 3.5 多维度排序（Multi-dimensional Sorting）

**场景**: 商品排序（价格+销量+评分）、内容排序（热度+时间）

**实现方式**:
- **多个 SortedSet**: 每个维度一个 SortedSet
- **综合排序**: 使用 `ZINTERSTORE` 或 `ZUNIONSTORE` 合并
- **权重设置**: `ZUNIONSTORE result 2 set1 set2 WEIGHTS 0.5 0.5 AGGREGATE SUM`

**示例**:
- **商品排序**: 价格（升序）+ 销量（降序）+ 评分（降序）
- **内容排序**: 热度（降序）+ 时间（降序）

**优势**:
- 支持复杂排序规则
- 灵活调整权重
- 支持实时更新

### 3.6 范围查询（Range Query）

**场景**: 价格区间查询、时间范围查询、分数段统计

**实现方式**:
- **范围查询**: `ZRANGEBYSCORE key {min} {max}`
- **倒序范围**: `ZREVRANGEBYSCORE key {max} {min}`
- **限制数量**: `ZRANGEBYSCORE key {min} {max} LIMIT {offset} {count}`

**应用场景**:
- 商品价格区间筛选
- 用户积分段统计
- 时间范围数据查询
- 地理位置范围查询（结合 GeoHash）

### 3.7 实时计数器（Real-time Counter）

**场景**: 实时访问量、实时点赞数、实时评论数

**实现方式**:
- **计数器**: `counter:{type}:{id}` - SortedSet，分数为计数值
- **增加**: `ZINCRBY counter:{type}:{id} 1 {itemId}`
- **查询**: `ZSCORE counter:{type}:{id} {itemId}` 获取计数值
- **排序**: `ZREVRANGE counter:{type}:{id} 0 9` 获取前10名

**优势**:
- 实时更新
- 自动排序
- 支持多维度统计

---

## 四、组合使用模式

### 4.1 List + Set 组合

**场景**: 消息队列 + 去重

**实现**:
- List 存储消息队列
- Set 存储已处理消息ID，防止重复处理

### 4.2 SortedSet + Set 组合

**场景**: 排行榜 + 关系数据

**实现**:
- SortedSet 存储排行榜
- Set 存储用户关系（关注、好友等）
- 使用 `ZINTERSTORE` 结合关系数据筛选排行榜

### 4.3 List + SortedSet 组合

**场景**: 延迟队列 + 任务队列

**实现**:
- SortedSet 存储延迟任务（分数为执行时间）
- List 存储就绪任务
- 定时将到期任务从 SortedSet 移到 List

---

## 五、性能优化建议

### 5.1 批量操作

- **List**: `LPUSH` 支持多个值，减少网络往返
- **Set**: `SADD`、`SREM` 支持多个值
- **SortedSet**: `ZADD` 支持多个成员

### 5.2 管道（Pipeline）

- 多个操作使用 Pipeline 减少网络往返
- 适合批量操作场景

### 5.3 分页查询

- **List**: `LRANGE` 支持分页
- **Set**: `SSCAN` 支持游标分页（大集合）
- **SortedSet**: `ZRANGE` 支持分页

### 5.4 内存优化

- **定期清理**: 使用 `LTRIM`、`ZREMRANGEBYSCORE` 清理过期数据
- **数据分片**: 大集合分片存储
- **压缩存储**: 使用更短的 key 和 value

### 5.5 异步处理

- 非关键操作异步执行
- 使用消息队列解耦
- 批量同步到数据库

---

## 六、总结

业界对 Redis List、Set、SortedSet 的使用非常丰富：

1. **List**: 主要用于消息队列、任务队列、最新动态列表
2. **Set**: 主要用于社交关系、去重统计、标签系统、随机推荐
3. **SortedSet**: 主要用于排行榜、延迟队列、时间线、时间窗口统计

相比当前项目的简单使用，业界做法更加**复杂和深入**，充分利用了 Redis 的高级特性，实现了更多业务场景。

建议项目参考这些做法，逐步引入更多复杂场景，提升项目的技术深度和业务价值。

