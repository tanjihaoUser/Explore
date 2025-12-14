# Redis 通用命令与最佳实践

## 一、键（Key）相关命令

### 1.1 KEYS 命令

#### 命令格式
```
KEYS pattern
```

#### 方法说明
- **功能**：查找所有匹配指定模式的键
- **返回值**：匹配的键列表
- **模式支持**：
  - `*`：匹配任意多个字符
  - `?`：匹配单个字符
  - `[abc]`：匹配指定字符中的一个
  - `[a-z]`：匹配字符范围

#### 适用场景
- ✅ **开发/测试环境**：快速查找键，调试和开发
- ✅ **小数据集**：键数量较少（< 1000）的场景
- ❌ **生产环境**：禁止在生产环境使用，特别是 `KEYS *`

#### 注意事项
1. **阻塞性**：KEYS 命令会阻塞 Redis 服务器，直到遍历完所有键
2. **时间复杂度**：O(N)，其中 N 是数据库中键的数量
3. **性能影响**：
   - 如果数据库有大量键，KEYS 命令可能导致 Redis 服务器阻塞数秒甚至更长时间
   - 其他客户端请求可能超时
   - 影响整个系统的响应性能
4. **替代方案**：生产环境必须使用 `SCAN` 命令替代

#### 常见用法
```bash
# 查找所有键（⚠️ 仅限开发/测试环境）
KEYS *

# 查找特定模式的键
KEYS user:*
KEYS post:123:*
KEYS browse:history:user:*
```

#### ⚠️ 生产环境警告
**禁止在生产环境使用 `KEYS *`**，应该使用 `SCAN` 命令替代。

---

### 1.2 SCAN 命令（推荐）

#### 命令格式
```
SCAN cursor [MATCH pattern] [COUNT count]
```

#### 方法说明
- **功能**：增量式遍历键，不会阻塞 Redis 服务器
- **返回值**：包含游标和键列表的数组 `[cursor, [key1, key2, ...]]`
- **游标机制**：使用游标进行迭代，每次返回一部分结果

#### 适用场景
- ✅ **生产环境键遍历**：定时任务、数据清理、统计分析
- ✅ **大数据集**：键数量较多（> 1000）的场景
- ✅ **实时性要求高**：不能阻塞 Redis 服务器的场景

#### 注意事项
1. **非阻塞性**：SCAN 命令是增量式的，不会阻塞 Redis 服务器
2. **时间复杂度**：每次调用 O(1)，但完整遍历需要 O(N)
3. **一致性保证**：在遍历过程中，如果键被修改，可能返回重复或遗漏的键（最终一致性）
4. **COUNT 参数**：
   - 只是建议值，实际返回数量可能更多或更少
   - 过大的 COUNT 值可能导致单次调用时间过长
   - 建议值：小数据集 10-50，大数据集 100-1000
5. **游标处理**：
   - 游标从 0 开始
   - 返回 0 表示遍历完成
   - 每次调用返回新的游标值
6. **避免在遍历过程中修改键**：
   - 遍历过程中修改键可能导致重复或遗漏
   - 如果需要修改，建议先收集所有键，再批量处理

#### 参数说明
- `cursor`：游标，从 0 开始，返回 0 表示遍历完成
- `MATCH pattern`：可选，匹配模式（类似 KEYS 的模式）
- `COUNT count`：可选，建议每次扫描的键数量（默认 10，实际可能更多或更少）

#### 使用示例

**Java 实现（Spring Data Redis）**：
```java
public Set<String> scanKeys(String pattern) {
    Set<String> keys = new HashSet<>();
    ScanOptions options = ScanOptions.scanOptions()
            .match(pattern)
            .count(100)  // 每次扫描建议数量
            .build();
    
    try (Cursor<String> cursor = redisTemplate.scan(options)) {
        while (cursor.hasNext()) {
            keys.add(cursor.next());
        }
    } catch (Exception e) {
        log.error("Failed to scan keys with pattern: {}", pattern, e);
        throw new CacheOperationException("Failed to scan keys", e);
    }
    return keys;
}
```

**使用 BoundUtil（本项目推荐）**：
```java
// 基本用法（默认 count=100）
Set<String> keys = boundUtil.scanKeys("browse:history:user:*");

// 指定 count 值
Set<String> keys = boundUtil.scanKeys("browse:history:user:*", 200);

// 使用回调处理大批量数据（避免内存溢出）
boundUtil.scanKeysWithCallback("browse:history:user:*", 100, (batch) -> {
    // 处理每批键
    processBatch(batch);
});
```

**Lua 脚本实现**：
```lua
-- scan_keys.lua
-- 使用 SCAN 遍历所有匹配的键
local cursor = "0"
local keys = {}
local pattern = ARGV[1] or "*"

repeat
    local result = redis.call("SCAN", cursor, "MATCH", pattern, "COUNT", 100)
    cursor = result[1]
    local batch = result[2]
    for i = 1, #batch do
        table.insert(keys, batch[i])
    end
until cursor == "0"

return keys
```

#### 最佳实践
1. **使用合适的 COUNT 值**：
   - 小数据集：COUNT 10-50
   - 大数据集：COUNT 100-1000
   - 过大的 COUNT 值可能导致单次调用时间过长

2. **处理游标**：
   - 游标从 0 开始
   - 返回 0 表示遍历完成
   - 每次调用返回新的游标值

3. **避免在遍历过程中修改键**：
   - 遍历过程中修改键可能导致重复或遗漏
   - 如果需要修改，建议先收集所有键，再批量处理

---

### 1.3 EXISTS - 检查键是否存在

#### 命令格式
```bash
EXISTS key [key ...]
```

#### 方法说明
- **功能**：检查一个或多个键是否存在
- **返回值**：存在的键数量（0 表示不存在，1 表示存在）
- **时间复杂度**：O(1)

#### 适用场景
- ✅ **缓存命中检查**：判断缓存是否存在
- ✅ **幂等性检查**：判断某个操作是否已执行
- ✅ **批量检查**：一次检查多个键的存在性

#### 注意事项
1. **性能**：O(1) 时间复杂度，性能极好
2. **批量检查**：可以一次检查多个键，返回存在的数量
3. **不区分类型**：只要键存在就返回 1，不关心值的数据类型

#### 使用示例
```bash
# 检查单个键
EXISTS user:123
# 返回：1（存在）或 0（不存在）

# 检查多个键
EXISTS user:123 user:456 user:789
# 返回：存在的键数量（如 2 表示有 2 个键存在）
```

```java
// Java 实现
Boolean exists = boundUtil.exists("user:123");
```

---

### 1.4 DEL - 删除键

#### 命令格式
```bash
DEL key [key ...]
```

#### 方法说明
- **功能**：删除一个或多个键
- **返回值**：成功删除的键数量
- **时间复杂度**：O(1) 对于单个键，O(N) 对于多个键（N 是键的数量）

#### 适用场景
- ✅ **缓存失效**：删除过期的缓存
- ✅ **数据清理**：清理不需要的数据
- ✅ **批量删除**：一次删除多个键

#### 注意事项
1. **原子性**：删除操作是原子性的
2. **不存在的键**：如果键不存在，不会报错，只是返回 0
3. **批量删除**：可以一次删除多个键，返回成功删除的数量
4. **性能**：删除操作很快，但删除大键（如包含大量元素的 Set、List）可能较慢

#### 使用示例
```bash
# 删除单个键
DEL user:123
# 返回：1（成功删除）或 0（键不存在）

# 删除多个键
DEL user:123 user:456 user:789
# 返回：成功删除的键数量（如 2 表示删除了 2 个键）
```

```java
// Java 实现
Boolean deleted = boundUtil.del("user:123");
```

---

### 1.5 EXPIRE / PEXPIRE - 设置过期时间

#### 命令格式
```bash
EXPIRE key seconds
PEXPIRE key milliseconds
```

#### 方法说明
- **功能**：为键设置过期时间
- **返回值**：1 表示设置成功，0 表示键不存在或设置失败
- **时间复杂度**：O(1)

#### 适用场景
- ✅ **缓存过期**：设置缓存的过期时间
- ✅ **临时数据**：存储临时数据，自动清理
- ✅ **限时操作**：限制某个操作的时效性

#### 注意事项
1. **时间单位**：
   - `EXPIRE`：秒为单位
   - `PEXPIRE`：毫秒为单位
2. **更新过期时间**：如果键已经有过期时间，再次设置会更新过期时间
3. **移除过期时间**：使用 `PERSIST` 命令可以移除键的过期时间
4. **过期策略**：Redis 使用惰性删除和定期删除两种策略清理过期键

#### 使用示例
```bash
# 设置 60 秒后过期
EXPIRE user:123 60

# 设置 60000 毫秒后过期
PEXPIRE user:123 60000
```

```java
// Java 实现
boundUtil.expire("user:123", 60, TimeUnit.SECONDS);
```

---

### 1.6 TTL / PTTL - 查看剩余过期时间

#### 命令格式
```bash
TTL key
PTTL key
```

#### 方法说明
- **功能**：查看键的剩余过期时间
- **返回值**：
  - 正数：剩余时间（TTL 返回秒，PTTL 返回毫秒）
  - `-1`：键存在但没有设置过期时间
  - `-2`：键不存在
- **时间复杂度**：O(1)

#### 适用场景
- ✅ **缓存监控**：检查缓存是否即将过期
- ✅ **调试**：调试过期时间设置是否正确
- ✅ **业务逻辑**：根据剩余时间决定是否刷新缓存

#### 注意事项
1. **时间单位**：
   - `TTL`：返回秒
   - `PTTL`：返回毫秒（更精确）
2. **返回值含义**：
   - 正数：剩余时间
   - `-1`：永不过期
   - `-2`：键不存在

#### 使用示例
```bash
# 查看剩余过期时间（秒）
TTL user:123
# 返回：60（剩余 60 秒）或 -1（永不过期）或 -2（不存在）

# 查看剩余过期时间（毫秒）
PTTL user:123
# 返回：60000（剩余 60000 毫秒）
```

```java
// Java 实现
Long ttl = boundUtil.getExpire("user:123", TimeUnit.SECONDS);
```

---

### 1.7 TYPE - 查看键的类型

#### 命令格式
```bash
TYPE key
```

#### 方法说明
- **功能**：查看键的数据类型
- **返回值**：`string`、`list`、`set`、`zset`、`hash`、`stream` 等
- **时间复杂度**：O(1)

#### 适用场景
- ✅ **调试**：检查键的数据类型是否正确
- ✅ **类型检查**：在操作前检查键的类型
- ✅ **工具开发**：开发 Redis 管理工具

#### 注意事项
1. **不存在的键**：如果键不存在，返回 `none`
2. **类型判断**：在操作键之前，可以使用 TYPE 检查类型，避免类型错误

#### 使用示例
```bash
# 查看键的类型
TYPE user:123
# 返回：string、list、set、zset、hash、stream 等
```

```java
// Java 实现（通过 Spring Data Redis）
DataType type = redisTemplate.type("user:123");
```

---

### 1.8 RENAME - 重命名键

#### 命令格式
```bash
RENAME oldkey newkey
```

#### 方法说明
- **功能**：将键重命名为新名称
- **返回值**：`OK` 表示成功
- **时间复杂度**：O(1)

#### 适用场景
- ✅ **键迁移**：将数据从一个键迁移到另一个键
- ✅ **版本升级**：升级键的命名规范
- ✅ **数据重组**：重新组织数据结构

#### 注意事项
1. **覆盖新键**：如果新键已存在，会被覆盖
2. **原子性**：重命名操作是原子性的
3. **不存在的键**：如果旧键不存在，会返回错误
4. **大键性能**：对于大键（包含大量元素），重命名可能较慢

#### 使用示例
```bash
# 重命名键
RENAME user:old:123 user:new:123
```

```java
// Java 实现
boundUtil.rename("user:old:123", "user:new:123");
```

---

## 二、键遍历最佳实践

### 2.1 问题场景

在生产环境中，以下场景需要遍历键：
- 定时任务清理过期数据
- 数据迁移
- 统计分析
- 批量操作

### 2.2 解决方案对比

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|---------|
| KEYS | 简单直接 | 阻塞服务器，性能差 | ❌ 禁止在生产环境使用 |
| SCAN | 非阻塞，性能好 | 可能重复或遗漏，需要多次调用 | ✅ 生产环境推荐 |
| 维护键列表 | 精确，性能好 | 需要额外维护，增加复杂度 | ✅ 适合已知键集合的场景 |

### 2.3 SCAN 使用最佳实践

#### 1. 基本用法
```java
public Set<String> scanKeys(String pattern) {
    Set<String> keys = new HashSet<>();
    ScanOptions options = ScanOptions.scanOptions()
            .match(pattern)
            .count(100)  // 建议值：100-1000
            .build();
    
    try (Cursor<String> cursor = redisTemplate.scan(options)) {
        while (cursor.hasNext()) {
            keys.add(cursor.next());
        }
    } catch (Exception e) {
        log.error("Failed to scan keys with pattern: {}", pattern, e);
    }
    return keys;
}
```

#### 2. 分批处理（避免内存溢出）
```java
public void processKeysInBatches(String pattern, int batchSize, 
                                 Consumer<List<String>> processor) {
    ScanOptions options = ScanOptions.scanOptions()
            .match(pattern)
            .count(100)
            .build();
    
    List<String> batch = new ArrayList<>();
    try (Cursor<String> cursor = redisTemplate.scan(options)) {
        while (cursor.hasNext()) {
            batch.add(cursor.next());
            if (batch.size() >= batchSize) {
                processor.accept(new ArrayList<>(batch));
                batch.clear();
            }
        }
        // 处理最后一批
        if (!batch.isEmpty()) {
            processor.accept(batch);
        }
    }
}
```

#### 3. 带重试的扫描
```java
public Set<String> scanKeysWithRetry(String pattern, int maxRetries) {
    Set<String> keys = new HashSet<>();
    int retryCount = 0;
    
    while (retryCount < maxRetries) {
        try {
            ScanOptions options = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(100)
                    .build();
            
            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(cursor.next());
                }
            }
            break; // 成功，退出循环
        } catch (Exception e) {
            retryCount++;
            log.warn("Scan failed, retry {}/{}: {}", retryCount, maxRetries, e.getMessage());
            if (retryCount >= maxRetries) {
                log.error("Scan failed after {} retries", maxRetries, e);
                throw e;
            }
            // 等待后重试
            try {
                Thread.sleep(1000 * retryCount); // 指数退避
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Scan interrupted", ie);
            }
        }
    }
    return keys;
}
```

### 2.4 项目中的实际使用

#### 当前代码中的使用情况

**BrowseHistoryServiceImpl.persistOldBrowseHistoryToDatabase**：
```java
// ⚠️ 当前使用 KEYS 命令（建议改为 SCAN）
Set<String> allKeys = boundUtil.keys(BROWSE_PREFIX + "*");

// ✅ 推荐改为：
Set<String> allKeys = boundUtil.scanKeys(BROWSE_PREFIX + "*");
```

**TimeWindowStatisticsServiceImpl.persistOldStatisticsToDatabase**：
```java
// ⚠️ 当前使用 KEYS 命令（建议改为 SCAN）
String pattern = STATS_PREFIX + metric + "*";
Set<String> matchingKeys = boundUtil.keys(pattern);

// ✅ 推荐改为：
Set<String> matchingKeys = boundUtil.scanKeys(STATS_PREFIX + metric + "*");
```

#### 迁移建议

对于定时任务中的键遍历，建议从 `keys()` 迁移到 `scanKeys()`：

```java
// 迁移前
Set<String> allKeys = boundUtil.keys("prefix:*");

// 迁移后（基本用法）
Set<String> allKeys = boundUtil.scanKeys("prefix:*");

// 迁移后（大批量数据，使用回调避免内存溢出）
List<String> allKeys = new ArrayList<>();
boundUtil.scanKeysWithCallback("prefix:*", 100, (batch) -> {
    allKeys.addAll(batch);
    // 或者直接处理每批数据，不收集到内存
    processBatch(batch);
});
```

### 2.5 维护键列表方案

对于已知的键集合，可以维护一个索引：

```java
// 方案1：使用 Set 维护键列表
boundUtil.sAdd("index:browse:history:keys", "browse:history:user:123");
boundUtil.sAdd("index:browse:history:keys", "browse:history:user:456");

// 获取所有键
Set<String> keys = boundUtil.sMembers("index:browse:history:keys", String.class);

// 方案2：使用 List 维护键列表（保持顺序）
boundUtil.leftPush("index:browse:history:keys", "browse:history:user:123");
List<String> keys = boundUtil.range("index:browse:history:keys", 0, -1, String.class);
```

---

## 三、性能优化建议

### 3.1 批量操作

#### 使用 Pipeline 减少网络往返
```java
List<Object> results = redisTemplate.executePipelined(new RedisCallback<Object>() {
    @Override
    public Object doInRedis(RedisConnection connection) throws DataAccessException {
        for (int i = 0; i < 1000; i++) {
            connection.set(("key" + i).getBytes(), ("value" + i).getBytes());
        }
        return null;
    }
});
```

#### 使用批量命令
```java
// 批量获取（MGET）
List<String> keys = Arrays.asList("key1", "key2", "key3");
List<String> values = boundUtil.mGet(keys, String.class);

// 批量设置（MSET）
Map<String, String> data = new HashMap<>();
data.put("key1", "value1");
data.put("key2", "value2");
boundUtil.mSet(data);
```

### 3.2 避免大键（Big Key）

#### 问题
- 大键会导致：
  - 网络传输时间长
  - 内存占用大
  - 阻塞其他操作

#### 解决方案
1. **拆分大键**：
   ```java
   // 不推荐：存储整个用户列表
   boundUtil.set("users:all", allUsers);  // 可能几MB
   
   // 推荐：分页存储
   boundUtil.set("users:page:1", page1Users);
   boundUtil.set("users:page:2", page2Users);
   ```

2. **使用 Hash 存储对象**：
   ```java
   // 不推荐：JSON 字符串
   boundUtil.set("user:123", jsonString);  // 可能很大
   
   // 推荐：Hash 存储
   boundUtil.hMSet("user:123", userMap);  // 可以部分更新
   ```

### 3.3 合理设置过期时间

```java
// 设置过期时间，避免内存泄漏
boundUtil.set("cache:key", value, 3600, TimeUnit.SECONDS);

// 使用 EXPIRE 命令
boundUtil.expire("cache:key", 3600, TimeUnit.SECONDS);
```

### 3.4 使用连接池

```yaml
spring:
  redis:
    lettuce:
      pool:
        max-active: 20      # 最大连接数
        max-idle: 10        # 最大空闲连接
        min-idle: 5         # 最小空闲连接
        max-wait: -1        # 最大等待时间（-1表示无限等待）
```

---

## 四、常见问题与解决方案

### 4.1 KEYS 命令导致超时

**问题**：使用 `KEYS *` 导致 Redis 阻塞，其他请求超时。

**解决方案**：
1. 使用 `SCAN` 命令替代
2. 维护键索引（Set 或 List）
3. 使用 Redis 集群，分散压力

### 4.2 内存占用过高

**问题**：Redis 内存使用率过高。

**解决方案**：
1. 设置合理的过期时间
2. 定期清理过期数据
3. 使用 LRU 淘汰策略
4. 拆分大键
5. 使用压缩（如果值可以压缩）

### 4.3 热点键问题

**问题**：某个键访问频率过高，成为瓶颈。

**解决方案**：
1. **读写分离**：使用主从复制，读请求分散到从节点
2. **本地缓存**：在应用层增加本地缓存（如 Caffeine）
3. **分片**：将热点键拆分到多个键
4. **限流**：对热点键的访问进行限流

### 4.4 数据一致性

**问题**：缓存与数据库数据不一致。

**解决方案**：
1. **Write-Through**：写数据库时同步更新缓存
2. **Write-Behind**：先写缓存，异步写数据库
3. **失效策略**：更新数据库时删除缓存
4. **版本号机制**：使用版本号判断缓存是否过期

---

## 五、监控与调试

### 5.1 常用监控命令

```bash
# 查看 Redis 信息
INFO

# 查看内存使用
INFO memory

# 查看客户端连接
CLIENT LIST

# 查看慢查询
SLOWLOG GET 10

# 查看键空间统计
INFO keyspace
```

### 5.2 性能分析

```bash
# 查看命令统计
INFO commandstats

# 查看键空间统计
INFO keyspace

# 监控实时命令
MONITOR  # ⚠️ 仅用于调试，会严重影响性能
```

---

## 六、总结

### 6.1 核心原则

1. **避免阻塞操作**：不使用 `KEYS *`，使用 `SCAN`
2. **批量操作**：使用 Pipeline、MGET、MSET 等减少网络往返
3. **合理设置过期时间**：避免内存泄漏
4. **避免大键**：拆分大键，使用合适的数据结构
5. **监控性能**：定期检查慢查询、内存使用等

### 6.2 命令选择指南

| 场景 | 推荐命令 | 说明 |
|------|---------|------|
| 遍历所有键 | SCAN | 非阻塞，适合生产环境 |
| 检查键是否存在 | EXISTS | O(1)，性能好 |
| 删除键 | DEL | O(1)，支持批量删除 |
| 设置过期时间 | EXPIRE / PEXPIRE | O(1)，避免内存泄漏 |
| 查看剩余过期时间 | TTL / PTTL | O(1)，监控缓存状态 |
| 查看键类型 | TYPE | O(1)，调试和类型检查 |
| 重命名键 | RENAME | O(1)，键迁移和版本升级 |

### 6.3 性能对比

| 操作 | 单次调用 | 说明 |
|------|---------|------|
| EXISTS | ~0.1ms | O(1)，性能极好 |
| DEL | ~0.1ms | O(1)，删除单个键很快 |
| EXPIRE | ~0.1ms | O(1)，设置过期时间 |
| TTL | ~0.1ms | O(1)，查看剩余时间 |
| TYPE | ~0.1ms | O(1)，查看键类型 |
| RENAME | ~0.1ms | O(1)，重命名键 |
| KEYS * | 阻塞，可能数秒 | ❌ 禁止在生产环境使用 |
| SCAN | ~1ms/次 | 非阻塞，适合生产环境 |

---

## 七、参考资料

- [Redis 官方文档](https://redis.io/docs/)
- [Redis 命令参考](https://redis.io/commands/)
- [Spring Data Redis 文档](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)

