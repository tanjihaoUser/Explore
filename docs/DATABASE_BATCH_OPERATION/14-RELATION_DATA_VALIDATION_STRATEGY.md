# 关系数据定时校验策略与实践

## 1. 概述

在Redis+数据库的混合存储架构中，关系数据（点赞、收藏、关注、黑名单等）通常采用Write-Behind或Write-Through策略，将Redis作为实时数据源，数据库作为持久化存储。由于网络延迟、系统故障、并发冲突等原因，Redis和数据库之间可能出现数据不一致的情况。

定时校验是确保数据一致性的重要手段，通过定期对比Redis和数据库中的数据，发现并修复不一致问题。

## 2. 业界常见做法

### 2.1 校验策略

#### 2.1.1 全量校验 vs 抽样校验

**全量校验**
- **适用场景**：数据量较小（< 100万），对一致性要求极高
- **优点**：能发现所有不一致问题
- **缺点**：耗时较长，对系统资源消耗大
- **实现方式**：遍历所有数据，逐一对比Redis和数据库

**抽样校验**
- **适用场景**：数据量较大（> 100万），对一致性要求较高但可接受少量不一致
- **优点**：资源消耗小，执行速度快
- **缺点**：可能遗漏部分不一致问题
- **实现方式**：随机抽取一定比例的数据进行校验（如10%、20%）

**推荐做法**：
- 小数据量：全量校验
- 大数据量：抽样校验 + 定期全量校验（如每周一次）

#### 2.1.2 分批校验

**分批校验**是业界最常用的做法，将数据分成多个批次，每次校验一批，避免一次性校验所有数据对系统造成过大压力。

**实现要点**：
1. **批次大小**：根据数据量和系统负载确定，通常为50-500条
2. **进度跟踪**：记录当前校验到哪个批次，支持断点续传
3. **轮询策略**：不同类型的数据轮询校验，避免单次任务耗时过长

**示例代码**：
```java
// 分批校验点赞数据
private void validateLikeDataInBatches() {
    List<Long> postIds = postLikeMapper.selectDistinctPostIdsWithPaging(
            offset, BATCH_SIZE);
    
    if (postIds.isEmpty()) {
        offset = 0; // 重置，开始新一轮校验
        return;
    }
    
    for (Long postId : postIds) {
        validateLikeData(postId);
    }
    
    offset += postIds.size();
}
```

#### 2.1.3 增量校验

**增量校验**只校验最近变更的数据，适用于数据变更频繁但历史数据稳定的场景。

**实现要点**：
1. **变更追踪**：记录数据的最后更新时间
2. **时间窗口**：只校验最近N小时/天内的数据
3. **变更日志**：维护变更日志，追踪数据变更历史

**适用场景**：
- 数据变更频繁（如点赞、收藏）
- 历史数据稳定，很少出现不一致
- 需要快速发现新产生的不一致问题

### 2.2 校验频率

#### 2.2.1 固定间隔校验

**固定间隔**：每N分钟/小时执行一次校验

**优点**：
- 实现简单
- 校验频率可预测

**缺点**：
- 可能在高负载时执行，影响业务
- 无法根据数据变更频率动态调整

**配置示例**：
```yaml
relation:
  validation:
    interval: 1800000  # 30分钟
```

#### 2.2.2 错峰校验

**错峰校验**：在业务低峰期执行校验（如凌晨2-4点）

**优点**：
- 减少对业务的影响
- 可以执行更耗时的全量校验

**缺点**：
- 需要配置cron表达式
- 发现不一致的延迟较大

**配置示例**：
```java
@Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
public void scheduledValidation() {
    // 执行全量校验
}
```

#### 2.2.3 动态频率校验

**动态频率**：根据数据变更频率和系统负载动态调整校验频率

**实现要点**：
1. **监控数据变更频率**：统计单位时间内的数据变更次数
2. **监控系统负载**：CPU、内存、数据库连接数等
3. **动态调整**：变更频繁时提高校验频率，负载高时降低频率

**适用场景**：
- 数据变更频率波动较大
- 系统负载变化明显
- 对一致性要求高但需要平衡性能

### 2.3 修复策略

#### 2.3.1 以Redis为准

**策略**：发现不一致时，以Redis数据为准修复数据库

**适用场景**：
- Redis是实时数据源，用户操作直接写入Redis
- Redis数据更准确，数据库可能因为异步写入失败导致不一致

**实现方式**：
```java
// 发现不一致，以Redis为准修复数据库
if (!redisData.equals(dbData)) {
    // 找出需要新增的（在Redis中但不在数据库中）
    List<Long> toInsert = findMissingInDb(redisData, dbData);
    // 找出需要删除的（在数据库中但不在Redis中）
    List<Long> toDelete = findMissingInRedis(redisData, dbData);
    
    // 批量修复
    batchInsert(toInsert);
    batchDelete(toDelete);
}
```

#### 2.3.2 以数据库为准

**策略**：发现不一致时，以数据库数据为准修复Redis

**适用场景**：
- 数据库是权威数据源
- Redis可能因为缓存过期、内存不足等原因丢失数据

**实现方式**：
```java
// 发现不一致，以数据库为准修复Redis
if (!redisData.equals(dbData)) {
    // 从数据库加载数据到Redis
    loadFromDbToRedis(dbData);
}
```

#### 2.3.3 人工介入

**策略**：发现不一致时，记录日志并告警，等待人工处理

**适用场景**：
- 数据不一致可能由业务逻辑错误引起
- 需要人工判断修复策略
- 对数据准确性要求极高

**实现方式**：
```java
if (!redisData.equals(dbData)) {
    log.error("Data inconsistency detected: redis={}, db={}", redisData, dbData);
    // 发送告警通知
    alertService.sendAlert("Data inconsistency", details);
    // 记录到待处理队列
    inconsistencyQueue.add(new InconsistencyRecord(...));
}
```

### 2.4 校验结果处理

#### 2.4.1 日志记录

**记录内容**：
- 校验时间
- 校验类型（点赞、收藏、关注等）
- 校验的数据ID
- Redis数据量
- 数据库数据量
- 差异数量
- 是否修复成功

**日志级别**：
- DEBUG：数据一致
- WARN：发现不一致但已修复
- ERROR：发现不一致且修复失败

#### 2.4.2 指标监控

**关键指标**：
- 校验执行次数
- 发现不一致次数
- 修复成功次数
- 修复失败次数
- 平均校验耗时
- 平均修复耗时

**监控工具**：
- Prometheus + Grafana
- 自定义指标收集

#### 2.4.3 告警通知

**告警条件**：
- 不一致率超过阈值（如1%）
- 修复失败次数超过阈值
- 校验执行失败

**通知方式**：
- 邮件
- 短信
- 企业微信/钉钉
- 监控平台

## 3. 实现方案

### 3.1 架构设计

```
┌─────────────────┐
│  定时任务调度器   │
│  @Scheduled     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  校验服务        │
│  Validation     │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌──────┐  ┌──────┐
│Redis │  │ 数据库 │
└──────┘  └──────┘
    │         │
    └────┬────┘
         │
         ▼
┌─────────────────┐
│  修复服务        │
│  Fix Service    │
└─────────────────┘
```

### 3.2 核心实现

#### 3.2.1 分批校验实现

```java
/**
 * 分批校验点赞数据
 */
private void validateLikeDataInBatches() {
    try {
        // 分页查询需要校验的帖子ID
        List<Long> postIds = postLikeMapper.selectDistinctPostIdsWithPaging(
                likeValidationOffset, BATCH_VALIDATION_SIZE);
        
        if (postIds.isEmpty()) {
            // 重置偏移量，开始新一轮校验
            likeValidationOffset = 0;
            log.info("Like data validation cycle completed");
            return;
        }

        int validatedCount = 0;
        for (Long postId : postIds) {
            try {
                validateLikeData(postId);
                validatedCount++;
            } catch (Exception e) {
                log.error("Failed to validate like data for postId: {}", postId, e);
            }
        }

        likeValidationOffset += postIds.size();
        log.info("Validated {} like records, current offset: {}", 
                validatedCount, likeValidationOffset);
    } catch (Exception e) {
        log.error("Failed to validate like data in batches", e);
    }
}
```

#### 3.2.2 轮询校验实现

```java
/**
 * 定时校验任务 - 轮询不同类型的数据
 */
@Scheduled(fixedDelayString = "${relation.validation.interval:1800000}")
public void scheduledValidation() {
    try {
        log.info("Starting scheduled validation, type index: {}", validationTypeIndex);

        // 轮询校验不同类型的数据
        switch (validationTypeIndex % 4) {
            case 0:
                validateLikeDataInBatches();
                break;
            case 1:
                validateFavoriteDataInBatches();
                break;
            case 2:
                validateFollowDataInBatches();
                break;
            case 3:
                validateBlockDataInBatches();
                break;
        }

        // 轮询到下一个类型
        validationTypeIndex = (validationTypeIndex + 1) % 4;

        log.info("Scheduled validation completed, next type index: {}", validationTypeIndex);
    } catch (Exception e) {
        log.error("Scheduled validation failed", e);
    }
}
```

#### 3.2.3 数据修复实现

```java
/**
 * 修复点赞数据不一致
 */
private boolean fixLikeData(Long postId, Set<Long> redisLikers, Set<Long> dbLikers) {
    try {
        // 找出需要新增的（在Redis中但不在数据库中）
        List<Long> toInsert = new ArrayList<>();
        if (redisLikers != null) {
            for (Long userId : redisLikers) {
                if (!dbLikers.contains(userId)) {
                    toInsert.add(userId);
                }
            }
        }

        // 找出需要删除的（在数据库中但不在Redis中）
        List<Long> toDelete = new ArrayList<>();
        for (Long userId : dbLikers) {
            if (redisLikers == null || !redisLikers.contains(userId)) {
                toDelete.add(userId);
            }
        }

        // 批量修复
        int fixedCount = 0;
        if (!toInsert.isEmpty()) {
            List<PostLike> insertList = new ArrayList<>();
            for (Long userId : toInsert) {
                insertList.add(PostLike.builder()
                        .postId(postId)
                        .userId(userId)
                        .build());
            }
            fixedCount += postLikeMapper.batchInsert(insertList);
        }

        if (!toDelete.isEmpty()) {
            List<PostLike> deleteList = new ArrayList<>();
            for (Long userId : toDelete) {
                deleteList.add(PostLike.builder()
                        .postId(postId)
                        .userId(userId)
                        .build());
            }
            fixedCount += postLikeMapper.batchDelete(deleteList);
        }

        if (fixedCount > 0) {
            log.info("Fixed {} like data inconsistencies for postId: {}", fixedCount, postId);
        }

        return true;
    } catch (Exception e) {
        log.error("Failed to fix like data for postId: {}", postId, e);
        return false;
    }
}
```

### 3.3 配置说明

```yaml
relation:
  validation:
    # 校验间隔（毫秒）
    interval: 1800000  # 30分钟
    # 是否启用定时校验
    enabled: true
    # 每批校验数量
    batch-size: 100
    # 修复配置
    fix:
      # 修复间隔（毫秒）
      interval: 3600000  # 1小时
      # 初始延迟（毫秒）
      initialDelay: 300000  # 5分钟
      # 是否启用定时修复
      enabled: true
```

## 4. 应用场景

### 4.1 高并发场景

**特点**：
- 数据变更频繁
- 并发冲突可能导致数据不一致
- 对一致性要求高

**策略**：
- 提高校验频率（如每10分钟一次）
- 使用分批校验，避免影响业务
- 以Redis为准修复数据库

### 4.2 大数据量场景

**特点**：
- 数据量巨大（> 1000万）
- 全量校验耗时过长
- 对一致性要求中等

**策略**：
- 使用抽样校验（如10%）
- 定期全量校验（如每周一次）
- 分批校验，每批数量适中（50-100）

### 4.3 低并发场景

**特点**：
- 数据变更较少
- 系统负载低
- 对一致性要求高

**策略**：
- 可以执行全量校验
- 校验频率可以较低（如每小时一次）
- 错峰执行，减少对业务的影响

### 4.4 关键业务场景

**特点**：
- 数据准确性要求极高
- 数据不一致可能导致业务问题
- 需要快速发现和修复不一致

**策略**：
- 提高校验频率（如每5分钟一次）
- 全量校验 + 增量校验结合
- 发现不一致立即告警
- 人工介入处理关键不一致

## 5. 性能优化

### 5.1 数据库查询优化

**优化点**：
1. **索引优化**：为查询字段添加索引
2. **分页查询**：使用LIMIT和OFFSET，避免一次性查询大量数据
3. **批量查询**：使用IN查询，减少查询次数

**示例**：
```sql
-- 分页查询有点赞的帖子ID
SELECT DISTINCT post_id 
FROM post_like 
ORDER BY post_id 
LIMIT 100 OFFSET 0;
```

### 5.2 Redis查询优化

**优化点**：
1. **Pipeline**：批量执行Redis命令，减少网络往返
2. **连接池**：复用Redis连接，减少连接开销
3. **异步查询**：使用异步方式查询Redis，提高并发性能

**示例**：
```java
// 使用Pipeline批量查询
List<Object> results = redisTemplate.executePipelined(
    (RedisCallback<Object>) connection -> {
        for (Long postId : postIds) {
            connection.sMembers((POST_LIKE_PREFIX + postId).getBytes());
        }
        return null;
    }
);
```

### 5.3 并发控制

**优化点**：
1. **线程池**：使用线程池执行校验任务，避免阻塞主线程
2. **限流**：控制校验频率，避免对系统造成过大压力
3. **锁机制**：避免多个校验任务同时执行

**示例**：
```java
@Scheduled(fixedDelayString = "${relation.validation.interval:1800000}")
public void scheduledValidation() {
    // 使用分布式锁，避免多实例重复执行
    if (distributedLock.tryLock("validation-lock", 10, TimeUnit.SECONDS)) {
        try {
            // 执行校验逻辑
        } finally {
            distributedLock.unlock("validation-lock");
        }
    }
}
```

## 6. 监控与告警

### 6.1 关键指标

**校验指标**：
- `validation.execution.count`：校验执行次数
- `validation.execution.duration`：校验执行耗时
- `validation.inconsistency.count`：发现不一致次数
- `validation.inconsistency.rate`：不一致率

**修复指标**：
- `fix.execution.count`：修复执行次数
- `fix.execution.duration`：修复执行耗时
- `fix.success.count`：修复成功次数
- `fix.failure.count`：修复失败次数

### 6.2 告警规则

**告警条件**：
1. 不一致率 > 1%
2. 修复失败次数 > 10次/小时
3. 校验执行失败
4. 校验耗时 > 10分钟

**告警方式**：
- 邮件通知
- 短信通知
- 企业微信/钉钉通知
- 监控平台告警

## 7. 最佳实践

### 7.1 校验频率选择

- **高频数据**（点赞、收藏）：每10-30分钟校验一次
- **中频数据**（关注）：每30-60分钟校验一次
- **低频数据**（黑名单）：每1-2小时校验一次

### 7.2 批次大小选择

- **小数据量**（< 10万）：批次大小100-500
- **中数据量**（10万-100万）：批次大小50-200
- **大数据量**（> 100万）：批次大小20-100

### 7.3 修复策略选择

- **实时数据源为Redis**：以Redis为准修复数据库
- **实时数据源为数据库**：以数据库为准修复Redis
- **关键业务数据**：人工介入处理

### 7.4 错峰执行

- **业务高峰期**：降低校验频率或暂停校验
- **业务低峰期**：提高校验频率或执行全量校验
- **凌晨时段**：执行全量校验和深度修复

## 8. 总结

定时校验是确保Redis和数据库数据一致性的重要手段。通过合理的校验策略、修复策略和性能优化，可以在保证数据一致性的同时，最小化对系统性能的影响。

**核心要点**：
1. **分批校验**：避免一次性校验所有数据
2. **轮询策略**：不同类型数据轮询校验
3. **进度跟踪**：支持断点续传
4. **以Redis为准**：修复数据库不一致
5. **监控告警**：及时发现和处理问题

**适用场景**：
- Redis+数据库混合存储架构
- 对数据一致性要求较高的业务
- 需要自动发现和修复数据不一致的系统

