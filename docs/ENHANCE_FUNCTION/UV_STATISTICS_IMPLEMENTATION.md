# UV统计实现文档

## 1. 业界常见的UV统计方法

### 1.1 按天统计法（Daily UV）
**原理**：每天独立统计一份UV数据，然后根据需求进行合并去重。

**特点**：
- ✅ 可以查看每天的UV变化趋势（绘制曲线图）
- ✅ 可以灵活统计任意时间段的UV（通过合并多天数据并去重）
- ✅ 数据粒度细，便于分析
- ⚠️ 需要存储每天的详细访客列表（存储空间较大）

**应用场景**：
- 网站分析工具（如Google Analytics）
- 内容平台（如博客、论坛）
- 电商平台（商品浏览量统计）

**示例**：
```
用户A在2024-01-01访问了帖子123 → 记录到 uv:daily:post:123:20240101
用户A在2024-01-02又访问了帖子123 → 记录到 uv:daily:post:123:20240102

按天统计：
- 2024-01-01的UV = 1（用户A）
- 2024-01-02的UV = 1（用户A）

绘制曲线时：显示两天的UV都是1

统计一周总UV时：
- 合并两天的数据并去重 → 总UV = 1（用户A只算一次）
```

### 1.2 累计统计法（Cumulative UV）
**原理**：使用一个全局Set存储所有历史访客，不区分日期。

**特点**：
- ✅ 存储空间相对较小（只存储一份去重后的访客列表）
- ✅ 查询总UV速度快（直接SCARD）
- ❌ 无法查看每天的UV变化趋势
- ❌ 无法统计特定时间段的UV

**应用场景**：
- 只需要总UV的场景
- 存储空间受限的场景

### 1.3 时间窗口统计法（Time Window UV）
**原理**：使用滑动时间窗口，只保留最近N天的数据。

**特点**：
- ✅ 存储空间可控（只保留最近N天）
- ✅ 可以查看最近N天的UV趋势
- ❌ 无法统计超过N天的历史UV

**应用场景**：
- 实时性要求高的场景
- 只需要最近数据的场景

### 1.4 采样统计法（Sampling UV）
**原理**：使用概率算法（如HyperLogLog）估算UV，不存储完整访客列表。

**特点**：
- ✅ 存储空间极小（固定大小，如12KB）
- ✅ 支持超大数据量统计
- ⚠️ 存在误差（通常误差率在0.81%以内）
- ❌ 无法获取具体的访客列表

**应用场景**：
- 超大数据量场景（如亿级UV）
- 对精度要求不高的场景
- Redis HyperLogLog命令

## 2. 本项目的实现方式

### 2.1 采用的方法
**按天统计法 + 热冷数据分离**

结合了按天统计法和时间窗口统计法的优点：
- 使用按天统计法，支持查看每天的UV变化趋势
- 使用热冷数据分离，控制存储空间和查询性能

### 2.2 数据存储策略

#### Redis存储（热数据）
- **存储内容**：最近7天的UV数据
- **数据结构**：Redis Set
- **Key格式**：`uv:daily:{resourceType}:{resourceId}:{date}`
  - `resourceType`: 资源类型（如post、page、user_profile等）
  - `resourceId`: 资源ID（如帖子ID、页面ID等）
  - `date`: 日期（格式：yyyyMMdd，如20240101）

**示例**：
```
uv:daily:post:123:20240101  → Set{userId1, userId2, userId3}
uv:daily:post:123:20240102  → Set{userId1, userId4}
uv:daily:post:123:20240103  → Set{userId2, userId5}
```

#### 数据库存储（冷数据）
- **存储内容**：7天前的UV数据
- **表结构**：`uv_statistics` 表
- **字段**：
  - `resource_type`: 资源类型
  - `resource_id`: 资源ID
  - `date`: 日期（格式：yyyyMMdd）
  - `visitor_id`: 访客ID（用户ID或IP地址）

**数据迁移**：
- 定时任务每天凌晨5点执行
- 将超过7天的Redis数据持久化到数据库
- 从Redis删除已持久化的数据

### 2.3 Redis Set的使用

#### 为什么使用Set？
1. **自动去重**：Set天然支持去重，同一个访客在同一天多次访问只算一次
2. **高效查询**：`SCARD`命令可以快速获取独立访客数
3. **灵活操作**：支持`SADD`、`SMEMBERS`、`SISMEMBER`等操作

#### 使用的Redis命令

| 命令 | 用途 | 示例 |
|------|------|------|
| `SADD` | 记录访问（自动去重） | `SADD uv:daily:post:123:20240101 userId1` |
| `SCARD` | 获取日UV数 | `SCARD uv:daily:post:123:20240101` |
| `SMEMBERS` | 获取所有访客列表 | `SMEMBERS uv:daily:post:123:20240101` |
| `SISMEMBER` | 检查是否已访问 | `SISMEMBER uv:daily:post:123:20240101 userId1` |
| `DEL` | 删除过期数据 | `DEL uv:daily:post:123:20240101` |

#### 存储示例

**场景**：帖子123在2024-01-01的访问记录

```
Key: uv:daily:post:123:20240101
Value (Set):
  - userId1
  - userId2
  - userId3
  - userId1  (重复，Set自动去重，实际只存储一份)

SCARD结果: 3（3个独立访客）
```

### 2.4 统计逻辑

#### 按天统计UV
**场景**：查看某一天的UV数

**实现**：
```java
// 判断日期是否在最近7天内
if (daysDiff < 7) {
    // 从Redis查询
    String key = "uv:daily:post:123:20240101";
    Long count = SCARD key;  // 返回3
} else {
    // 从数据库查询（使用COUNT DISTINCT）
    SELECT COUNT(DISTINCT visitor_id) 
    FROM uv_statistics 
    WHERE resource_type='post' AND resource_id=123 AND date='20240101';
}
```

**结果**：
- 如果用户A在2024-01-01访问了帖子123，则当天的UV = 1（包含用户A）
- 如果用户A在2024-01-02又访问了帖子123，则2024-01-02的UV = 1（包含用户A）

**绘制曲线时**：
- X轴：日期（2024-01-01, 2024-01-02, ...）
- Y轴：每日UV数（1, 1, ...）
- 显示每天的UV变化趋势

#### 时间段统计UV（合并去重）
**场景**：统计一周的总UV数

**实现**：
```java
// 1. 收集7天的访客列表
Set<String> allVisitors = new HashSet<>();

// 2. 从Redis获取最近7天的数据
for (int i = 0; i < 7; i++) {
    String key = "uv:daily:post:123:" + date;
    Set<String> dailyVisitors = SMEMBERS key;
    allVisitors.addAll(dailyVisitors);  // 自动去重
}

// 3. 从数据库获取7天前的数据（如果有）
// 查询并合并到allVisitors

// 4. 返回去重后的总数
return allVisitors.size();
```

**结果**：
- 用户A在2024-01-01和2024-01-02都访问了帖子123
- 按天统计：2024-01-01的UV=1，2024-01-02的UV=1
- 统计一周总UV：合并两天的数据并去重 → 总UV = 1（用户A只算一次）

#### 总UV统计（从创建以来）
**场景**：获取帖子123从创建以来的总UV数

**实现**：
```java
// 1. 从Redis获取最近7天的所有访客（合并所有日期，自动去重）
Set<String> redisVisitors = new HashSet<>();
for (int i = 0; i < 7; i++) {
    Set<String> dailyVisitors = SMEMBERS "uv:daily:post:123:" + date;
    redisVisitors.addAll(dailyVisitors);
}

// 2. 从数据库获取所有历史访客（从最早开始）
List<UVStatistics> dbStatistics = SELECT * FROM uv_statistics 
    WHERE resource_type='post' AND resource_id=123;

Set<String> dbVisitors = new HashSet<>();
for (UVStatistics stat : dbStatistics) {
    dbVisitors.add(stat.getVisitorId());
}

// 3. 合并Redis和数据库的访客，自动去重
Set<String> allVisitors = new HashSet<>(redisVisitors);
allVisitors.addAll(dbVisitors);

// 4. 返回总数
return allVisitors.size();
```

## 3. 实现的功能

### 3.1 记录访问
**方法**：`recordVisit(ResourceType resourceType, Long resourceId, String visitorId)`

**功能**：
- 自动记录当日访问
- 返回是否是新访客（首次访问返回true）

**实现**：
```java
// 获取当前日期
String date = LocalDate.now().format("yyyyMMdd");

// 构建Redis Key
String key = "uv:daily:post:123:20240101";

// 使用SADD记录访问（自动去重）
Long added = SADD key visitorId;

// added > 0 表示是新访客
return added > 0;
```

### 3.2 获取总UV
**方法**：`getUV(ResourceType resourceType, Long resourceId)`

**功能**：
- 获取从创建以来的总UV数
- 合并Redis（最近7天）和数据库（历史数据）的数据
- 自动去重

**实现**：
- 从Redis获取最近7天的所有访客（合并所有日期）
- 从数据库获取所有历史访客
- 合并两个Set，自动去重
- 返回总数

### 3.3 获取日UV
**方法**：`getDailyUV(String resourceType, Long resourceId, String date)`

**功能**：
- 获取指定日期的UV数
- 自动判断从Redis还是数据库查询

**实现**：
- 判断日期是否在最近7天内
- 如果在7天内：从Redis查询（`SCARD`）
- 如果超过7天：从数据库查询（`COUNT DISTINCT`）

### 3.4 合并多天UV
**方法**：`mergeUV(String resourceType, Long resourceId, Set<String> dates)`

**功能**：
- 合并指定日期列表的UV数据
- 自动去重（同一访客在多天访问只算一次）

**实现**：
- 遍历日期列表
- 对每个日期：从Redis或数据库获取访客列表
- 合并所有访客到一个Set（自动去重）
- 返回总数

**使用场景**：
- 统计一周的UV：`mergeUV("post", 123, ["20240101", "20240102", ..., "20240107"])`
- 统计一个月的UV：传入该月的所有日期

### 3.5 检查是否已访问
**方法**：`hasVisited(String resourceType, Long resourceId, String visitorId)`

**功能**：
- 检查指定访客是否访问过该资源
- 查询最近7天的Redis数据和历史数据库数据

**实现**：
- 先检查最近7天的Redis数据（使用`SISMEMBER`）
- 如果未找到，再查询数据库

### 3.6 获取用户所有帖子的UV统计
**方法**：`getUserPostsUV(Long userId)`

**功能**：
- 获取用户所有帖子的UV统计
- 包含帖子ID、标题、总UV、点赞数、收藏数、评论数等信息

**实现**：
- 查询用户的所有帖子
- 对每个帖子调用`getUV`获取总UV
- 从`RankingService`获取点赞、收藏、评论数
- 返回统计列表

## 4. 数据持久化

### 4.1 持久化策略
- **触发时机**：每天凌晨5点执行定时任务
- **保留策略**：Redis中保留最近7天的数据
- **迁移逻辑**：
  1. 扫描所有`uv:daily:*`的Redis keys
  2. 解析key中的日期，判断是否超过7天
  3. 如果超过7天：
     - 从Redis获取所有访客列表（`SMEMBERS`）
     - 批量插入到数据库
     - 从Redis删除该key（`DEL`）

### 4.2 持久化服务
**类**：`UVStatisticsPersistenceService`

**实现**：
- 实现`DataPersistenceService<UVStatistics>`接口
- 由`DataPersistenceTaskExecutor`统一调度
- 使用`SCAN`命令安全地遍历keys（避免阻塞Redis）

## 5. 性能优化

### 5.1 热冷数据分离
- **热数据（Redis）**：最近7天的数据，查询速度快
- **冷数据（数据库）**：历史数据，查询相对较慢但存储成本低

### 5.2 批量操作
- **批量插入**：使用`batchInsert`一次性插入多条记录
- **批量查询**：使用`selectByResourceAndDateRange`批量查询

### 5.3 去重优化
- **Set自动去重**：Redis Set天然支持去重，无需额外处理
- **合并去重**：使用Java `HashSet`合并多天数据，自动去重

### 5.4 查询优化
- **索引优化**：数据库表在`(resource_type, resource_id, date)`上建立索引
- **分页查询**：对于大量数据，支持分页查询

## 6. 使用示例

### 6.1 记录访问
```java
// 用户123访问帖子456
uvStatisticsService.recordVisit(
    ResourceType.POST, 
    456L, 
    "123"
);
```

### 6.2 获取总UV
```java
// 获取帖子456的总UV数
Long totalUV = uvStatisticsService.getUV(ResourceType.POST, 456L);
```

### 6.3 获取日UV
```java
// 获取帖子456在2024-01-01的UV数
Long dailyUV = uvStatisticsService.getDailyUV("post", 456L, "20240101");
```

### 6.4 合并多天UV
```java
// 统计帖子456在2024-01-01到2024-01-07的UV数
Set<String> dates = Set.of("20240101", "20240102", "20240103", 
                           "20240104", "20240105", "20240106", "20240107");
Long weeklyUV = uvStatisticsService.mergeUV("post", 456L, dates);
```

### 6.5 绘制UV曲线
```java
// 获取最近7天的每日UV数据
List<Long> dailyUVs = new ArrayList<>();
for (int i = 0; i < 7; i++) {
    String date = LocalDate.now().minusDays(i).format("yyyyMMdd");
    Long uv = uvStatisticsService.getDailyUV("post", 456L, date);
    dailyUVs.add(uv);
}

// 使用ECharts等图表库绘制曲线
// X轴：日期列表
// Y轴：dailyUVs列表
```

## 7. 总结

### 7.1 实现特点
1. **按天统计**：每天独立统计一份UV数据，支持查看每天的变化趋势
2. **灵活合并**：可以灵活统计任意时间段的UV，通过合并多天数据并去重
3. **热冷分离**：Redis存储热数据（最近7天），数据库存储冷数据（历史数据）
4. **自动去重**：使用Redis Set和Java HashSet自动去重，无需手动处理

### 7.2 适用场景
- ✅ 需要查看每天UV变化趋势的场景
- ✅ 需要统计任意时间段UV的场景
- ✅ 需要精确UV统计的场景（不使用HyperLogLog估算）
- ✅ 数据量中等（百万级以内）的场景

### 7.3 不适用场景
- ❌ 超大数据量（亿级UV）场景：建议使用HyperLogLog
- ❌ 只需要总UV的场景：可以使用更简单的累计统计法
- ❌ 存储空间极度受限的场景：可以考虑采样统计法
