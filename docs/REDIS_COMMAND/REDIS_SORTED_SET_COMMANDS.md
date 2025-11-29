# Sorted Set 命令列表

本文档基于 `BoundUtil` 中的 Sorted Set 相关方法，总结 Redis Sorted Set 类型的所有命令、使用频率和使用场景示例代码。

Redis Sorted Set（有序集合）是 Redis 中唯一同时支持**排序**和**去重**的数据结构，每个成员（member）都有一个分数（score），Redis 通过分数对成员进行排序。Sorted Set 非常适合实现排行榜、时间线排序、延迟队列等功能。



## 使用频率说明

- **⭐⭐⭐ 极高频率**：几乎每个 Sorted Set 场景都会使用
- **⭐⭐ 高频**：大多数场景会使用
- **⭐ 中频**：特定场景使用
- **无标记**：低频或特殊场景使用



## 1. 基础操作

### ZADD - 添加成员 ⭐⭐⭐

**Redis命令：** `ZADD key [NX|XX] [CH] [INCR] score member [score member ...]`

**方法签名：**
```java
boundUtil.zAdd(key, value, score);
```

**用途：** 向有序集合中添加一个或多个成员，或者更新已存在成员的分数。

**参数说明：**
- `key`: 有序集合的键名
- `value`: 成员值
- `score`: 分数（用于排序）

**返回值：** `true` 表示成功添加或更新，`false` 表示失败。

**行为说明：**
- 如果成员已存在，会更新其分数
- 如果成员不存在，会添加新成员
- 分数可以是整数或浮点数

**使用场景：**

1. **排行榜（游戏积分排行榜）**
```java
// 添加玩家积分
boundUtil.zAdd("game:leaderboard", userId, 1000.0);

// 更新玩家积分（如果已存在则更新，不存在则添加）
boundUtil.zAdd("game:leaderboard", userId, 1500.0);
```

2. **时间线排序（按发布时间排序）**
```java
// 帖子发布时间作为分数（时间戳）
long publishTime = System.currentTimeMillis();
boundUtil.zAdd("timeline:posts", postId, publishTime);
```

3. **点赞数排行榜**
```java
// 帖子ID作为成员，点赞数作为分数
boundUtil.zAdd("post:ranking:likes", postId, likeCount);
```

4. **延迟队列（到期时间作为分数）**
```java
// 订单到期时间作为分数
long expireTime = System.currentTimeMillis() + 30 * 60 * 1000; // 30分钟后
boundUtil.zAdd("order:delayed", orderId, expireTime);
```

5. **热门内容推荐（综合分数）**
```java
// 综合分数 = 点赞数 * 0.4 + 收藏数 * 0.3 + 评论数 * 0.2 + 分享数 * 0.1
double hotScore = likeCount * 0.4 + favoriteCount * 0.3 + 
                  commentCount * 0.2 + shareCount * 0.1;
boundUtil.zAdd("post:ranking:hot", postId, hotScore);
```



### ZINCRBY - 增加成员分数 ⭐⭐⭐

**Redis命令：** `ZINCRBY key increment member`

**方法签名：**
```java
boundUtil.zIncrBy(key, value, delta);
```

**用途：** 对有序集合中指定成员的分数增加指定的增量。

**参数说明：**
- `key`: 有序集合的键名
- `value`: 成员值
- `delta`: 增量（可以为负数，相当于减少分数）

**返回值：** 更新后的分数值。

**使用场景：**

1. **实时更新排行榜分数**
```java
// 玩家获得积分后，实时更新排行榜
Double newScore = boundUtil.zIncrBy("game:leaderboard", userId, 100.0);
```

2. **点赞/取消点赞时更新帖子热度**
```java
// 点赞时增加分数
Double newScore = boundUtil.zIncrBy("post:ranking:hot", postId, 10.0);

// 取消点赞时减少分数
Double newScore = boundUtil.zIncrBy("post:ranking:hot", postId, -10.0);
```

3. **统计文章阅读量**
```java
// 每阅读一次，增加阅读量分数
Double newReadCount = boundUtil.zIncrBy("article:ranking:reads", articleId, 1.0);
```

4. **用户活跃度统计**
```java
// 用户每次登录/操作，增加活跃度分数
Double newActivity = boundUtil.zIncrBy("user:ranking:activity", userId, 1.0);
```



### ZREM - 删除成员 ⭐⭐

**Redis命令：** `ZREM key member [member ...]`

**方法签名：**
```java
boundUtil.zRem(key, value1, value2, ...);
```

**用途：** 移除有序集合中一个或多个成员。

**参数说明：**
- `key`: 有序集合的键名
- `value1, value2, ...`: 要删除的成员（可变参数）

**返回值：** 被移除成员的数量。

**使用场景：**

1. **删除帖子时从排行榜移除**
```java
// 删除帖子时，从各种排行榜中移除
boundUtil.zRem("post:ranking:likes", postId);
boundUtil.zRem("post:ranking:hot", postId);
boundUtil.zRem("timeline:posts", postId);
```

2. **用户注销时从排行榜移除**
```java
// 用户注销账号时，从排行榜移除
boundUtil.zRem("user:ranking:activity", userId);
boundUtil.zRem("user:ranking:fans", userId);
```

3. **清理过期任务**
```java
// 处理完延迟队列任务后移除
boundUtil.zRem("order:delayed", orderId);
```



## 2. 查询操作

### ZRANGE - 按排名获取成员 ⭐⭐⭐

**Redis命令：** `ZRANGE key start stop [WITHSCORES]`

**方法签名：**

```java
boundUtil.zRange(key, start, end, Class<T> clazz);
```

**用途：** 返回有序集合中指定排名范围内的成员（按分数从低到高排序）。

**参数说明：**

- `key`: 有序集合的键名
- `start`: 起始排名（0-based，包含）
- `end`: 结束排名（0-based，包含）
- `clazz`: 返回成员类型
- `[WITHSCORES]`：返回成员对应的分数

**索引说明：**
- 索引从 0 开始
- `0` 表示分数最低的成员，`-1` 表示分数最高的成员
- 支持负数索引：`-1` 表示最后一个成员

**返回值：** 指定范围内的成员集合。

**使用场景：**

1. **获取排行榜前N名**
```java
// 获取游戏排行榜前10名（分数从低到高）
Set<Long> top10 = boundUtil.zRange("game:leaderboard", 0, 9, Long.class);

// 如果要获取分数最高的前10名，应该使用 zReverseRange
```

2. **分页查询排行榜**
```java
// 获取第2页数据（每页10条，排名10-19）
int page = 2;
int pageSize = 10;
long start = (page - 1) * pageSize;  // 10
long end = start + pageSize - 1;     // 19
Set<Long> pageData = boundUtil.zRange("game:leaderboard", start, end, Long.class);
```

3. **获取时间线最早发布的帖子**
```java
// 获取最早发布的10条帖子（分数为时间戳，从小到大）
Set<Long> earliestPosts = boundUtil.zRange("timeline:posts", 0, 9, Long.class);
```



### ZREVRANGE - 按排名反向获取成员 ⭐⭐⭐

**Redis命令：** `ZREVRANGE key start stop [WITHSCORES]`

**方法签名：**
```java
boundUtil.zReverseRange(key, start, end, Class<T> clazz);
```

**用途：** 返回有序集合中指定排名范围内的成员（按分数从高到低排序）。

**参数说明：**
- `key`: 有序集合的键名
- `start`: 起始排名（0-based，包含）
- `end`: 结束排名（0-based，包含）
- `clazz`: 返回成员类型

**返回值：** 指定范围内的成员集合（按分数从高到低）。

**使用场景：**

1. **获取排行榜前N名（分数从高到低）**
```java
// 获取游戏排行榜前10名（分数最高的10个）
Set<Long> top10 = boundUtil.zReverseRange("game:leaderboard", 0, 9, Long.class);
```

2. **获取最新发布的帖子（时间戳从大到小）**
```java
// 获取最新发布的10条帖子（时间戳越大越新）
Set<Long> latestPosts = boundUtil.zReverseRange("timeline:posts", 0, 9, Long.class);
```

3. **热门内容推荐**
```java
// 获取热门帖子前20名（热度分数从高到低）
Set<Long> hotPosts = boundUtil.zReverseRange("post:ranking:hot", 0, 19, Long.class);
```

4. **用户粉丝数排行榜**
```java
// 获取粉丝数最多的前50名用户
Set<Long> topUsers = boundUtil.zReverseRange("user:ranking:fans", 0, 49, Long.class);
```



### ZSCORE - 获取成员分数 ⭐⭐

**Redis命令：** `ZSCORE key member`

**方法签名：**
```java
boundUtil.zScore(key, value);
```

**用途：** 返回有序集合中指定成员的分数。

**参数说明：**
- `key`: 有序集合的键名
- `value`: 成员值

**返回值：** 成员的分数，如果成员不存在返回 `null`。

**使用场景：**

1. **查询用户在排行榜中的分数**
```java
// 查询玩家当前的游戏积分
Double score = boundUtil.zScore("game:leaderboard", userId);
if (score != null) {
    System.out.println("玩家积分: " + score);
} else {
    System.out.println("玩家未在排行榜中");
}
```

2. **检查帖子热度分数**
```java
// 检查帖子的当前热度分数
Double hotScore = boundUtil.zScore("post:ranking:hot", postId);
if (hotScore != null && hotScore > 1000) {
    // 帖子很热门
}
```

3. **验证成员是否存在**
```java
// 检查用户是否在排行榜中
Double score = boundUtil.zScore("user:ranking:activity", userId);
if (score == null) {
    // 用户不在排行榜中，需要初始化
    boundUtil.zAdd("user:ranking:activity", userId, 0.0);
}
```



### ZCARD - 获取集合大小 ⭐⭐

**Redis命令：** `ZCARD key`

**方法签名：**
```java
boundUtil.zCard(key);
```

**用途：** 返回有序集合中的成员数量。

**参数说明：**
- `key`: 有序集合的键名

**返回值：** 有序集合中成员的数量，如果 key 不存在返回 0。

**使用场景：**

1. **统计排行榜总人数**
```java
// 统计游戏排行榜中的总玩家数
Long totalPlayers = boundUtil.zCard("game:leaderboard");
System.out.println("排行榜总人数: " + totalPlayers);
```

2. **计算分页总页数**
```java
// 计算排行榜的总页数
Long totalCount = boundUtil.zCard("post:ranking:hot");
int pageSize = 20;
int totalPages = (int) Math.ceil(totalCount.doubleValue() / pageSize);
```

3. **判断集合是否为空**
```java
// 判断时间线是否为空
Long size = boundUtil.zCard("timeline:posts");
if (size == null || size == 0) {
    // 时间线为空，需要初始化或加载数据
}
```



## 3. 范围查询（按分数）

### ZRANGEBYSCORE - 按分数范围获取成员 ⭐⭐

**Redis命令：** `ZRANGEBYSCORE key min max [WITHSCORES] [LIMIT offset count]`

**注意：** BoundUtil 中可能需要通过原生 RedisTemplate 调用。

**用途：** 返回有序集合中分数在指定范围内的成员（按分数从低到高排序）。

**参数说明：**
- `min`: 最小分数（包含），可以使用 `-inf` 表示负无穷
- `max`: 最大分数（包含），可以使用 `+inf` 表示正无穷
- `LIMIT offset count`: 可选，限制返回数量

**使用场景：**

1. **查询指定分数段的玩家**
```java
// 查询积分在1000-2000之间的玩家
Set<Object> players = redisTemplate.opsForZSet().rangeByScore(
    "game:leaderboard", 1000.0, 2000.0);
```

2. **查询最近发布的帖子（按时间戳）**
```java
// 查询最近1小时内发布的帖子
long oneHourAgo = System.currentTimeMillis() - 3600 * 1000;
long now = System.currentTimeMillis();
Set<Object> recentPosts = redisTemplate.opsForZSet().rangeByScore(
    "timeline:posts", oneHourAgo, now);
```

3. **查询即将到期的订单（延迟队列）**
```java
// 查询5分钟内即将到期的订单
long now = System.currentTimeMillis();
long fiveMinutesLater = now + 5 * 60 * 1000;
Set<Object> expiringOrders = redisTemplate.opsForZSet().rangeByScore(
    "order:delayed", now, fiveMinutesLater);
```



### ZREVRANGEBYSCORE - 按分数范围反向获取成员 ⭐⭐

**Redis命令：** `ZREVRANGEBYSCORE key max min [WITHSCORES] [LIMIT offset count]`

**注意：** BoundUtil 中可能需要通过原生 RedisTemplate 调用。

**用途：** 返回有序集合中分数在指定范围内的成员（按分数从高到低排序）。

**使用场景：**

1. **查询高分段的玩家**
```java
// 查询积分在5000以上的玩家（按分数从高到低）
Set<Object> topPlayers = redisTemplate.opsForZSet().reverseRangeByScore(
    "game:leaderboard", 5000.0, Double.POSITIVE_INFINITY);
```

2. **查询最新发布的帖子（按时间戳从大到小）**
```java
// 查询最近1小时发布的帖子（最新的在前）
long oneHourAgo = System.currentTimeMillis() - 3600 * 1000;
Set<Object> recentPosts = redisTemplate.opsForZSet().reverseRangeByScore(
    "timeline:posts", Double.POSITIVE_INFINITY, oneHourAgo);
```



### ZCOUNT - 统计分数范围内的成员数量 ⭐

**Redis命令：** `ZCOUNT key min max`

**注意：** BoundUtil 中可能需要通过原生 RedisTemplate 调用。

**用途：** 返回有序集合中分数在指定范围内的成员数量。

**使用场景：**

```java
// 统计积分在1000-2000之间的玩家数量
Long count = redisTemplate.opsForZSet().count("game:leaderboard", 1000.0, 2000.0);
```



## 4. 排名查询

### ZRANK - 获取成员排名（从低到高）⭐

**Redis命令：** `ZRANK key member`

**注意：** BoundUtil 中可能需要通过原生 RedisTemplate 调用。

**用途：** 返回有序集合中指定成员的排名（分数从低到高，0-based）。

**返回值：** 成员的排名，如果成员不存在返回 `null`。

**使用场景：**

```java
// 查询玩家在排行榜中的排名（分数从低到高）
Long rank = redisTemplate.opsForZSet().rank("game:leaderboard", userId);
if (rank != null) {
    System.out.println("玩家排名: " + (rank + 1)); // 转换为1-based排名
}
```



### ZREVRANK - 获取成员排名（从高到低）⭐

**Redis命令：** `ZREVRANK key member`

**注意：** BoundUtil 中可能需要通过原生 RedisTemplate 调用。

**用途：** 返回有序集合中指定成员的排名（分数从高到低，0-based）。

**使用场景：**

```java
// 查询玩家在排行榜中的排名（分数从高到低）
Long rank = redisTemplate.opsForZSet().reverseRank("game:leaderboard", userId);
if (rank != null) {
    System.out.println("玩家排名: 第 " + (rank + 1) + " 名");
}
```



## 5. 集合运算

### ZUNIONSTORE - 并集运算并存储 ⭐

**Redis命令：** `ZUNIONSTORE destination numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX]`

**注意：** BoundUtil 中可能需要通过原生 RedisTemplate 调用。

**用途：** 计算多个有序集合的并集，并将结果存储到新的有序集合中。

**参数说明：**
- `AGGREGATE SUM`: 相同成员的分数相加（默认）
- `AGGREGATE MIN`: 相同成员的分数取最小值
- `AGGREGATE MAX`: 相同成员的分数取最大值
- `WEIGHTS`: 权重，可以给不同集合的分数乘以权重

**使用场景：**

1. **合并多个排行榜**
```java
// 合并本周和本周的热门帖子排行榜（分数相加）
Long count = redisTemplate.opsForZSet().unionAndStore(
    "post:ranking:hot:week", 
    Arrays.asList("post:ranking:hot:today", "post:ranking:hot:yesterday"),
    "post:ranking:hot:week");
```

2. **综合评分（多个维度加权）**
```java
// 综合评分 = 点赞数 * 0.5 + 收藏数 * 0.3 + 评论数 * 0.2
// 可以分别存储三个排行榜，然后使用并集运算合并（带权重）
```



### ZINTERSTORE - 交集运算并存储 ⭐

**Redis命令：** `ZINTERSTORE destination numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX]`

**注意：** BoundUtil 中可能需要通过原生 RedisTemplate 调用。

**用途：** 计算多个有序集合的交集，并将结果存储到新的有序集合中。

**使用场景：**

1. **多标签筛选（交集）**
```java
// 找出同时包含"科技"和"AI"标签的帖子
// 假设每个标签是一个有序集合，存储该标签下的帖子
Long count = redisTemplate.opsForZSet().intersectAndStore(
    "tag:posts:科技",
    Arrays.asList("tag:posts:AI"),
    "tag:posts:科技_AI");
```



## 6. 删除操作

### ZREMRANGEBYRANK - 按排名范围删除成员 ⭐

**Redis命令：** `ZREMRANGEBYRANK key start stop`

**注意：** BoundUtil 中可能需要通过原生 RedisTemplate 调用。

**用途：** 删除有序集合中指定排名范围内的成员。

**使用场景：**

```java
// 只保留排行榜前1000名，删除排名1000之后的成员
redisTemplate.opsForZSet().removeRange("game:leaderboard", 1000, -1);
```



### ZREMRANGEBYSCORE - 按分数范围删除成员 ⭐

**Redis命令：** `ZREMRANGEBYSCORE key min max`

**注意：** BoundUtil 中可能需要通过原生 RedisTemplate 调用。

**用途：** 删除有序集合中分数在指定范围内的成员。

**使用场景：**

1. **清理过期数据**
```java
// 删除7天前的帖子（时间戳小于7天前）
long sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 3600 * 1000;
redisTemplate.opsForZSet().removeRangeByScore(
    "timeline:posts", 0, sevenDaysAgo);
```

2. **清理低分玩家**
```java
// 删除积分低于100的玩家
redisTemplate.opsForZSet().removeRangeByScore(
    "game:leaderboard", 0, 100);
```



## 7. 扫描操作

### ZSCAN - 增量遍历 ⭐

**Redis命令：** `ZSCAN key cursor [MATCH pattern] [COUNT count]`

**注意：** BoundUtil 中可能需要通过原生 RedisTemplate 调用。

**用途：** 增量遍历有序集合，避免一次性加载大集合造成阻塞。

**使用场景：**

```java
// 遍历大排行榜（超过1万成员）
Cursor<ZSetOperations.TypedTuple<Object>> cursor = redisTemplate.opsForZSet().scan(
    "game:leaderboard",
    ScanOptions.scanOptions().match("*").count(100).build());
try {
    while (cursor.hasNext()) {
        ZSetOperations.TypedTuple<Object> tuple = cursor.next();
        Object member = tuple.getValue();
        Double score = tuple.getScore();
        // 处理每个成员
    }
} finally {
    cursor.close();
}
```



## 使用示例

![image-20251125093925472](/Users/apple/Pictures/assets/image-20251125093925472.png)

![image-20251125094035368](/Users/apple/Pictures/assets/image-20251125094035368.png)

![image-20251125094103748](/Users/apple/Pictures/assets/image-20251125094103748.png)



## 8. 综合使用场景

### 场景1：游戏积分排行榜

```java
public class LeaderboardService {
    
    private final BoundUtil boundUtil;
    
    /**
     * 更新玩家积分
     */
    public void updateScore(Long userId, double points) {
        boundUtil.zIncrBy("game:leaderboard", userId, points);
    }
    
    /**
     * 获取排行榜前N名
     */
    public Set<Long> getTopN(int n) {
        return boundUtil.zReverseRange("game:leaderboard", 0, n - 1, Long.class);
    }
    
    /**
     * 获取玩家排名和分数
     */
    public RankInfo getPlayerRank(Long userId) {
        Long rank = redisTemplate.opsForZSet().reverseRank("game:leaderboard", userId);
        Double score = boundUtil.zScore("game:leaderboard", userId);
        
        if (rank == null || score == null) {
            return null; // 玩家不在排行榜中
        }
        
        return new RankInfo(rank + 1, score); // 转换为1-based排名
    }
    
    /**
     * 获取玩家附近的排名（前后各10名）
     */
    public List<Long> getNearbyPlayers(Long userId, int range) {
        Long rank = redisTemplate.opsForZSet().reverseRank("game:leaderboard", userId);
        if (rank == null) {
            return Collections.emptyList();
        }
        
        long start = Math.max(0, rank - range);
        long end = rank + range;
        
        return new ArrayList<>(
            boundUtil.zReverseRange("game:leaderboard", start, end, Long.class)
        );
    }
}
```



### 场景2：时间线排序（按发布时间）

```java
public class TimelineService {
    
    private final BoundUtil boundUtil;
    
    /**
     * 发布新帖子
     */
    public void publishPost(Long postId) {
        long timestamp = System.currentTimeMillis();
        boundUtil.zAdd("timeline:posts", postId, timestamp);
    }
    
    /**
     * 获取最新发布的帖子（分页）
     */
    public Set<Long> getLatestPosts(int page, int pageSize) {
        long start = (page - 1) * pageSize;
        long end = start + pageSize - 1;
        return boundUtil.zReverseRange("timeline:posts", start, end, Long.class);
    }
    
    /**
     * 获取指定时间范围内的帖子
     */
    public Set<Object> getPostsByTimeRange(long startTime, long endTime) {
        return redisTemplate.opsForZSet().reverseRangeByScore(
            "timeline:posts", endTime, startTime);
    }
    
    /**
     * 删除旧帖子（清理7天前的数据）
     */
    public void cleanupOldPosts() {
        long sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 3600 * 1000;
        redisTemplate.opsForZSet().removeRangeByScore(
            "timeline:posts", 0, sevenDaysAgo);
    }
}
```



### 场景3：延迟队列

```java
public class DelayedQueueService {
    
    private final BoundUtil boundUtil;
    
    /**
     * 添加延迟任务
     */
    public void addDelayedTask(String taskId, long delaySeconds) {
        long executeTime = System.currentTimeMillis() + delaySeconds * 1000;
        boundUtil.zAdd("delayed:tasks", taskId, executeTime);
    }
    
    /**
     * 获取到期的任务
     */
    public Set<Object> getExpiredTasks() {
        long now = System.currentTimeMillis();
        return redisTemplate.opsForZSet().rangeByScore(
            "delayed:tasks", 0, now);
    }
    
    /**
     * 处理到期任务并删除
     */
    public void processExpiredTasks() {
        Set<Object> expiredTasks = getExpiredTasks();
        for (Object taskId : expiredTasks) {
            // 处理任务
            processTask((String) taskId);
            // 从队列中删除
            boundUtil.zRem("delayed:tasks", taskId);
        }
    }
}
```



### 场景4：热门内容推荐（综合评分）

```java
public class HotContentService {
    
    private final BoundUtil boundUtil;
    
    /**
     * 更新帖子热度分数
     * 热度 = 点赞数 * 0.4 + 收藏数 * 0.3 + 评论数 * 0.2 + 分享数 * 0.1
     */
    public void updateHotScore(Long postId, int likeDelta, int favoriteDelta, 
                                int commentDelta, int shareDelta) {
        double hotDelta = likeDelta * 0.4 + favoriteDelta * 0.3 + 
                         commentDelta * 0.2 + shareDelta * 0.1;
        boundUtil.zIncrBy("post:ranking:hot", postId, hotDelta);
    }
    
    /**
     * 获取热门帖子（前N名）
     */
    public Set<Long> getHotPosts(int limit) {
        return boundUtil.zReverseRange("post:ranking:hot", 0, limit - 1, Long.class);
    }
    
    /**
     * 获取热门帖子分数
     */
    public Double getHotScore(Long postId) {
        return boundUtil.zScore("post:ranking:hot", postId);
    }
}
```



## 9. 性能优化建议

### 1. 大集合处理

**问题：** `ZRANGE` 对于大集合（> 1万成员）会阻塞 Redis。

**解决方案：**
- 使用 `ZSCAN` 增量遍历
- 使用 `ZCARD` 检查集合大小，超过阈值时使用 `ZSCAN`
- 定期清理历史数据，限制集合大小

```java
// 安全的范围查询方法
public Set<Long> safeRange(String key, long start, long end) {
    Long size = boundUtil.zCard(key);
    if (size != null && size > 10000) {
        // 大集合，使用 ZSCAN
        // ... 实现增量遍历
    } else {
        // 小集合，直接使用 ZRANGE
        return boundUtil.zRange(key, start, end, Long.class);
    }
}
```



### 2. 分数设计建议

**整数 vs 浮点数：**
- 对于精度要求不高的场景（如点赞数、阅读量），使用整数
- 对于需要精确计算的场景（如金融、评分），使用浮点数
- **注意**：Redis 使用 IEEE 754 双精度浮点数，可能存在精度误差

**时间戳作为分数：**
- 使用毫秒时间戳：`System.currentTimeMillis()`
- 使用秒时间戳：`System.currentTimeMillis() / 1000`
- 使用负数时间戳实现"倒序"（如 `-System.currentTimeMillis()`）

**综合评分设计：**
- 使用权重组合多个指标
- 可以预先计算并存储，也可以使用 `ZUNIONSTORE` 实时计算



### 3. 内存优化

**定期清理：**
- 使用 `ZREMRANGEBYRANK` 只保留前N名
- 使用 `ZREMRANGEBYSCORE` 删除过期或低分数据
- 设置合理的过期时间（`EXPIRE`）

**分片策略：**
- 对于超大型排行榜，可以按时间或类别分片
- 例如：`leaderboard:2024:01`, `leaderboard:2024:02`



### 4. 并发安全

**原子操作：**
- `ZADD`、`ZINCRBY`、`ZREM` 都是原子操作
- 对于复杂逻辑，使用 Lua 脚本保证原子性

```lua
-- 原子性地更新分数并检查是否超过阈值
local score = redis.call('ZINCRBY', KEYS[1], ARGV[1], ARGV[2])
if score >= tonumber(ARGV[3]) then
    redis.call('ZADD', KEYS[2], score, ARGV[2])
end
return score
```



## 10. 业界常见实践（Sorted Set）

1. **排行榜实现**
   - 游戏积分、用户活跃度、帖子热度等
   - 使用 `ZREVRANGE` 获取前N名
   - 使用 `ZREVRANK` 查询用户排名

2. **时间线排序**
   - 帖子、动态、消息等按时间排序
   - 使用时间戳作为分数
   - 使用 `ZREVRANGE` 获取最新内容

3. **延迟队列**
   - 订单超时、任务调度等
   - 使用到期时间作为分数
   - 定期扫描到期任务（`ZRANGEBYSCORE`）

4. **范围查询**
   - 按分数范围筛选数据
   - 使用 `ZRANGEBYSCORE` 或 `ZREVRANGEBYSCORE`

5. **综合评分**
   - 多维度评分系统
   - 使用 `ZUNIONSTORE` 合并多个排行榜（带权重）

6. **去重+排序**
   - Sorted Set 天然支持去重和排序
   - 适合需要同时满足这两个需求的场景

7. **内存管理**
   - 定期清理历史数据
   - 限制集合大小（只保留前N名）
   - 使用过期时间控制数据生命周期

8. **性能优化**
   - 大集合使用 `ZSCAN` 增量遍历
   - 合理设计分数，避免频繁更新
   - 使用 Lua 脚本减少网络往返

