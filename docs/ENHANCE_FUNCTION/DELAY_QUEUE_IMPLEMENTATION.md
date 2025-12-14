# 延迟队列实现文档

## 1. 概述

延迟队列功能使用 Redis Sorted Set 实现精确的延迟任务队列，支持延迟执行、定时任务、任务重试等功能。通过定时轮询机制消费到期任务，适用于订单超时取消、定时推送、延迟通知等场景。

## 2. 业界常见的延迟队列实现方法

### 2.1 数据库轮询法
**原理**：将延迟任务存储在数据库中，定时扫描数据库查询到期任务。

**特点**：
- ✅ 实现简单
- ✅ 数据持久化
- ❌ 数据库压力大
- ❌ 实时性差（依赖轮询间隔）
- ❌ 扩展性差

**应用场景**：
- 延迟时间较长（小时级、天级）
- 任务量较小的场景

### 2.2 消息队列延迟消息
**原理**：使用消息队列的延迟消息功能（如 RabbitMQ 的延迟插件、RocketMQ 的延迟消息）。

**特点**：
- ✅ 实时性好
- ✅ 支持高并发
- ⚠️ 需要特定的消息队列支持
- ⚠️ 延迟精度受消息队列限制

**应用场景**：
- 已有消息队列基础设施的场景
- 需要高并发的场景

### 2.3 Redis Sorted Set 延迟队列（本项目采用）
**原理**：使用 Redis Sorted Set，score 为执行时间戳，定时轮询获取到期任务。

**特点**：
- ✅ 实现简单，无需额外组件
- ✅ 延迟精度高（毫秒级）
- ✅ 支持高并发
- ✅ 支持任务查询和删除
- ⚠️ 需要定时轮询（有轻微延迟）

**应用场景**：
- 需要精确延迟的场景
- 已有 Redis 基础设施的场景
- 延迟时间在秒级到小时级的场景

### 2.4 时间轮算法
**原理**：使用时间轮数据结构，将延迟任务分配到不同的时间槽。

**特点**：
- ✅ 延迟精度高
- ✅ 内存占用小
- ❌ 实现复杂度高
- ❌ 不支持动态调整延迟时间

**应用场景**：
- 延迟时间固定的场景
- 对内存占用要求高的场景

## 3. 本项目的实现方式

### 3.1 数据存储结构

#### Redis Sorted Set
- **Key格式**：`delay:queue:{queueName}`
- **Score**：任务执行时间戳（毫秒）
- **Member**：任务ID（taskId）
- **数据结构**：Sorted Set 按 score 排序，到期任务排在前面

**示例**：
```
Key: delay:queue:order_timeout
Sorted Set:
  - taskId: "order_123", score: 1703123456789 (已到期)
  - taskId: "order_456", score: 1703123457890 (未到期)
  - taskId: "order_789", score: 1703123458901 (未到期)
```

### 3.2 Redis 命令使用

| 命令 | 用途 | 示例 |
|------|------|------|
| `ZADD` | 添加延迟任务（分数为执行时间戳） | `ZADD delay:queue:order_timeout 1703123456789 order_123` |
| `ZRANGEBYSCORE` | 获取到期任务（分数 <= 当前时间） | `ZRANGEBYSCORE delay:queue:order_timeout 0 currentTime` |
| `ZREM` | 删除任务 | `ZREM delay:queue:order_timeout order_123` |
| `ZCARD` | 获取队列长度 | `ZCARD delay:queue:order_timeout` |
| `ZSCORE` | 获取任务执行时间 | `ZSCORE delay:queue:order_timeout order_123` |

### 3.3 核心功能实现

#### 3.3.1 添加延迟任务
**功能**：添加一个延迟执行的任务

**实现方式**：
- `addTask(queueName, taskId, executeTime)`：指定执行时间戳
- `addTaskWithDelay(queueName, taskId, delaySeconds)`：指定延迟秒数

**特点**：
- 支持精确到毫秒的延迟时间
- 支持多个队列（通过 queueName 区分）

#### 3.3.2 获取到期任务
**功能**：获取已到期的任务列表

**实现方式**：
- `getReadyTasks(queueName, limit)`：获取到期任务（不删除）
- `pollReadyTasks(queueName, limit)`：获取并删除到期任务

**查询逻辑**：
- 使用 `ZRANGEBYSCORE` 查询分数 <= 当前时间的任务
- 限制返回数量，避免一次处理过多任务

#### 3.3.3 任务消费
**功能**：定时轮询消费到期任务

**实现方式**：
- 使用 `ThreadPoolTaskScheduler` 定时轮询（默认1秒间隔）
- 每次获取一批到期任务（默认100个）
- 调用注册的处理器处理任务
- 处理失败的任务自动重试（延迟1秒或5秒）

**消费流程**：
1. 定时轮询（1秒间隔）
2. 获取到期任务（`pollReadyTasks`）
3. 调用处理器处理任务
4. 处理失败则重新添加到队列（延迟重试）

#### 3.3.4 任务管理
**功能**：管理延迟任务

**实现方式**：
- `removeTask(queueName, taskId)`：删除任务
- `getTaskExecuteTime(queueName, taskId)`：获取任务执行时间
- `getQueueSize(queueName)`：获取队列长度
- `clearQueue(queueName)`：清空队列

#### 3.3.5 处理器注册
**功能**：为每个队列注册任务处理器

**实现方式**：
- `registerHandler(queueName, handler)`：注册处理器
- `startConsuming(queueName)`：开始消费队列
- `stopConsuming(queueName)`：停止消费队列

## 4. 使用场景

### 4.1 订单超时取消
**场景**：订单创建后30分钟未支付，自动取消订单

**实现**：
```
// 订单创建时
delayQueueService.addTaskWithDelay("order_timeout", "order_123", 30 * 60);

// 注册处理器
delayQueueService.registerHandler("order_timeout", taskId -> {
    // 检查订单状态，如果未支付则取消
    orderService.cancelOrderIfUnpaid(taskId);
    return true;
});

// 开始消费
delayQueueService.startConsuming("order_timeout");
```

### 4.2 定时推送
**场景**：每天上午9点推送消息给用户

**实现**：
```
// 计算明天的9点时间戳
long tomorrow9am = calculateTomorrow9am();
delayQueueService.addTask("daily_push", "push_123", tomorrow9am);
```

### 4.3 延迟通知
**场景**：用户操作后延迟5秒发送通知

**实现**：
```
delayQueueService.addTaskWithDelay("notification", "notify_123", 5);
```

### 4.4 任务重试
**场景**：任务执行失败后延迟重试

**实现**：
- 处理器返回 `false` 表示失败
- 系统自动将任务重新添加到队列（延迟1秒）
- 异常情况延迟5秒重试

## 5. 性能优化

### 5.1 批量处理
- 每次获取一批到期任务（默认100个），减少 Redis 交互次数
- 批量删除已处理的任务

### 5.2 定时轮询优化
- 轮询间隔可配置（默认1秒）
- 无任务时不阻塞，立即返回

### 5.3 任务去重
- 使用 Sorted Set 的 member 唯一性，同一 taskId 只保留一个任务
- 如果任务已存在，`ZADD` 会更新执行时间

## 6. 优缺点分析

### 6.1 优点
1. **实现简单**：基于 Redis Sorted Set，无需额外组件
2. **延迟精度高**：支持毫秒级延迟
3. **支持高并发**：Redis 性能高，支持大量任务
4. **灵活性强**：支持多个队列、任务查询、任务删除
5. **自动重试**：处理失败的任务自动重试

### 6.2 缺点
1. **轮询延迟**：定时轮询有1秒延迟（可配置）
2. **内存占用**：所有任务存储在 Redis 中，内存占用较大
3. **数据持久化**：Redis 数据可能丢失（需要配置持久化）
4. **单点故障**：依赖 Redis，Redis 故障会影响延迟队列

## 7. 适用场景

### 7.1 适用场景
- ✅ 延迟时间在秒级到小时级的场景
- ✅ 需要精确延迟的场景
- ✅ 任务量中等的场景（百万级以内）
- ✅ 已有 Redis 基础设施的场景

### 7.2 不适用场景
- ❌ 延迟时间很长（天级、月级）的场景：建议使用数据库
- ❌ 需要极高实时性的场景：建议使用消息队列延迟消息
- ❌ 任务量极大的场景（亿级）：建议使用专门的消息队列

## 8. 扩展功能

### 8.1 任务优先级
- 使用多个 Sorted Set 存储不同优先级的任务
- 优先消费高优先级队列

### 8.2 任务状态跟踪
- 使用 Redis Hash 存储任务状态（pending、processing、completed、failed）
- 支持任务状态查询

### 8.3 任务持久化
- 将重要任务持久化到数据库
- Redis 故障时从数据库恢复任务

### 8.4 分布式消费
- 使用 Redis 分布式锁确保任务只被一个消费者处理
- 支持多实例部署


