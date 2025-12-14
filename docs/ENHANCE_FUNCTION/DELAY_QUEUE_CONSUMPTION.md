# Redis SortedSet 延迟队列消费实现说明

## 概述

本文档说明基于 Redis SortedSet 实现的延迟队列的消费机制。延迟队列使用 SortedSet 存储任务，分数为执行时间戳，通过定时轮询方式消费到期任务。

**实现位置**: 
- 接口：`DelayQueueService`
- 实现：`DelayQueueServiceImpl`

---

## 一、延迟队列消费机制

### 1.1 为什么需要消费机制？

延迟队列的核心功能是：
1. **添加任务**：将任务添加到队列，设置执行时间
2. **消费任务**：当任务到期时，自动执行任务

现有的 `DelayQueueService` 提供了：
- ✅ `addTask()` - 添加任务
- ✅ `getReadyTasks()` - 查询到期任务
- ✅ `pollReadyTasks()` - 获取并删除到期任务

但是**缺少自动消费机制**，需要手动调用 `pollReadyTasks()` 来获取任务。因此，需要实现自动消费逻辑。

---

### 1.2 Redis SortedSet 是否有阻塞式命令？

**答案：没有**

与 List 的 `BRPOP`（阻塞式弹出）不同，**SortedSet 没有阻塞式获取命令**。SortedSet 的命令都是非阻塞的：
- `ZRANGEBYSCORE` - 范围查询（非阻塞）
- `ZPOPMIN` / `ZPOPMAX` - 弹出最小/最大分数成员（非阻塞，Redis 5.0+）
- 没有 `BZPOPMIN` / `BZPOPMAX` 等阻塞命令

因此，**必须使用轮询方式**来消费延迟队列。

---

## 二、业界常见做法

### 2.1 定时轮询（推荐）✅

**实现方式**：
- 使用定时任务（如 `@Scheduled` 或 `ThreadPoolTaskScheduler`）
- 定期查询到期任务（如每秒查询一次）
- 获取任务后执行处理逻辑

**优点**：
- ✅ 实现简单
- ✅ 可控性强（可调整轮询间隔）
- ✅ 适合大多数场景

**缺点**：
- ⚠️ 有延迟（最多一个轮询间隔）
- ⚠️ 空轮询会浪费资源

**适用场景**：
- 延迟精度要求不高（秒级）
- 任务量中等
- 大多数业务场景

### 2.2 阻塞轮询

**实现方式**：
- 使用 `while` 循环 + `Thread.sleep()`
- 不断查询到期任务
- 没有任务时休眠一段时间

**优点**：
- ✅ 实现简单
- ✅ 资源占用可控

**缺点**：
- ⚠️ 需要管理线程生命周期
- ⚠️ 异常处理复杂

**适用场景**：
- 简单的单机应用
- 任务量较小

### 2.3 事件驱动（Redis 键空间通知）

**实现方式**：
- 使用 Redis 的键空间通知（Keyspace Notifications）
- 监听 SortedSet 的变化事件
- 当任务到期时触发通知

**优点**：
- ✅ 实时性好
- ✅ 无轮询开销

**缺点**：
- ⚠️ **SortedSet 不支持键空间通知**
- ⚠️ 需要额外的 Redis 配置
- ⚠️ 实现复杂

**适用场景**：
- **不适用于 SortedSet 延迟队列**

### 2.4 混合方案（自适应轮询）

**实现方式**：
- 根据队列中最近任务的执行时间动态调整轮询间隔
- 如果最近任务还有很久才到期，增加轮询间隔
- 如果任务即将到期，减少轮询间隔

**优点**：
- ✅ 资源利用率高
- ✅ 延迟可控

**缺点**：
- ⚠️ 实现复杂
- ⚠️ 需要额外的计算逻辑

**适用场景**：
- 任务量很大
- 对资源利用率要求高

---

## 三、本项目实现方案

### 3.1 实现方式：定时轮询 ✅

本项目采用**定时轮询**方式，使用 `ThreadPoolTaskScheduler` 实现：

```java
// 创建定时轮询任务（每秒执行一次）
ScheduledFuture<?> future = taskScheduler.scheduleWithFixedDelay(
    () -> consumeTasks(queueName, handler),
    DEFAULT_POLL_INTERVAL_MS); // 默认1秒
```

### 3.2 核心组件

#### 3.2.1 任务处理器接口

```java
@FunctionalInterface
interface DelayTaskHandler {
    /**
     * 处理延迟任务
     * @param taskId 任务ID
     * @return 是否处理成功，返回false会触发重试
     */
    boolean handle(String taskId);
}
```

#### 3.2.2 消费流程

```
1. 注册处理器
   ↓
2. 启动消费（开始定时轮询）
   ↓
3. 定时查询到期任务（ZRANGEBYSCORE）
   ↓
4. 获取并删除任务（pollReadyTasks）
   ↓
5. 调用处理器处理任务
   ↓
6. 处理失败则重新入队（延迟重试）
```

### 3.3 关键特性

1. **自动消费**：启动后自动轮询，无需手动调用
2. **失败重试**：处理失败的任务会自动重新入队
3. **异常处理**：异常情况会延迟重试
4. **可控制**：支持启动/停止消费
5. **批量处理**：每次最多处理100个任务

---

## 四、使用示例

### 4.1 基本使用

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final DelayQueueService delayQueueService;
    
    @PostConstruct
    public void init() {
        // 1. 注册任务处理器
        delayQueueService.registerHandler("order", taskId -> {
            // 处理订单取消逻辑
            return cancelOrder(taskId);
        });
        
        // 2. 启动消费
        delayQueueService.startConsuming("order");
    }
    
    public void createOrder(String orderId) {
        // 创建订单
        // ...
        
        // 添加延迟任务：15分钟后自动取消未支付订单
        delayQueueService.addTaskWithDelay("order", orderId, 15 * 60);
    }
    
    private boolean cancelOrder(String orderId) {
        // 检查订单状态
        Order order = orderMapper.selectById(orderId);
        if (order.getStatus() == OrderStatus.UNPAID) {
            // 取消订单
            order.setStatus(OrderStatus.CANCELLED);
            orderMapper.updateById(order);
            log.info("Order cancelled due to timeout: {}", orderId);
            return true;
        }
        return true; // 订单已支付，无需处理
    }
}
```

### 4.2 多队列使用

```java
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final DelayQueueService delayQueueService;
    
    @PostConstruct
    public void init() {
        // 注册多个队列的处理器
        delayQueueService.registerHandler("coupon", this::handleCouponExpiry);
        delayQueueService.registerHandler("message", this::handleMessagePush);
        
        // 启动多个队列的消费
        delayQueueService.startConsuming("coupon");
        delayQueueService.startConsuming("message");
    }
    
    private boolean handleCouponExpiry(String couponId) {
        // 处理优惠券过期
        // ...
        return true;
    }
    
    private boolean handleMessagePush(String messageId) {
        // 处理消息推送
        // ...
        return true;
    }
}
```

### 4.3 手动控制消费

```java
@Service
@RequiredArgsConstructor
public class TaskManagementService {
    
    private final DelayQueueService delayQueueService;
    
    // 启动消费
    public void startQueue(String queueName) {
        delayQueueService.startConsuming(queueName);
    }
    
    // 停止消费
    public void stopQueue(String queueName) {
        delayQueueService.stopConsuming(queueName);
    }
    
    // 检查消费状态
    public boolean isQueueRunning(String queueName) {
        return delayQueueService.isConsuming(queueName);
    }
}
```

### 4.4 处理失败重试

```java
delayQueueService.registerHandler("order", taskId -> {
    try {
        // 处理订单
        boolean success = processOrder(taskId);
        
        if (!success) {
            // 返回false会触发重试（延迟1秒）
            return false;
        }
        
        return true;
    } catch (Exception e) {
        // 异常会触发重试（延迟5秒）
        log.error("Error processing order: {}", taskId, e);
        throw e;
    }
});
```

---

## 五、配置说明

### 5.1 轮询间隔

默认轮询间隔为 **1秒**，可以在 `DelayQueueServiceImpl` 中修改：

```java
/** 默认轮询间隔：1秒 */
private static final long DEFAULT_POLL_INTERVAL_MS = 1000;
```

**建议**：
- 延迟精度要求高：500ms - 1s
- 延迟精度要求中等：1s - 5s
- 延迟精度要求低：5s - 10s

### 5.2 批量大小

默认每次最多处理 **100个任务**：

```java
/** 默认每次获取任务数量 */
private static final int DEFAULT_BATCH_SIZE = 100;
```

**建议**：
- 任务处理快：100 - 500
- 任务处理慢：10 - 50

### 5.3 重试策略

- **处理失败（返回false）**：延迟 **1秒** 后重试
- **处理异常**：延迟 **5秒** 后重试

可以在 `consumeTasks()` 方法中调整重试延迟时间。

---

## 六、性能优化建议

### 6.1 轮询间隔优化

**自适应轮询**：根据最近任务的执行时间动态调整轮询间隔

```java
// 伪代码示例
long nextTaskTime = getNextTaskExecuteTime(queueName);
long currentTime = System.currentTimeMillis();
long delay = nextTaskTime - currentTime;

if (delay > 60000) {
    // 最近任务还有1分钟以上，轮询间隔设为10秒
    pollInterval = 10000;
} else if (delay > 10000) {
    // 最近任务还有10秒以上，轮询间隔设为1秒
    pollInterval = 1000;
} else {
    // 任务即将到期，轮询间隔设为100毫秒
    pollInterval = 100;
}
```

### 6.2 批量处理优化

- 使用 Pipeline 批量获取任务
- 并行处理任务（使用线程池）
- 控制并发数，避免资源耗尽

### 6.3 监控告警

- 监控队列长度
- 监控任务处理延迟
- 监控失败率
- 设置告警阈值

---

## 七、与其他方案对比

### 7.1 vs RabbitMQ 延迟队列

| 特性 | Redis SortedSet | RabbitMQ Delayed Message Plugin |
|------|----------------|--------------------------------|
| 延迟精度 | 秒级 | 毫秒级 |
| 实现复杂度 | 简单 | 中等 |
| 性能 | 高 | 中等 |
| 可靠性 | 中等 | 高 |
| 适用场景 | 简单延迟任务 | 复杂消息队列场景 |

### 7.2 vs 定时任务（@Scheduled）

| 特性 | Redis SortedSet | @Scheduled |
|------|----------------|------------|
| 分布式支持 | ✅ 支持 | ❌ 不支持 |
| 动态添加任务 | ✅ 支持 | ❌ 不支持 |
| 任务持久化 | ✅ 支持 | ❌ 不支持 |
| 实现复杂度 | 中等 | 简单 |

---

## 八、注意事项

### 8.1 任务ID唯一性

确保任务ID在队列中唯一，避免重复处理。

### 8.2 任务幂等性

任务处理逻辑应该是幂等的，因为失败重试可能导致重复处理。

### 8.3 资源清理

应用关闭时，应该停止所有消费任务：

```java
@PreDestroy
public void destroy() {
    delayQueueService.stopConsuming("order");
    delayQueueService.stopConsuming("coupon");
    // ...
}
```

### 8.4 异常处理

确保任务处理器中的异常被正确处理，避免影响其他任务。

### 8.5 队列监控

定期监控队列长度，防止任务堆积。

---

## 九、总结

### 9.1 实现要点

1. **定时轮询**：使用 `ThreadPoolTaskScheduler` 实现定时轮询
2. **处理器模式**：使用函数式接口实现任务处理
3. **失败重试**：自动重试失败的任务
4. **可控制**：支持启动/停止消费

### 9.2 适用场景

- ✅ 订单超时自动取消
- ✅ 优惠券过期提醒
- ✅ 定时推送消息
- ✅ 定时数据同步
- ✅ 其他需要延迟执行的业务场景

### 9.3 不适用场景

- ❌ 需要毫秒级精度的延迟任务
- ❌ 需要复杂消息路由的场景
- ❌ 需要消息持久化保证的场景（可配合数据库实现）

---

## 十、相关文档

- [Redis SortedSet 实现总结](./REDIS_SORTEDSET_IMPLEMENTATION.md)
- [Redis List、Set、SortedSet 当前用法总结](./REDIS_LIST_SET_SORTEDSET_CURRENT_USAGE.md)

