# 排行榜实现文档

## 1. 概述

排行榜功能使用 Redis Sorted Set 实现，支持按点赞数、收藏数、评论数等维度进行排序。所有统计数据实时更新，支持分页查询和排名查询。

## 2. 业界常见的排行榜实现方法

### 2.1 数据库排序法
**原理**：将统计数据存储在数据库中，查询时使用 `ORDER BY` 排序。

**特点**：
- ✅ 实现简单
- ✅ 数据持久化
- ❌ 查询性能差（需要排序）
- ❌ 实时性差（需要实时更新数据库）

**应用场景**：
- 数据量小的场景
- 对实时性要求不高的场景

### 2.2 Redis Sorted Set 排序法（本项目采用）
**原理**：使用 Redis Sorted Set，score 为统计值，自动排序。

**特点**：
- ✅ 查询性能高（O(log N)）
- ✅ 实时性好（内存操作）
- ✅ 支持分页查询
- ✅ 支持排名查询
- ⚠️ 需要定期持久化到数据库

**应用场景**：
- 需要实时排行榜的场景
- 数据量中等的场景（百万级以内）

### 2.3 定时计算法
**原理**：定时（如每小时）计算排行榜，将结果缓存。

**特点**：
- ✅ 数据库压力小
- ✅ 查询性能高（预计算）
- ❌ 实时性差（有延迟）
- ❌ 计算资源消耗大

**应用场景**：
- 对实时性要求不高的场景
- 数据量很大的场景

## 3. 本项目的实现方式

### 3.1 数据存储结构

#### Redis Sorted Set
- **点赞排行榜**：`post:ranking:likes`
- **收藏排行榜**：`post:ranking:favorites`
- **评论排行榜**：`post:ranking:comments`
- **Score**：统计值（点赞数、收藏数、评论数）
- **Member**：帖子ID（postId）

**示例**：
```
Key: post:ranking:likes
Sorted Set:
  - postId: 456, score: 1000 (1000个赞)
  - postId: 789, score: 800 (800个赞)
  - postId: 123, score: 500 (500个赞)
```

### 3.2 Redis 命令使用

| 命令 | 用途 | 示例 |
|------|------|------|
| `ZINCRBY` | 增加/减少统计值 | `ZINCRBY post:ranking:likes 1 456` |
| `ZREVRANGE` | 获取排行榜（按分数从高到低） | `ZREVRANGE post:ranking:likes 0 19` |
| `ZSCORE` | 获取统计值 | `ZSCORE post:ranking:likes 456` |
| `ZREVRANK` | 获取排名（0-based） | `ZREVRANK post:ranking:likes 456` |

### 3.3 核心功能实现

#### 3.3.1 更新统计数据
**功能**：当用户点赞、收藏、评论时，更新排行榜

**实现方式**：
- `onLike(postId)`：点赞时调用，`ZINCRBY` 增加1
- `onUnlike(postId)`：取消点赞时调用，`ZINCRBY` 减少1
- `onFavorite(postId)`：收藏时调用
- `onUnfavorite(postId)`：取消收藏时调用
- `onComment(postId)`：评论时调用
- `onUncomment(postId)`：删除评论时调用

**特点**：
- 实时更新，立即生效
- 使用 `ZINCRBY` 原子性操作，避免并发问题

#### 3.3.2 获取排行榜
**功能**：获取排行榜列表（分页）

**实现方式**：
- `getLikesRanking(page, pageSize)`：获取点赞排行榜
- `getFavoritesRanking(page, pageSize)`：获取收藏排行榜
- `getCommentsRanking(page, pageSize)`：获取评论排行榜

**查询逻辑**：
- 使用 `ZREVRANGE` 按分数从高到低获取
- 支持分页查询（通过 start 和 end 参数）

#### 3.3.3 获取统计值
**功能**：获取某个帖子的统计值

**实现方式**：
- `getLikeCount(postId)`：获取点赞数
- `getFavoriteCount(postId)`：获取收藏数
- `getCommentCount(postId)`：获取评论数

**查询逻辑**：
- 使用 `ZSCORE` 获取分数（统计值）
- 如果不存在，返回0

## 4. 使用场景

### 4.1 热门内容推荐
**场景**：根据点赞数推荐热门帖子

**实现**：
```
List<Long> hotPosts = rankingService.getLikesRanking(1, 20);
```

### 4.2 排行榜展示
**场景**：在首页展示点赞排行榜

**实现**：
```
// 获取前10名
List<Long> topPosts = rankingService.getLikesRanking(1, 10);
```

### 4.3 统计值展示
**场景**：在帖子详情页展示点赞数、收藏数、评论数

**实现**：
```
Long likeCount = rankingService.getLikeCount(postId);
Long favoriteCount = rankingService.getFavoriteCount(postId);
Long commentCount = rankingService.getCommentCount(postId);
```

## 5. 性能优化

### 5.1 实时更新
- 使用 `ZINCRBY` 原子性操作，避免并发问题
- 内存操作，性能高

### 5.2 分页查询
- 使用 `ZREVRANGE` 支持分页，避免一次性加载所有数据
- 查询性能 O(log N + M)，N 为总数，M 为返回数量

### 5.3 数据持久化
- 定期将排行榜数据持久化到数据库
- 系统启动时从数据库恢复数据

## 6. 优缺点分析

### 6.1 优点
1. **查询性能高**：Redis Sorted Set 查询性能 O(log N)
2. **实时性好**：统计数据实时更新，立即生效
3. **支持分页**：支持分页查询，避免一次性加载所有数据
4. **支持排名**：可以查询某个帖子的排名
5. **原子性操作**：使用 `ZINCRBY` 保证并发安全

### 6.2 缺点
1. **内存占用**：所有统计数据存储在 Redis 中，内存占用较大
2. **数据持久化**：需要定期持久化到数据库，否则数据可能丢失
3. **单维度排序**：每个排行榜只能按一个维度排序（点赞、收藏、评论分开）

## 7. 适用场景

### 7.1 适用场景
- ✅ 需要实时排行榜的场景
- ✅ 数据量中等的场景（百万级以内）
- ✅ 需要分页查询的场景
- ✅ 需要查询排名的场景

### 7.2 不适用场景
- ❌ 数据量极大的场景（亿级）：建议使用定时计算法
- ❌ 对实时性要求不高的场景：可以使用数据库排序法
- ❌ 需要多维度综合排序的场景：需要使用热度排行榜（HotRankingService）

## 8. 扩展功能

### 8.1 多时间段排行榜
- 日榜、周榜、月榜、总榜
- 使用不同的 Redis Key 存储不同时间段的排行榜

### 8.2 排行榜缓存
- 将排行榜结果缓存，减少 Redis 查询
- 定时刷新缓存

### 8.3 排行榜统计
- 统计排行榜变化趋势
- 统计热门内容类型分布


