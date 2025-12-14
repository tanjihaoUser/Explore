# 时间窗口统计实现文档

## 1. 概述

时间窗口统计功能使用 Redis Sorted Set 实现滑动窗口数据统计，支持按时间范围查询、数据点计数、过期数据清理等功能。适用于时间序列数据统计、实时监控、数据分析等场景。

## 2. 业界常见的时间窗口统计方法

### 2.1 数据库时间序列法
**原理**：将时间序列数据存储在数据库中，查询时使用 SQL 聚合函数统计。

**特点**：
- ✅ 数据持久化
- ✅ 支持复杂统计
- ❌ 查询性能差（需要聚合计算）
- ❌ 实时性差

**应用场景**：
- 数据量小的场景
- 需要复杂统计的场景

### 2.2 Redis Sorted Set 时间窗口法（本项目采用）
**原理**：使用 Redis Sorted Set，score 为时间戳，member 为数据值。

**特点**：
- ✅ 查询性能高
- ✅ 支持按时间范围查询
- ✅ 支持自动清理过期数据
- ⚠️ 需要定期持久化到数据库

**应用场景**：
- 需要实时统计的场景
- 数据量中等的场景

### 2.3 时序数据库法
**原理**：使用专门的时序数据库（如 InfluxDB、TimescaleDB）。

**特点**：
- ✅ 专为时间序列数据设计
- ✅ 查询性能高
- ✅ 支持复杂统计
- ❌ 需要额外基础设施

**应用场景**：
- 大规模时间序列数据场景
- 需要复杂统计的场景

## 3. 本项目的实现方式

### 3.1 数据存储结构

#### Redis Sorted Set
- **Key格式**：`stats:window:{metric}`
- **Score**：时间戳（毫秒）
- **Member**：数据值（字符串）

**示例**：
```
Key: stats:window:post:view:123
Sorted Set:
  - value: "1", score: 1703123456789
  - value: "1", score: 1703123456790
  - value: "1", score: 1703123456791
```

### 3.2 Redis 命令使用

| 命令 | 用途 | 示例 |
|------|------|------|
| `ZADD` | 添加数据点（分数为时间戳） | `ZADD stats:window:metric 1703123456789 "1"` |
| `ZRANGEBYSCORE` | 查询时间范围内的数据 | `ZRANGEBYSCORE stats:window:metric startTime endTime` |
| `ZREMRANGEBYSCORE` | 清理过期数据 | `ZREMRANGEBYSCORE stats:window:metric 0 expireTime` |
| `ZCOUNT` | 统计时间范围内数量 | `ZCOUNT stats:window:metric startTime endTime` |
| `ZCARD` | 获取数据点总数 | `ZCARD stats:window:metric` |

### 3.3 核心功能实现

#### 3.3.1 添加数据点
**功能**：添加一个数据点到时间窗口

**实现方式**：
- `addDataPoint(metric, value, timestamp)`：指定时间戳添加
- `addDataPoint(metric, value)`：使用当前时间戳添加

**特点**：
- 支持指定时间戳或使用当前时间
- 数据值可以是任意字符串

#### 3.3.2 查询数据点
**功能**：查询指定时间范围内的数据点

**实现方式**：
- `getDataPoints(metric, startTime, endTime)`：查询时间范围内的数据点
- `getRecentDataPoints(metric, days)`：查询最近N天的数据点
- `getRecentDataPointsByHours(metric, hours)`：查询最近N小时的数据点

**特点**：
- 支持精确时间范围查询
- 支持相对时间查询（最近N天、N小时）

#### 3.3.3 统计数据点数量
**功能**：统计指定时间范围内的数据点数量

**实现方式**：
- `countDataPoints(metric, startTime, endTime)`：统计时间范围内的数量
- 使用 `ZCOUNT` 命令，无需获取列表

**特点**：
- 性能高，直接统计，无需获取数据
- 适用于只需要数量的场景

#### 3.3.4 清理过期数据
**功能**：清理过期的数据点

**实现方式**：
- `cleanExpiredData(metric, expireTime)`：清理指定时间之前的数据
- `cleanExpiredDataByDays(metric, keepDays)`：保留最近N天的数据

**特点**：
- 使用 `ZREMRANGEBYSCORE` 批量删除
- 避免内存无限增长

#### 3.3.5 计算统计值
**功能**：计算时间范围内的统计值（求和、平均值、最大值、最小值）

**实现方式**：
- `calculateStatistics(metric, startTime, endTime)`：计算统计值
- 返回包含 sum、avg、max、min、count 的 Map

**特点**：
- 假设数据值是数字
- 支持基本统计计算

#### 3.3.6 获取总数
**功能**：获取指标的数据点总数

**实现方式**：
- `getTotalCount(metric)`：获取总数
- 使用 `ZCARD` 命令

## 4. 使用场景

### 4.1 实时监控
**场景**：监控系统指标，如请求数、错误数

**实现**：
```
// 记录请求
timeWindowStatisticsService.addDataPoint("api:request", "1");

// 查询最近1小时的请求数
Long count = timeWindowStatisticsService.countDataPoints("api:request", startTime, endTime);
```

### 4.2 业务统计
**场景**：统计业务数据，如浏览量、点赞数

**实现**：
```
// 记录浏览量
timeWindowStatisticsService.addDataPoint("post:view:123", "1");

// 查询最近24小时的数据点
List<String> dataPoints = timeWindowStatisticsService.getRecentDataPointsByHours("post:view:123", 24);
```

### 4.3 数据分析
**场景**：分析数据趋势

**实现**：
```
// 计算统计值
Map<String, Double> stats = timeWindowStatisticsService.calculateStatistics("metric", startTime, endTime);
```

## 5. 性能优化

### 5.1 直接统计
- 使用 `ZCOUNT` 直接统计，无需获取数据列表
- 性能高，节省内存

### 5.2 批量清理
- 使用 `ZREMRANGEBYSCORE` 批量删除过期数据
- 避免逐个删除

### 5.3 数据持久化
- 定期将统计数据持久化到数据库
- 支持历史数据查询

## 6. 优缺点分析

### 6.1 优点
1. **查询性能高**：Redis Sorted Set 查询性能 O(log N)
2. **支持时间范围查询**：可以精确查询任意时间范围
3. **自动排序**：按时间戳自动排序
4. **支持过期清理**：可以自动清理过期数据
5. **灵活的数据值**：数据值可以是任意字符串

### 6.2 缺点
1. **存储空间**：所有数据点存储在 Redis 中，存储空间较大
2. **数据持久化**：需要定期持久化到数据库
3. **统计计算**：需要在应用层进行统计计算

## 7. 适用场景

### 7.1 适用场景
- ✅ 需要实时统计的场景
- ✅ 需要时间范围查询的场景
- ✅ 数据量中等的场景（百万级以内）
- ✅ 需要时间序列数据统计的场景

### 7.2 不适用场景
- ❌ 数据量极大的场景（亿级）：建议使用时序数据库
- ❌ 需要复杂统计的场景：建议使用专业数据分析工具
- ❌ 对数据持久化要求极高的场景：需要加强持久化机制

## 8. 扩展功能

### 8.1 数据聚合
- 支持按时间粒度聚合（如按小时、按天）
- 支持多种聚合函数（sum、avg、max、min）

### 8.2 数据压缩
- 压缩历史数据，减少存储空间
- 保留关键数据点

### 8.3 数据告警
- 设置阈值，超过阈值时告警
- 支持多种告警方式

### 8.4 数据可视化
- 集成图表库，可视化时间序列数据
- 支持多种图表类型


