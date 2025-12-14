# 统计数据同步策略 - 业界常见做法对比

## 问题背景

在社交平台和内容社区中，统计数据（点赞数、收藏数、评论数）是高频更新的数据。每次用户操作时，都需要更新这些统计数据。如何高效、准确地维护这些统计数据，业界有多种做法。

本文档涵盖三种统计数据的同步策略：
- **点赞数（Like Count）**
- **收藏数（Favorite Count）**
- **评论数（Comment Count）**



## 方案对比

### 方案一：实时同步（Write-Through）

#### 实现方式
每次评论操作时，同时更新Redis和数据库，确保两者实时一致。

```java
// 示例代码
public void incrementCommentCount(Long postId) {
    // 1. 更新数据库
    postMapper.incrementCommentCount(postId);
    // 2. 更新Redis
    rankingService.onComment(postId);
}
```

#### 优点
1. **数据一致性高**：Redis和数据库实时同步，数据一致性强
2. **实现简单**：逻辑清晰，易于理解和维护
3. **故障恢复容易**：数据库是数据源，Redis故障不影响数据完整性

#### 缺点
1. **性能问题**：
   - 每次操作都要写数据库，数据库压力大
   - 写操作延迟高（数据库I/O较慢）
   - 高并发场景下成为瓶颈
2. **扩展性差**：数据库写入性能有限，难以支持大规模高并发

#### 适用场景
- 数据更新频率低
- 对一致性要求极高
- 并发量较小
- 数据量不大



### 方案二：异步同步（Write-Behind，推荐方案）

#### 实现方式
操作时只更新Redis，数据库通过异步任务或定时任务同步。

**点赞数同步**：
```java
// 点赞操作：只更新Redis
public void likePost(Long userId, Long postId) {
    rankingService.onLike(postId);  // 只更新Redis
    // 关系数据通过RelationPersistenceService异步批量写入数据库
}

// 定时任务：每天凌晨2点同步点赞数
@Scheduled(cron = "0 0 2 * * ?")
public void syncLikeCountsFromDatabase() {
    List<PostLike> counts = postLikeMapper.selectAllPostLikeCounts();
    for (PostLike like : counts) {
        boundUtil.zAdd(RANKING_LIKES, like.getPostId(), like.getId().doubleValue());
    }
}
```

**收藏数同步**：
```java
// 收藏操作：只更新Redis
public void favoritePost(Long userId, Long postId) {
    rankingService.onFavorite(postId);  // 只更新Redis
    // 关系数据通过RelationPersistenceService异步批量写入数据库
}

// 定时任务：每天凌晨2点同步收藏数
@Scheduled(cron = "0 0 2 * * ?")
public void syncFavoriteCountsFromDatabase() {
    List<PostFavorite> counts = postFavoriteMapper.selectAllPostFavoriteCounts();
    for (PostFavorite fav : counts) {
        boundUtil.zAdd(RANKING_FAVORITES, fav.getPostId(), fav.getId().doubleValue());
    }
}
```

**评论数同步**：
```java
// 评论操作：只更新Redis
public void incrementCommentCount(Long postId) {
    rankingService.onComment(postId);  // 只更新Redis
    // 数据库通过定时任务同步（每天凌晨2点）
}

@Scheduled(cron = "0 0 2 * * ?")
public void syncCommentCountsFromDatabase() {
    List<Comment> counts = commentMapper.selectAllPostCommentCounts();
    for (Comment c : counts) {
        boundUtil.zAdd(RANKING_COMMENTS, c.getPostId(), c.getLikeCount().doubleValue());
    }
}
```

#### 优点
1. **性能优异**：
   - 写操作只更新Redis，延迟极低（内存操作）
   - 大幅降低数据库压力
   - 支持高并发场景
2. **实时性强**：
   - Redis中的数据实时更新，用户看到的是最新数据
   - 排行榜和热度计算基于实时数据
3. **扩展性好**：
   - Redis支持水平扩展
   - 可以轻松应对大规模高并发

#### 缺点
1. **数据最终一致性**：
   - Redis和数据库可能存在短暂不一致（但在可接受范围内）
   - 需要定时任务保证最终一致性
2. **故障恢复**：
   - 如果Redis数据丢失，需要从数据库恢复
   - 需要实现数据恢复机制

#### 适用场景
- **高并发场景**：评论操作频繁
- **实时性要求高**：需要实时反映数据变化
- **大规模数据**：需要支持大量帖子和用户
- **缓存优先架构**：Redis作为主要数据源



### 方案三：混合方案（Redis优先，数据库降级）

#### 实现方式
优先从Redis获取数据，如果Redis中没有或出错，则从数据库查询并回写Redis。

```java
// 示例代码
public Long getCommentCount(Long postId) {
    // 1. 优先从Redis获取
    Long count = rankingService.getCommentCount(postId);
    if (count != null && count > 0) {
        return count;
    }
    
    // 2. Redis中没有，从数据库查询
    int dbCount = commentMapper.countByPostId(postId);
    // 3. 回写Redis
    rankingService.setCommentCount(postId, dbCount);
    return (long) dbCount;
}
```

#### 优点
1. **高可用性**：
   - Redis不可用时可以降级到数据库
   - 保证服务的可用性
2. **数据完整性**：
   - 即使Redis数据丢失，也能从数据库恢复
   - 适用于冷数据场景

#### 缺点
1. **实现复杂**：
   - 需要处理降级逻辑
   - 需要维护两套数据源
2. **性能不稳定**：
   - 降级时会增加数据库压力
   - 响应时间可能波动较大

#### 适用场景
- 对可用性要求极高的场景
- 需要处理冷数据
- Redis可能不稳定或数据可能丢失的场景



### 方案四：消息队列异步更新

#### 实现方式
评论操作时发送消息到MQ，消费者异步更新数据库。

```java
// 示例代码
public void incrementCommentCount(Long postId) {
    // 1. 立即更新Redis
    rankingService.onComment(postId);
    // 2. 发送消息到MQ
    mqService.sendMessage("comment_count_increment", postId);
}

// 消费者
@RabbitListener(queues = "comment_count_queue")
public void handleCommentCountUpdate(Long postId) {
    // 异步更新数据库
    postMapper.incrementCommentCount(postId);
}
```

#### 优点
1. **解耦**：
   - 写操作和数据库更新解耦
   - 系统架构更清晰
2. **可靠性**：
   - 消息队列保证消息不丢失
   - 支持重试机制
3. **扩展性**：
   - 可以水平扩展消费者
   - 支持批量处理

#### 缺点
1. **复杂度高**：
   - 需要引入消息队列中间件
   - 需要处理消息丢失、重复等问题
2. **延迟**：
   - 数据库更新有延迟（取决于消息处理速度）
   - 不适合对实时性要求极高的场景

#### 适用场景
- 需要解耦的系统架构
- 对可靠性要求高
- 需要支持批量处理
- 有消息队列基础设施



## 定时同步策略

### 同步频率选择

#### 1. 实时同步（每次操作后立即同步）
- **优点**：数据一致性最高
- **缺点**：数据库压力大，性能差
- **适用**：对一致性要求极高的场景

#### 2. 定时同步（推荐）
- **频率选择**：
  - **每小时**：适合数据更新频繁的场景
  - **每天凌晨**：适合数据更新不频繁的场景（**当前实现**）
  - **每周**：适合数据更新很少的场景
- **优点**：数据库压力小，性能好
- **缺点**：数据有延迟（但在可接受范围内）

#### 3. 批量同步（达到阈值后同步）
- **策略**：累计一定数量的更新后，批量同步
- **优点**：平衡性能和一致性
- **缺点**：实现复杂，需要维护计数器



## 数据一致性保证

### 最终一致性模型

在Write-Behind模式下，我们采用**最终一致性模型**：

1. **日常操作**：只更新Redis，保证实时性
2. **定时同步**：从数据库同步到Redis，保证最终一致性
3. **数据校验**：定期校验Redis和数据库的一致性

### 一致性窗口

- **时间窗口**：最多24小时（定时任务执行间隔）
- **影响范围**：只影响数据库中的评论数，不影响用户看到的实时数据
- **可接受性**：对于评论数这种统计数据，最终一致性是可接受的



## 业界案例

### 1. 微博/推特
- **策略**：Write-Behind + 定时同步
- **原因**：高并发、实时性要求高
- **实现**：评论操作只更新Redis，每小时同步一次数据库

### 2. 知乎
- **策略**：Write-Behind + 消息队列
- **原因**：需要解耦，支持批量处理
- **实现**：评论操作更新Redis并发送消息，消费者批量更新数据库

### 3. Reddit
- **策略**：Write-Behind + 定时同步（每天）
- **原因**：数据量大，对实时性要求不是特别高
- **实现**：评论操作只更新Redis，每天凌晨同步数据库



## 最佳实践建议

### 1. 推荐方案：Write-Behind + 定时同步

对于大多数社交平台和内容社区，推荐使用**Write-Behind模式 + 定时同步**。

**原因**：
- 高性能、低延迟
- 实时性强
- 数据库压力小
- 实现相对简单

### 2. 实现要点

#### 数据结构
```java
// Redis Sorted Set存储评论数排行榜
post:ranking:comments
- Member: 帖子ID
- Score: 评论数
```

#### 更新时机
- **增加评论**：立即更新Redis（`rankingService.onComment(postId)`）
- **删除评论**：立即更新Redis（`rankingService.onUncomment(postId)`）
- **定时同步**：每天凌晨2点从数据库同步到Redis

#### 数据同步
- 从数据库查询所有有评论的帖子ID和评论数
- 批量更新到Redis Sorted Set
- 记录同步日志，便于排查问题

### 3. 优化建议

#### 批量处理
```java
// 批量更新Redis，减少网络往返
for (Map.Entry<Long, Integer> entry : commentCountMap.entrySet()) {
    boundUtil.zAdd(RANKING_COMMENTS, entry.getKey(), entry.getValue().doubleValue());
}
```

#### 增量同步
如果数据量很大，可以考虑增量同步：
- 只同步最近有变化的帖子
- 使用变更日志（Change Log）跟踪变化

#### 数据校验
定期校验Redis和数据库的一致性：
- 随机抽样检查
- 发现不一致时自动修复



## 总结

| 方案 | 性能 | 实时性 | 一致性 | 复杂度 | 推荐度 |
|------|------|--------|--------|--------|--------|
| 实时同步 | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ❌ |
| **Write-Behind + 定时同步** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ✅✅✅ |
| 混合方案 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ | ✅ |
| 消息队列 | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ | ✅✅ |

**最终建议**：对于点赞数、收藏数、评论数这种高频更新的统计数据，**优先使用Write-Behind模式 + 定时同步**。这样既能保证实时性和性能，又能通过定时任务保证最终一致性，是性能和一致性的最佳平衡。



## 当前实现总结

### 已实现的同步服务

1. **LikeCountSyncService**：点赞数同步服务
   - 定时任务：每天凌晨2点执行（Cron: `0 0 2 * * ?`）
   - 数据源：`post_like`表（GROUP BY post_id统计）
   - 目标：`post:ranking:likes` Sorted Set
   - 实现：`PostLikeMapper.selectAllPostLikeCounts()`

2. **FavoriteCountSyncService**：收藏数同步服务
   - 定时任务：每天凌晨2点执行（Cron: `0 0 2 * * ?`）
   - 数据源：`post_favorite`表（GROUP BY post_id统计）
   - 目标：`post:ranking:favorites` Sorted Set
   - 实现：`PostFavoriteMapper.selectAllPostFavoriteCounts()`

3. **CommentCountSyncService**：评论数同步服务
   - 定时任务：每天凌晨2点执行（Cron: `0 0 2 * * ?`）
   - 数据源：`post_comment`表（GROUP BY post_id统计，is_deleted=0）
   - 目标：`post:ranking:comments` Sorted Set
   - 实现：`CommentMapper.selectAllPostCommentCounts()`

### 数据流图

```
用户操作（点赞/收藏/评论）
    ↓
更新Redis Sorted Set（实时，通过RankingService）
    ↓
更新热度分数（实时，通过HotRankingService）
    ↓
关系数据异步批量写入数据库（RelationPersistenceService，30秒或5条触发）
    ↓
定时任务同步统计数据到Redis（每天凌晨2点，三个SyncService）
```

### 关键优势

- ✅ **用户看到的统计数据是实时的**：从Redis获取，延迟极低
- ✅ **数据库压力大幅降低**：统计数据定时批量同步，关系数据异步批量写入
- ✅ **支持高并发场景**：Redis内存操作，性能优异
- ✅ **数据最终一致性**：定时任务兜底，保证Redis和数据库最终一致

