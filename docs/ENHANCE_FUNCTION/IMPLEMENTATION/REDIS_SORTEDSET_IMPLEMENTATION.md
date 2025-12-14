# Redis SortedSet 数据结构实现总结

## 概述

本文档总结项目中 Redis SortedSet 数据结构的实现情况，包括常见功能和高级功能的应用场景。

**实现完成度**: 100% ✅

---

## 一、常见功能（基础功能）

### 1.1 基础操作

**实现位置**: `BoundUtil.java` (768-893行)

**已实现的 Redis 命令**:

| Redis命令 | BoundUtil方法 | 功能 | 时间复杂度 |
|-----------|---------------|------|------------|
| ZADD | `zAdd()` | 添加/更新成员分数 | O(log(N)) |
| ZINCRBY | `zIncrBy()` | 增量更新分数 | O(log(N)) |
| ZRANGE | `zRange()` | 正序范围查询 | O(log(N)+M) |
| ZREVRANGE | `zReverseRange()` | 倒序范围查询 | O(log(N)+M) |
| ZSCORE | `zScore()` | 获取成员分数 | O(1) |
| ZREM | `zRem()` | 删除成员 | O(M*log(N)) |
| ZCARD | `zCard()` | 获取集合大小 | O(1) |
| ZREVRANK | `zRevRank()` | 获取倒序排名 | O(log(N)) |
| ZRANGEBYSCORE | `zRangeByScore()` | 按分数范围查询（正序） | O(log(N)+M) |
| ZREVRANGEBYSCORE | `zRevRangeByScore()` | 按分数范围查询（倒序） | O(log(N)+M) |
| ZUNIONSTORE | `zUnionAndStore()` | 集合聚合（并集） | O(N*K)+O(M*log(M)) |

**特点**:
- ✅ 功能完整：已实现 Redis SortedSet 的主要命令
- ✅ 类型安全：支持泛型，自动类型转换
- ✅ 自动排序：按分数自动排序

---

## 二、高级功能

### 2.1 单项排行榜系统 ✅ 已实现

**服务**: `RankingService` / `RankingServiceImpl`

**功能**:
- ✅ `onLike/onUnlike()` - 更新点赞排行榜（ZINCRBY）
- ✅ `onFavorite/onUnfavorite()` - 更新收藏排行榜（ZINCRBY）
- ✅ `onComment/onUncomment()` - 更新评论排行榜（ZINCRBY）
- ✅ `getLikesRanking()` - 获取点赞数排行榜（ZREVRANGE，分页）
- ✅ `getFavoritesRanking()` - 获取收藏数排行榜（ZREVRANGE，分页）
- ✅ `getCommentsRanking()` - 获取评论数排行榜（ZREVRANGE，分页）
- ✅ `getLikeCount()` - 获取帖子点赞数（ZSCORE）
- ✅ `getFavoriteCount()` - 获取帖子收藏数（ZSCORE）
- ✅ `getCommentCount()` - 获取帖子评论数（ZSCORE）

**Redis 命令使用**:
- `ZINCRBY` - 增量更新分数（点赞/收藏/评论时）
- `ZREVRANGE` - 获取排行榜（按分数从高到低，支持分页）
- `ZSCORE` - 获取单个成员的分数（统计数据）

**实现位置**: `src/main/java/com/wait/service/impl/RankingServiceImpl.java`

**数据结构**:
- `post:ranking:likes` - 点赞数排行榜（Member: 帖子ID, Score: 点赞数）
- `post:ranking:favorites` - 收藏数排行榜（Member: 帖子ID, Score: 收藏数）
- `post:ranking:comments` - 评论数排行榜（Member: 帖子ID, Score: 评论数）

**特点**:
- ✅ 实时更新：操作时立即更新Redis，保证实时性
- ✅ 高性能：使用ZINCRBY原子性增量更新，避免并发问题
- ✅ 支持分页：使用ZREVRANGE实现高效分页查询
- ✅ 统计数据查询：通过ZSCORE快速获取单个帖子的统计数据

**技术亮点**:
1. **增量更新**: 使用 `ZINCRBY` 实现原子性增量更新，避免并发问题
2. **高效分页**: 使用 `ZREVRANGE` 实现高效分页查询，无需应用层排序
3. **实时统计**: 统计数据实时更新，保证准确性

**使用示例**:
```java
// 更新点赞排行榜
rankingService.onLike(postId);

// 获取点赞排行榜（分页）
List<Long> topPosts = rankingService.getLikesRanking(1, 20);

// 获取单个帖子点赞数
Long likeCount = rankingService.getLikeCount(postId);
```

---

### 2.2 综合热度排行榜 ✅ 已实现

**服务**: `HotRankingService` / `HotRankingServiceImpl`

**功能**:
- ✅ `updateHotScore()` - 更新热度分数（从Redis获取实时数据计算）
- ✅ `onLike/onUnlike()` - 点赞时更新热度
- ✅ `onFavorite/onUnfavorite()` - 收藏时更新热度
- ✅ `onComment()` - 评论时更新热度
- ✅ `getHotPosts()` - 获取热度排行榜（ZREVRANGE，支持多时间段）
- ✅ `getPostRank()` - 获取帖子排名（ZREVRANK）
- ✅ `getHotScore()` - 获取帖子热度分数（ZSCORE）

**Redis 命令使用**:
- `ZADD` - 更新热度分数（如果不存在则添加，存在则更新）
- `ZREVRANGE` - 获取热度排行榜（按分数从高到低）
- `ZREVRANK` - 获取排名（0-based，转换为1-based）
- `ZSCORE` - 获取热度分数

**实现位置**: `src/main/java/com/wait/service/impl/HotRankingServiceImpl.java`

**数据结构**:
- `post:ranking:hot:daily` - 日榜
- `post:ranking:hot:weekly` - 周榜
- `post:ranking:hot:monthly` - 月榜
- `post:ranking:hot:alltime` - 总榜

**热度算法**:
```
热度 = 点赞数 × 0.4 + 收藏数 × 0.3 + 评论数 × 0.2 + 分享数 × 0.1
```

**特点**:
- ✅ 多时间段支持：日榜、周榜、月榜、总榜
- ✅ 实时计算：从Redis获取实时统计数据，保证准确性
- ✅ 权重配置：可配置的权重算法，灵活调整热度计算
- ✅ 性能优化：避免数据库查询，所有数据从Redis获取

**技术亮点**:
1. **实时数据源**: 热度计算从Redis获取实时数据，避免数据库查询
2. **多时间段**: 支持日榜、周榜、月榜、总榜等多种时间段
3. **排名查询**: 使用 `ZREVRANK` 快速查询排名

**使用示例**:
```java
// 更新热度分数
hotRankingService.updateHotScore(postId);

// 获取日榜
List<Long> hotPosts = hotRankingService.getHotPosts("daily", 1, 20);

// 获取帖子排名
Long rank = hotRankingService.getPostRank(postId, "daily");
```

---

### 2.3 时间线聚合功能 ✅ 已实现

**服务**: `TimelineSortedSetService` / `TimelineSortedSetServiceImpl`

**功能**:
- ✅ `publishToTimeline()` - 发布帖子到时间线（Lua脚本原子操作）
- ✅ `getUserTimeline()` - 获取用户时间线（ZREVRANGE，分页）
- ✅ `getGlobalTimeline()` - 获取全局时间线（ZREVRANGE，分页）
- ✅ `getMyTimeline()` - 获取我的时间线（ZUNIONSTORE聚合）
- ✅ `getPostsByTimeRange()` - 按时间范围查询（ZRANGEBYSCORE）
- ✅ `removeFromTimeline()` - 从时间线移除帖子（Lua脚本原子操作）

**Redis 命令使用**:
- `ZADD` - 添加帖子到时间线（分数为时间戳）
- `ZREVRANGE` - 获取最新帖子（按时间戳倒序）
- `ZRANGEBYSCORE` - 按时间范围查询帖子
- `ZUNIONSTORE` - 聚合多个用户的时间线（MAX聚合，取最新时间戳）
- `ZREMRANGEBYRANK` - 限制时间线大小（Lua脚本中）

**实现位置**: `src/main/java/com/wait/service/impl/TimelineSortedSetServiceImpl.java`

**数据结构**:
- `timeline:posts:user:{userId}` - 用户时间线（Member: 帖子ID, Score: 发布时间戳）
- `timeline:posts:global` - 全局时间线
- `timeline:posts:my:{userId}:{timestamp}` - 我的时间线临时key（聚合结果）

**特点**:
- ✅ 原子性操作：使用Lua脚本保证操作的原子性
- ✅ 自动限制大小：最多缓存1000条，自动删除旧数据
- ✅ 时间线聚合：使用ZUNIONSTORE聚合多个用户的时间线
- ✅ 临时key管理：使用时间戳确保唯一性，finally块确保清理
- ✅ 黑名单过滤：支持过滤黑名单用户和拉黑关系

**技术亮点**:
1. **集合聚合**: 使用 `ZUNIONSTORE` 聚合多个时间线，服务器端操作性能高
2. **原子性保证**: 使用Lua脚本保证操作的原子性
3. **临时key策略**: 使用时间戳确保唯一性，finally块确保清理
4. **范围查询**: 使用 `ZRANGEBYSCORE` 实现时间范围查询

**使用示例**:
```java
// 发布帖子到时间线
timelineSortedSetService.publishToTimeline(userId, postId, System.currentTimeMillis());

// 获取用户时间线
List<Long> userTimeline = timelineSortedSetService.getUserTimeline(userId, 1, 20);

// 获取我的时间线（聚合关注用户的时间线）
List<Long> myTimeline = timelineSortedSetService.getMyTimeline(userId, 1, 20);

// 按时间范围查询
long startTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
long endTime = System.currentTimeMillis();
List<Long> posts = timelineSortedSetService.getPostsByTimeRange(userId, startTime, endTime);
```

---

### 2.4 精确延迟队列 ✅ 已实现

**服务**: `DelayQueueService` / `DelayQueueServiceImpl`

**功能**:
- ✅ `addTask()` - 添加延迟任务（ZADD，分数为执行时间戳）
- ✅ `addTaskWithDelay()` - 添加延迟任务（延迟N秒后执行）
- ✅ `getReadyTasks()` - 获取到期任务（ZRANGEBYSCORE，不删除）
- ✅ `pollReadyTasks()` - 获取并删除到期任务
- ✅ `removeTask()` - 删除任务（ZREM）
- ✅ `getTaskExecuteTime()` - 获取任务执行时间（ZSCORE）
- ✅ `getQueueSize()` - 获取队列长度（ZCARD）
- ✅ `clearQueue()` - 清空队列（DEL）

**Redis 命令使用**:
- `ZADD` - 添加延迟任务（分数为执行时间戳）
- `ZRANGEBYSCORE` - 获取到期任务（分数 <= 当前时间）
- `ZREM` - 删除任务
- `ZCARD` - 获取队列长度
- `ZSCORE` - 获取任务执行时间

**实现位置**: `src/main/java/com/wait/service/impl/DelayQueueServiceImpl.java`

**数据结构**:
- `delay:queue:{queueName}` - 延迟任务队列（Member: 任务ID, Score: 执行时间戳）

**特点**:
- ✅ 精确控制执行时间（毫秒级）
- ✅ 支持大量延迟任务
- ✅ 天然排序，优先处理到期任务
- ✅ 支持任务查询和取消

**技术亮点**:
1. **精确延迟**: 使用时间戳作为分数，实现毫秒级精确延迟
2. **范围查询**: 使用 `ZRANGEBYSCORE` 高效查询到期任务
3. **自动排序**: SortedSet 自动按分数排序，优先处理到期任务

**应用场景**:
- 订单15分钟未支付自动取消
- 优惠券过期提醒
- 定时推送消息
- 定时数据同步

**使用示例**:
```java
// 添加延迟任务（延迟15分钟）
delayQueueService.addTaskWithDelay("order", orderId, 15 * 60);

// 获取到期任务
List<String> readyTasks = delayQueueService.getReadyTasks("order", 100);

// 获取并删除到期任务
List<String> tasks = delayQueueService.pollReadyTasks("order", 100);
```

---

### 2.5 时间窗口统计 ✅ 已实现

**服务**: `TimeWindowStatisticsService` / `TimeWindowStatisticsServiceImpl`

**功能**:
- ✅ `addDataPoint()` - 添加数据点（ZADD，分数为时间戳）
- ✅ `getDataPoints()` - 查询时间范围内的数据点（ZRANGEBYSCORE）
- ✅ `getRecentDataPoints()` - 获取最近N天的数据点
- ✅ `getRecentDataPointsByHours()` - 获取最近N小时的数据点
- ✅ `countDataPoints()` - 统计时间范围内的数据点数量
- ✅ `cleanExpiredData()` - 清理过期数据（ZREMRANGEBYSCORE）
- ✅ `getTotalCount()` - 获取数据点总数（ZCARD）
- ✅ `calculateStatistics()` - 计算统计值（求和、平均、最大、最小）

**Redis 命令使用**:
- `ZADD` - 添加数据点（分数为时间戳）
- `ZRANGEBYSCORE` - 查询时间范围内的数据
- `ZREMRANGEBYSCORE` - 清理过期数据
- `ZCARD` - 获取数据点总数

**实现位置**: `src/main/java/com/wait/service/impl/TimeWindowStatisticsServiceImpl.java`

**数据结构**:
- `stats:window:{metric}` - 时间窗口数据（Member: 数据值, Score: 时间戳）

**特点**:
- ✅ 精确的时间窗口控制
- ✅ 自动清理过期数据
- ✅ 支持复杂统计计算
- ✅ 支持多时间粒度（小时、天）

**技术亮点**:
1. **范围查询**: 使用 `ZRANGEBYSCORE` 高效查询时间范围内的数据
2. **自动清理**: 支持清理过期数据，防止内存占用过大
3. **统计计算**: 支持求和、平均、最大、最小等统计计算

**应用场景**:
- 最近7天访问统计
- 最近30天销售额统计
- 最近1小时请求数统计
- 实时数据监控

**使用示例**:
```java
// 添加数据点
timeWindowStatisticsService.addDataPoint("sales", "100.5");

// 获取最近7天的数据
List<String> dataPoints = timeWindowStatisticsService.getRecentDataPoints("sales", 7);

// 计算统计值
Map<String, Double> stats = timeWindowStatisticsService.calculateStatistics(
    "sales", startTime, endTime);
```

---

### 2.6 多维度排序 ✅ 已实现

**服务**: `MultiDimensionSortService` / `MultiDimensionSortServiceImpl`

**功能**:
- ✅ `addDimensionData()` - 添加单维度数据（ZADD）
- ✅ `addDimensionDataBatch()` - 批量添加单维度数据
- ✅ `updateDimensionData()` - 更新单维度数据
- ✅ `incrementDimensionScore()` - 增加单维度分数（ZINCRBY）
- ✅ `compositeSort()` - 综合排序（ZUNIONSTORE，支持权重和聚合类型）
- ✅ `getSortedResult()` - 获取综合排序结果（ZREVRANGE）
- ✅ `getItemRank()` - 获取项目排名（ZREVRANK）
- ✅ `getItemScore()` - 获取项目分数（ZSCORE）
- ✅ `getDimensionSortResult()` - 获取单维度排序结果
- ✅ `deleteCompositeResult()` - 删除综合排序结果

**Redis 命令使用**:
- `ZADD` - 添加单维度数据
- `ZINCRBY` - 增加单维度分数
- `ZUNIONSTORE` - 多维度合并（加权）
- `ZREVRANGE` - 获取排序结果
- `ZREVRANK` - 获取排名
- `ZSCORE` - 获取分数

**实现位置**: `src/main/java/com/wait/service/impl/MultiDimensionSortServiceImpl.java`

**数据结构**:
- `sort:dimension:{dimension}` - 单维度排序数据
- `sort:result:{resultKey}` - 综合排序结果

**特点**:
- ✅ 支持多维度综合排序
- ✅ 支持权重设置（SUM、MAX、MIN聚合）
- ✅ 支持实时更新
- ✅ 灵活调整排序规则

**技术亮点**:
1. **集合聚合**: 使用 `ZUNIONSTORE` 实现多维度综合排序
2. **权重支持**: 支持不同维度的权重设置
3. **多种聚合**: 支持SUM、MAX、MIN等多种聚合方式

**应用场景**:
- 商品排序（价格+销量+评分）
- 内容排序（热度+时间+质量）
- 用户排序（活跃度+贡献度+影响力）

**使用示例**:
```java
// 添加单维度数据
multiDimensionSortService.addDimensionData("price", itemId, 100.0);
multiDimensionSortService.addDimensionData("sales", itemId, 50.0);

// 综合排序
List<String> dimensions = Arrays.asList("price", "sales");
List<Double> weights = Arrays.asList(0.3, 0.7);
multiDimensionSortService.compositeSort("product", dimensions, weights, "SUM");

// 获取排序结果
List<String> sortedItems = multiDimensionSortService.getSortedResult("product", 0, 19);
```

---

### 2.7 统计数据同步服务 ✅ 已实现

**服务**: `LikeCountSyncService`, `FavoriteCountSyncService`, `CommentCountSyncService`

**功能**:
- ✅ `syncLikeCountsFromDatabase()` - 同步点赞数（定时任务，每天凌晨2点）
- ✅ `syncFavoriteCountsFromDatabase()` - 同步收藏数（定时任务，每天凌晨2点）
- ✅ `syncCommentCountsFromDatabase()` - 同步评论数（定时任务，每天凌晨2点）
- ✅ `manualSync()` - 手动触发同步（用于测试或紧急修复）

**Redis 命令使用**:
- `ZADD` - 批量更新统计数据到Redis Sorted Set

**实现位置**:
- `src/main/java/com/wait/service/impl/LikeCountSyncService.java`
- `src/main/java/com/wait/service/impl/FavoriteCountSyncService.java`
- `src/main/java/com/wait/service/impl/CommentCountSyncService.java`

**数据同步策略**:
- **Write-Behind模式**：日常操作只更新Redis，定时任务从数据库同步
- **同步频率**：每天凌晨2点执行（Cron: `0 0 2 * * ?`）
- **数据源**：从数据库表统计（GROUP BY post_id）
- **批量更新**：批量查询后批量更新Redis，提高效率

**特点**:
- ✅ 保证最终一致性：定时任务确保Redis和数据库最终一致
- ✅ 减少数据库压力：统计数据不实时写入数据库
- ✅ 实时性保证：用户看到的统计数据是实时的（从Redis获取）
- ✅ 批量同步：从数据库GROUP BY统计后批量更新Redis，提高效率

---

## 三、实现总结

### 3.1 功能实现情况

| 功能 | 状态 | 服务类 | 说明 |
|------|------|--------|------|
| 单项排行榜 | ✅ 已实现 | RankingService | 点赞/收藏/评论排行榜 |
| 综合热度排行榜 | ✅ 已实现 | HotRankingService | 多时间段热度排行榜 |
| 时间线聚合 | ✅ 已实现 | TimelineSortedSetService | 用户/全局/聚合时间线 |
| 精确延迟队列 | ✅ 已实现 | DelayQueueService | 毫秒级精确延迟任务队列 |
| 时间窗口统计 | ✅ 已实现 | TimeWindowStatisticsService | 滑动窗口数据统计 |
| 多维度排序 | ✅ 已实现 | MultiDimensionSortService | 多维度综合排序 |
| 统计数据同步 | ✅ 已实现 | *CountSyncService | 定时同步统计数据 |

**实现完成度**: 100% ✅

---

### 3.2 技术亮点

1. **增量更新**: 使用 `ZINCRBY` 实现原子性增量更新，避免并发问题
2. **高效分页**: 使用 `ZREVRANGE` 实现高效分页查询，无需应用层排序
3. **集合聚合**: 使用 `ZUNIONSTORE` 聚合多个时间线，服务器端操作性能高
4. **范围查询**: 使用 `ZRANGEBYSCORE` 实现时间范围查询，适合时间线场景
5. **排名查询**: 使用 `ZREVRANK` 快速查询排名，适合展示场景
6. **实时数据源**: 热度计算从Redis获取实时数据，避免数据库查询
7. **Write-Behind模式**: 统计数据采用Write-Behind模式，保证性能和一致性
8. **Lua脚本原子性**: 时间线操作使用Lua脚本，保证原子性
9. **临时key管理**: 使用时间戳确保唯一性，finally块确保清理
10. **精确延迟**: 使用时间戳作为分数，实现毫秒级精确延迟

---

### 3.3 使用场景

1. **排行榜系统**: 单项排行榜和综合热度排行榜
2. **时间线系统**: 用户时间线、全局时间线、聚合时间线
3. **延迟队列**: 精确延迟任务队列
4. **时间窗口统计**: 滑动窗口数据统计
5. **多维度排序**: 多维度综合排序
6. **统计数据管理**: 实时统计和定时同步

---

### 3.4 性能优化建议

1. **批量操作**: 使用 Pipeline 优化批量操作
2. **大集合遍历**: 大集合使用 ZSCAN 游标遍历，避免阻塞
3. **定期清理**: 定期清理过期数据，防止内存占用过大
4. **监控告警**: 监控排行榜大小、延迟队列长度等，及时告警

---

## 四、相关文档

- [Redis List、Set、SortedSet 实现汇总](./REDIS_LIST_SET_SORTEDSET_IMPLEMENTATION_SUMMARY.md) ⭐ **推荐阅读**
- [Redis List、Set、SortedSet 当前用法总结](./REDIS_LIST_SET_SORTEDSET_CURRENT_USAGE.md)
- [Redis List、Set、SortedSet 实现总结](./REDIS_LIST_SET_IMPLEMENTATION_SUMMARY.md)
- [Redis List 实现总结](./REDIS_LIST_IMPLEMENTATION.md)
- [Redis Set 实现总结](./REDIS_SET_IMPLEMENTATION.md)

