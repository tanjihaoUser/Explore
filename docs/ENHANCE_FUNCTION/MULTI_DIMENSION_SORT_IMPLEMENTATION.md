# 多维度排序实现文档

## 1. 概述

多维度排序功能使用多个 Redis Sorted Set 实现，通过加权合并多个维度的分数得到综合排序结果。支持价格、销量、评分等多个维度的综合排序，适用于商品排序、内容排序、用户排序等场景。

## 2. 业界常见的多维度排序方法

### 2.1 数据库多字段排序
**原理**：使用数据库的 `ORDER BY field1, field2, field3` 多字段排序。

**特点**：
- ✅ 实现简单
- ✅ 支持复杂排序规则
- ❌ 查询性能差（需要排序）
- ❌ 难以实现加权排序

**应用场景**：
- 数据量小的场景
- 需要复杂排序规则的场景

### 2.2 Elasticsearch 多字段排序
**原理**：使用 Elasticsearch 的 `multi_match` 和 `function_score` 实现多维度排序。

**特点**：
- ✅ 查询性能高
- ✅ 支持复杂排序规则
- ✅ 支持相关性排序
- ⚠️ 需要 Elasticsearch 基础设施

**应用场景**：
- 需要全文搜索的场景
- 大型电商平台

### 2.3 Redis Sorted Set 合并排序（本项目采用）
**原理**：使用多个 Sorted Set 存储各维度数据，使用 `ZUNIONSTORE` 加权合并。

**特点**：
- ✅ 实现简单，无需额外组件
- ✅ 查询性能高
- ✅ 支持加权排序
- ✅ 支持动态调整权重
- ⚠️ 需要预先计算综合分数

**应用场景**：
- 已有 Redis 基础设施的场景
- 需要实时排序的场景
- 数据量中等的场景（百万级以内）

## 3. 本项目的实现方式

### 3.1 数据存储结构

#### 维度数据（Sorted Set）
- **Key格式**：`sort:dimension:{dimension}`
- **Score**：该维度的分数
- **Member**：项目ID（itemId）

**示例**：
```
Key: sort:dimension:price
Sorted Set:
  - itemId: "product_123", score: 99.9 (价格，越低越好，需要取反)
  - itemId: "product_456", score: 199.9

Key: sort:dimension:sales
Sorted Set:
  - itemId: "product_123", score: 1000 (销量)
  - itemId: "product_456", score: 500

Key: sort:dimension:rating
Sorted Set:
  - itemId: "product_123", score: 4.5 (评分)
  - itemId: "product_456", score: 4.8
```

#### 综合排序结果（Sorted Set）
- **Key格式**：`sort:result:{resultKey}`
- **Score**：综合分数（加权合并后的分数）
- **Member**：项目ID（itemId）

**示例**：
```
Key: sort:result:composite_20240101
Sorted Set:
  - itemId: "product_123", score: 850.5 (综合分数)
  - itemId: "product_456", score: 720.3
```

### 3.2 Redis 命令使用

| 命令 | 用途 | 示例 |
|------|------|------|
| `ZADD` | 添加单维度数据 | `ZADD sort:dimension:price 99.9 product_123` |
| `ZINCRBY` | 增加单维度分数 | `ZINCRBY sort:dimension:sales 10 product_123` |
| `ZUNIONSTORE` | 多维度合并（加权） | `ZUNIONSTORE sort:result:composite source1 source2 WEIGHTS 0.3 0.7 AGGREGATE SUM` |
| `ZREVRANGE` | 获取排序结果 | `ZREVRANGE sort:result:composite 0 19` |
| `ZREVRANK` | 获取排名 | `ZREVRANK sort:result:composite product_123` |
| `ZSCORE` | 获取分数 | `ZSCORE sort:result:composite product_123` |

### 3.3 核心功能实现

#### 3.3.1 添加维度数据
**功能**：添加或更新某个维度的数据

**实现方式**：
- `addDimensionData(dimension, itemId, score)`：添加单维度数据
- `addDimensionDataBatch(dimension, data)`：批量添加
- `updateDimensionData(dimension, itemId, score)`：更新数据
- `incrementDimensionScore(dimension, itemId, delta)`：增加分数

**特点**：
- 支持单个和批量操作
- 支持更新和增量更新

#### 3.3.2 综合排序
**功能**：将多个维度合并成综合排序结果

**实现方式**：
- `compositeSort(resultKey, dimensions, weights, aggregate)`：执行综合排序
- `resultKey`：结果存储的key（后续通过此key获取结果）
- `dimensions`：维度列表（如：["price", "sales", "rating"]）
- `weights`：权重列表（与dimensions对应，如：[0.3, 0.5, 0.2]）
- `aggregate`：聚合类型（SUM：求和、MAX：取最大值、MIN：取最小值）

**计算流程**：
1. 构建源key列表（各维度的Sorted Set）
2. 执行 `ZUNIONSTORE` 合并所有维度
3. 如果指定了权重，使用加权合并
4. 结果存储到 `sort:result:{resultKey}`

**聚合方式**：
- `SUM`：求和（适用于需要累加的场景，如价格+销量）
- `MAX`：取最大值（适用于取最优值的场景）
- `MIN`：取最小值（适用于取最差值的场景）

#### 3.3.3 获取排序结果
**功能**：获取综合排序结果

**实现方式**：
- `getSortedResult(resultKey, start, end)`：获取排序结果（分页）
- `getItemRank(resultKey, itemId)`：获取项目排名
- `getItemScore(resultKey, itemId)`：获取项目综合分数

**重要提示**：
- 必须先调用 `compositeSort()` 生成结果，才能使用 `getSortedResult()` 获取排序结果
- 如果先调用 `getSortedResult()` 而没有执行 `compositeSort()`，会返回空列表

#### 3.3.4 单维度排序
**功能**：获取单个维度的排序结果

**实现方式**：
- `getDimensionSortResult(dimension, start, end)`：获取单维度排序结果
- `getDimensionScore(dimension, itemId)`：获取单维度分数

## 4. 使用场景

### 4.1 商品综合排序
**场景**：电商平台根据价格、销量、评分综合排序商品

**实现**：
```
// 1. 添加各维度数据
multiDimensionSortService.addDimensionData("price", "product_123", 99.9);
multiDimensionSortService.addDimensionData("sales", "product_123", 1000);
multiDimensionSortService.addDimensionData("rating", "product_123", 4.5);

// 2. 执行综合排序（价格权重0.3，销量权重0.5，评分权重0.2）
multiDimensionSortService.compositeSort(
    "product_composite",
    Arrays.asList("price", "sales", "rating"),
    Arrays.asList(0.3, 0.5, 0.2),
    Aggregate.SUM
);

// 3. 获取排序结果
List<String> sortedProducts = multiDimensionSortService.getSortedResult("product_composite", 0, 19);
```

### 4.2 内容综合排序
**场景**：内容平台根据热度、时间、质量综合排序内容

**实现**：
```
multiDimensionSortService.compositeSort(
    "content_composite",
    Arrays.asList("hot", "time", "quality"),
    Arrays.asList(0.5, 0.3, 0.2),
    Aggregate.SUM
);
```

### 4.3 用户综合排序
**场景**：根据活跃度、贡献度、影响力综合排序用户

**实现**：
```
multiDimensionSortService.compositeSort(
    "user_composite",
    Arrays.asList("activity", "contribution", "influence"),
    Arrays.asList(0.4, 0.3, 0.3),
    Aggregate.SUM
);
```

## 5. 性能优化

### 5.1 批量操作
- 支持批量添加维度数据
- 使用 `ZUNIONSTORE` 批量合并维度

### 5.2 服务器端计算
- `ZUNIONSTORE` 是 Redis 服务器端操作，性能高
- 减少网络传输

### 5.3 结果缓存
- 综合排序结果存储在 Redis 中，可以复用
- 避免重复计算

## 6. 优缺点分析

### 6.1 优点
1. **灵活性强**：支持任意维度和权重配置
2. **查询性能高**：Redis Sorted Set 查询性能 O(log N)
3. **支持动态调整**：可以动态调整权重和维度
4. **实现简单**：基于 Redis，无需额外组件
5. **支持多种聚合方式**：SUM、MAX、MIN

### 6.2 缺点
1. **需要预先计算**：必须先执行 `compositeSort()` 才能获取结果
2. **存储空间**：需要存储各维度数据和综合结果，存储空间较大
3. **实时性**：维度数据更新后需要重新执行 `compositeSort()` 才能反映变化
4. **权重调优**：权重需要根据业务数据调优

## 7. 适用场景

### 7.1 适用场景
- ✅ 需要多维度综合排序的场景
- ✅ 需要灵活配置权重的场景
- ✅ 数据量中等的场景（百万级以内）
- ✅ 已有 Redis 基础设施的场景

### 7.2 不适用场景
- ❌ 只需要单维度排序的场景：可以使用 `RankingService`
- ❌ 数据量极大的场景（亿级）：建议使用 Elasticsearch
- ❌ 需要实时更新的场景：需要频繁重新计算综合排序

## 8. 扩展功能

### 8.1 动态权重
- 根据业务规则动态调整权重
- 根据用户偏好调整权重

### 8.2 结果持久化
- 将综合排序结果持久化到数据库
- 系统启动时从数据库恢复

### 8.3 排序策略
- 支持多种排序策略（综合排序、单维度排序、自定义排序）
- 支持排序策略切换

### 8.4 排序分析
- 分析排序效果
- 优化权重配置


