# 数据持久化任务统一管理方案

## 一、业界常见做法

### 1.1 统一管理 vs 分散管理

#### ✅ **统一管理（推荐）**
- **优点**：
  - 所有定时任务集中在一个类中，便于监控和维护
  - 统一的错误处理和日志记录
  - 便于任务调度和资源管理
  - 避免任务冲突（通过不同的执行时间）
  - 便于扩展新的持久化任务
- **缺点**：
  - 需要额外的抽象层（接口和实现）
  - 可能增加代码复杂度

#### ❌ **分散管理（不推荐）**
- **优点**：
  - 代码简单，每个 Service 自己管理定时任务
  - 职责清晰，每个 Service 只关注自己的数据
- **缺点**：
  - 定时任务分散在各个 Service 中，难以统一管理
  - 错误处理和日志记录不统一
  - 难以监控所有任务的执行情况
  - 任务冲突风险高

### 1.2 业界主流做法

**Spring Boot 项目中的常见做法：**

1. **统一管理 + 模板方法模式**（本项目采用）
   - 创建一个 `DataPersistenceTaskExecutor` 统一管理所有定时任务
   - 使用 `DataPersistenceService` 接口定义统一的数据持久化规范
   - 各个具体的数据类型实现该接口
   - 使用模板方法模式提取公共流程

2. **使用 Spring Batch**
   - 适合复杂的数据处理任务
   - 支持任务调度、监控、重试等高级功能
   - 适合大数据量的批处理场景

3. **使用 Quartz Scheduler**
   - 功能强大的任务调度框架
   - 支持集群、持久化、动态调度等
   - 适合复杂的任务调度需求

4. **使用 XXL-JOB 等分布式任务调度平台**
   - 适合微服务架构
   - 支持任务分片、故障转移等
   - 适合大规模分布式系统

## 二、本项目实现方案

### 2.1 架构设计

```
DataPersistenceTaskExecutor (统一执行器)
    ├── @Scheduled 定时任务
    ├── executePersistenceTask() 模板方法
    └── 调用各个 DataPersistenceService

DataPersistenceService<T> (接口)
    ├── UVStatisticsPersistenceService (UV统计)
    ├── BrowseHistoryPersistenceService (浏览历史)
    └── TimeWindowStatisticsPersistenceService (时间窗口统计)
```

### 2.2 核心组件

#### 1. DataPersistenceService 接口
定义统一的数据持久化规范：
- `getKeyPattern()` - 获取 Redis key 模式
- `getKeepDays()` - 获取数据保留天数
- `parseExpireTimeFromKey()` - 从 key 解析过期时间
- `collectDataFromRedis()` - 从 Redis 收集数据
- `batchInsertToDatabase()` - 批量插入数据库
- `deleteFromRedis()` - 删除 Redis 数据

#### 2. DataPersistenceTaskExecutor 执行器
统一管理所有定时任务：
- 使用模板方法模式提取公共流程
- 统一的错误处理和日志记录
- 统一的批量处理逻辑

#### 3. 具体实现类
每个数据类型实现 `DataPersistenceService` 接口：
- `UVStatisticsPersistenceService` - UV统计
- `BrowseHistoryPersistenceService` - 浏览历史
- `TimeWindowStatisticsPersistenceService` - 时间窗口统计

### 2.3 执行流程

```
1. 定时任务触发（@Scheduled）
   ↓
2. 查找对应的 DataPersistenceService
   ↓
3. 执行模板方法 executePersistenceTask()
   ├── 第一阶段：使用 SCAN 查找所有 Redis keys
   ├── 第二阶段：收集需要持久化的数据
   ├── 第三阶段：批量插入到数据库
   └── 第四阶段：删除 Redis 中的旧数据
```

## 三、优势分析

### 3.1 代码复用
- **公共流程统一**：SCAN 查找、批量插入、批量删除等逻辑统一实现
- **减少重复代码**：各个 Service 只需要实现具体的数据处理逻辑

### 3.2 易于维护
- **统一管理**：所有定时任务在一个类中，便于查看和维护
- **统一日志**：所有任务的日志格式统一，便于监控和排查问题
- **统一错误处理**：统一的异常处理机制，避免任务失败影响其他任务

### 3.3 易于扩展
- **新增任务简单**：只需要实现 `DataPersistenceService` 接口，并在执行器中添加定时任务
- **不影响现有代码**：新增任务不会影响现有的任务执行

### 3.4 性能优化
- **批量处理**：统一的批量插入和删除逻辑，提高性能
- **避免阻塞**：使用 SCAN 替代 KEYS，避免阻塞 Redis

## 四、使用示例

### 4.1 添加新的持久化任务

```java
// 1. 实现 DataPersistenceService 接口
@Service
@RequiredArgsConstructor
public class NewDataPersistenceService implements DataPersistenceService<NewData> {
    // 实现接口方法
}

// 2. 在 DataPersistenceTaskExecutor 中添加定时任务
@Scheduled(cron = "0 0 6 * * ?")
@Transactional(rollbackFor = Exception.class)
public void persistNewData() {
    persistenceServices.stream()
            .filter(service -> "New Data Persistence".equals(service.getTaskName()))
            .findFirst()
            .ifPresent(this::executePersistenceTask);
}
```

### 4.2 任务执行时间安排

| 任务 | 执行时间 | 说明 |
|------|---------|------|
| TimeWindowStatistics | 凌晨3点 | 最早执行，数据量可能较大 |
| BrowseHistory | 凌晨4点 | 中等数据量 |
| UVStatistics | 凌晨5点 | 数据量相对较小 |

**原则**：
- 错开执行时间，避免资源竞争
- 数据量大的任务先执行
- 重要任务放在后面执行（避免被前面的任务影响）

## 五、注意事项

### 5.1 事务管理
- 使用 `@Transactional` 确保数据一致性
- 批量操作失败时，需要记录日志并继续处理其他数据

### 5.2 错误处理
- 单个 key 处理失败不应影响整个任务
- 批量插入失败时，记录日志并继续处理下一批
- 使用 try-catch 包裹关键操作

### 5.3 性能优化
- 使用 SCAN 替代 KEYS，避免阻塞 Redis
- 批量插入和删除，减少数据库交互
- 合理设置批量大小（默认 1000）

### 5.4 监控和告警
- 记录任务执行时间、处理数量等关键指标
- 任务失败时发送告警
- 定期检查任务执行情况

## 六、总结

本项目采用**统一管理 + 模板方法模式**的方案，具有以下特点：

1. **统一管理**：所有定时任务在一个类中管理
2. **代码复用**：公共流程统一实现，减少重复代码
3. **易于扩展**：新增任务只需实现接口
4. **易于维护**：统一的日志和错误处理

这种方案在中小型项目中非常适用，既保证了代码的可维护性，又不会过度设计。

