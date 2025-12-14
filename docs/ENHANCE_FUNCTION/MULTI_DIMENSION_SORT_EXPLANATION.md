# 多维度排序服务详解

## 概述

`MultiDimensionSortService` 用于实现多维度综合排序，基于多个 Redis SortedSet 进行加权合并，得到综合排序结果。

**典型应用场景**：
- 商品排序：价格 + 销量 + 评分
- 内容排序：热度 + 时间 + 质量
- 用户排序：活跃度 + 贡献度 + 影响力

---

## 一、ZUNIONSTORE 命令详解

### 1.1 命令语法

```
ZUNIONSTORE destination numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX]
```

### 1.2 命令作用

将多个 SortedSet 合并成一个新的 SortedSet，存储在 `destination` key 中。

### 1.3 为什么返回 Long 值？

**ZUNIONSTORE 返回的是合并后 SortedSet 中成员的数量（即 ZCARD 的结果）**。

**示例**：
```redis
# 假设有三个维度数据
ZADD sort:dimension:price product1 100 product2 200 product3 150
ZADD sort:dimension:sales product1 50 product2 30 product3 80
ZADD sort:dimension:rating product1 4.5 product2 4.8 product3 4.2

# 执行 ZUNIONSTORE
ZUNIONSTORE sort:result:product 3 sort:dimension:price sort:dimension:sales sort:dimension:rating AGGREGATE SUM

# 返回：3（合并后的SortedSet中有3个成员：product1, product2, product3）
```

### 1.4 聚合方式（AGGREGATE）

当同一个成员（如 product1）在多个 SortedSet 中都存在时，如何计算最终分数：

| 聚合方式 | 说明 | 示例 |
|---------|------|------|
| **SUM** | 求和（默认） | product1: 100 + 50 + 4.5 = 154.5 |
| **MAX** | 取最大值 | product1: max(100, 50, 4.5) = 100 |
| **MIN** | 取最小值 | product1: min(100, 50, 4.5) = 4.5 |

### 1.5 权重（WEIGHTS）

可以为每个源 SortedSet 设置权重，分数会先乘以权重再聚合：

```redis
# 价格权重0.3，销量权重0.5，评分权重0.2
ZUNIONSTORE sort:result:product 3 sort:dimension:price sort:dimension:sales sort:dimension:rating 
    WEIGHTS 0.3 0.5 0.2 AGGREGATE SUM

# product1 的最终分数：
# 100 * 0.3 + 50 * 0.5 + 4.5 * 0.2 = 30 + 25 + 0.9 = 55.9
```

**注意**：当前实现中权重功能尚未完全实现，代码中有警告日志。

---

## 二、类的作用和方法详解

### 2.1 核心作用

**多维度加权排序**：将多个维度的分数按权重合并，得到综合排序结果。

**数据流程**：
```
1. 添加各维度数据（价格、销量、评分等）
   ↓
2. 执行综合排序（compositeSort）- 合并所有维度
   ↓
3. 获取排序结果（getSortedResult）- 按综合分数排序
```

### 2.2 方法详解（带示例）

#### 2.2.1 添加单维度数据

**方法**：`addDimensionData(String dimension, String itemId, double score)`

**作用**：为某个维度添加或更新项目的分数

**示例**：商品排序场景

```java
// 添加价格维度数据
multiDimensionSortService.addDimensionData("price", "product1", 100.0);  // 商品1价格100元
multiDimensionSortService.addDimensionData("price", "product2", 200.0);  // 商品2价格200元
multiDimensionSortService.addDimensionData("price", "product3", 150.0);  // 商品3价格150元

// 添加销量维度数据
multiDimensionSortService.addDimensionData("sales", "product1", 50.0);   // 商品1销量50
multiDimensionSortService.addDimensionData("sales", "product2", 30.0);   // 商品2销量30
multiDimensionSortService.addDimensionData("sales", "product3", 80.0);   // 商品3销量80

// 添加评分维度数据
multiDimensionSortService.addDimensionData("rating", "product1", 4.5);   // 商品1评分4.5
multiDimensionSortService.addDimensionData("rating", "product2", 4.8);   // 商品2评分4.8
multiDimensionSortService.addDimensionData("rating", "product3", 4.2);   // 商品3评分4.2
```

**Redis 存储**：
```
sort:dimension:price -> SortedSet {product1: 100, product2: 200, product3: 150}
sort:dimension:sales -> SortedSet {product1: 50, product2: 30, product3: 80}
sort:dimension:rating -> SortedSet {product1: 4.5, product2: 4.8, product3: 4.2}
```

---

#### 2.2.2 批量添加单维度数据

**方法**：`addDimensionDataBatch(String dimension, Map<String, Double> data)`

**作用**：批量添加某个维度的数据

**示例**：

```java
// 批量添加价格数据
Map<String, Double> priceData = new HashMap<>();
priceData.put("product1", 100.0);
priceData.put("product2", 200.0);
priceData.put("product3", 150.0);
multiDimensionSortService.addDimensionDataBatch("price", priceData);
```

---

#### 2.2.3 更新单维度数据

**方法**：`updateDimensionData(String dimension, String itemId, double score)`

**作用**：更新某个维度中项目的分数（ZADD 如果成员已存在会更新分数）

**示例**：

```java
// 商品1价格从100元涨到120元
multiDimensionSortService.updateDimensionData("price", "product1", 120.0);
```

---

#### 2.2.4 增加单维度分数

**方法**：`incrementDimensionScore(String dimension, String itemId, double delta)`

**作用**：增加某个维度中项目的分数（使用 ZINCRBY）

**示例**：

```java
// 商品1销量增加10
multiDimensionSortService.incrementDimensionScore("sales", "product1", 10.0);
// 结果：product1的销量从50变为60
```

---

#### 2.2.5 综合排序（核心方法）⭐

**方法**：`compositeSort(String resultKey, List<String> dimensions, List<Double> weights, Aggregate aggregate)`

**作用**：将多个维度的数据合并成一个综合排序结果

**示例**：商品综合排序

```java
// 1. 准备维度列表
List<String> dimensions = Arrays.asList("price", "sales", "rating");

// 2. 准备权重列表（价格权重0.3，销量权重0.5，评分权重0.2）
List<Double> weights = Arrays.asList(0.3, 0.5, 0.2);

// 3. 执行综合排序（SUM聚合）
Long count = multiDimensionSortService.compositeSort(
    "product",           // 结果key
    dimensions,          // 维度列表
    weights,             // 权重列表
    Aggregate.SUM        // 聚合方式：求和
);

// 返回：3（合并后的SortedSet中有3个商品）
```

**执行过程**：

1. **构建源key列表**：
   - `sort:dimension:price`
   - `sort:dimension:sales`
   - `sort:dimension:rating`

2. **执行 ZUNIONSTORE**：
   ```redis
   ZUNIONSTORE sort:result:product 3 
       sort:dimension:price sort:dimension:sales sort:dimension:rating 
       WEIGHTS 0.3 0.5 0.2 
       AGGREGATE SUM
   ```

3. **计算结果**（假设权重功能已实现）：
   - product1: 100×0.3 + 50×0.5 + 4.5×0.2 = 30 + 25 + 0.9 = **55.9**
   - product2: 200×0.3 + 30×0.5 + 4.8×0.2 = 60 + 15 + 0.96 = **75.96**
   - product3: 150×0.3 + 80×0.5 + 4.2×0.2 = 45 + 40 + 0.84 = **85.84**

4. **存储结果**：
   ```
   sort:result:product -> SortedSet {
       product3: 85.84,  // 第1名
       product2: 75.96,  // 第2名
       product1: 55.9   // 第3名
   }
   ```

**注意**：当前实现中权重功能尚未完全实现，会使用默认权重（所有维度权重相等）。

---

#### 2.2.6 获取综合排序结果

**方法**：`getSortedResult(String resultKey, long start, long end)`

**作用**：获取综合排序结果（按分数从高到低）

**示例**：

```java
// 获取前10名（分页查询）
List<String> top10 = multiDimensionSortService.getSortedResult("product", 0, 9);
// 返回：["product3", "product2", "product1"]（按综合分数从高到低）

// 获取第11-20名
List<String> page2 = multiDimensionSortService.getSortedResult("product", 10, 19);
```

**重要提示**：⚠️ **必须先调用 `compositeSort()` 生成结果，否则返回空列表**

```java
// ❌ 错误用法：直接获取结果，但还没有执行综合排序
List<String> result = multiDimensionSortService.getSortedResult("product", 0, 9);
// 返回：[]（空列表，因为 sort:result:product 还不存在）

// ✅ 正确用法：先执行综合排序，再获取结果
multiDimensionSortService.compositeSort("product", dimensions, weights, Aggregate.SUM);
List<String> result = multiDimensionSortService.getSortedResult("product", 0, 9);
// 返回：["product3", "product2", "product1"]
```

---

#### 2.2.7 获取项目排名

**方法**：`getItemRank(String resultKey, String itemId)`

**作用**：获取项目在综合排序中的排名（0-based）

**示例**：

```java
// 获取商品1的排名
Long rank = multiDimensionSortService.getItemRank("product", "product1");
// 返回：2（第3名，0-based，所以是2）

// 转换为1-based排名
Long rank1Based = rank != null ? rank + 1 : null;
// 返回：3（第3名）
```

---

#### 2.2.8 获取项目综合分数

**方法**：`getItemScore(String resultKey, String itemId)`

**作用**：获取项目的综合分数

**示例**：

```java
// 获取商品1的综合分数
Double score = multiDimensionSortService.getItemScore("product", "product1");
// 返回：55.9（综合分数）
```

---

#### 2.2.9 获取单维度排序结果

**方法**：`getDimensionSortResult(String dimension, long start, long end)`

**作用**：获取某个维度的排序结果（不经过综合排序）

**示例**：

```java
// 获取价格维度排序（价格从低到高，需要反转）
List<String> priceRanking = multiDimensionSortService.getDimensionSortResult("price", 0, 9);
// 返回：["product1", "product3", "product2"]（按价格从低到高）

// 注意：这里返回的是按分数从高到低，如果价格是越低越好，需要反转结果
```

---

#### 2.2.10 获取单维度分数

**方法**：`getDimensionScore(String dimension, String itemId)`

**作用**：获取项目在某个维度中的分数

**示例**：

```java
// 获取商品1的价格
Double price = multiDimensionSortService.getDimensionScore("price", "product1");
// 返回：100.0
```

---

#### 2.2.11 删除综合排序结果

**方法**：`deleteCompositeResult(String resultKey)`

**作用**：删除综合排序结果（释放内存）

**示例**：

```java
// 删除综合排序结果
multiDimensionSortService.deleteCompositeResult("product");
```

---

## 三、完整使用示例

### 3.1 商品综合排序示例

```java
@Service
@RequiredArgsConstructor
public class ProductSortService {
    
    private final MultiDimensionSortService multiDimensionSortService;
    
    /**
     * 初始化商品排序数据
     */
    public void initProductSort() {
        // 1. 添加价格数据（价格越低越好，所以使用负数或取倒数）
        multiDimensionSortService.addDimensionData("price", "product1", 100.0);
        multiDimensionSortService.addDimensionData("price", "product2", 200.0);
        multiDimensionSortService.addDimensionData("price", "product3", 150.0);
        
        // 2. 添加销量数据（销量越高越好）
        multiDimensionSortService.addDimensionData("sales", "product1", 50.0);
        multiDimensionSortService.addDimensionData("sales", "product2", 30.0);
        multiDimensionSortService.addDimensionData("sales", "product3", 80.0);
        
        // 3. 添加评分数据（评分越高越好）
        multiDimensionSortService.addDimensionData("rating", "product1", 4.5);
        multiDimensionSortService.addDimensionData("rating", "product2", 4.8);
        multiDimensionSortService.addDimensionData("rating", "product3", 4.2);
    }
    
    /**
     * 执行综合排序
     */
    public void performCompositeSort() {
        List<String> dimensions = Arrays.asList("price", "sales", "rating");
        List<Double> weights = Arrays.asList(0.3, 0.5, 0.2); // 价格30%，销量50%，评分20%
        
        // 执行综合排序
        Long count = multiDimensionSortService.compositeSort(
            "product", 
            dimensions, 
            weights, 
            Aggregate.SUM
        );
        
        log.info("Composite sort completed, {} products sorted", count);
    }
    
    /**
     * 获取商品排行榜
     */
    public List<String> getProductRanking(int page, int pageSize) {
        // ⚠️ 重要：必须先执行 compositeSort，否则返回空列表
        long start = (page - 1) * pageSize;
        long end = start + pageSize - 1;
        
        return multiDimensionSortService.getSortedResult("product", start, end);
    }
    
    /**
     * 获取商品排名
     */
    public Long getProductRank(String productId) {
        Long rank = multiDimensionSortService.getItemRank("product", productId);
        return rank != null ? rank + 1 : null; // 转换为1-based排名
    }
}
```

### 3.2 使用流程

```java
// 1. 初始化数据
productSortService.initProductSort();

// 2. 执行综合排序（必须步骤）
productSortService.performCompositeSort();

// 3. 获取排序结果
List<String> top10 = productSortService.getProductRanking(1, 10);

// 4. 获取单个商品排名
Long rank = productSortService.getProductRank("product1");
```

---

## 四、注意事项和常见问题

### 4.1 ⚠️ 必须先执行 compositeSort

**问题**：如果先调用 `getSortedResult()`，result key 还没有初始化，会返回空吗？

**答案**：**是的，会返回空列表**。

```java
// ❌ 错误用法
List<String> result = multiDimensionSortService.getSortedResult("product", 0, 9);
// 返回：[]（空列表）

// ✅ 正确用法
multiDimensionSortService.compositeSort("product", dimensions, null, Aggregate.SUM);
List<String> result = multiDimensionSortService.getSortedResult("product", 0, 9);
// 返回：排序后的商品列表
```

**原因**：
- `getSortedResult()` 只是查询 `sort:result:product` 这个 key
- 如果这个 key 不存在（还没有执行 `compositeSort()`），`ZREVRANGE` 会返回空列表
- Redis 的 `ZREVRANGE` 对不存在的 key 返回空列表，不会报错

**解决方案**：
1. **确保先调用 `compositeSort()`**：在获取结果前必须先执行综合排序
2. **检查返回结果**：如果返回空列表，检查是否已执行 `compositeSort()`
3. **封装方法**：在业务层封装，确保调用顺序正确

### 4.2 权重功能 ✅ 已实现

**当前实现**：权重功能已完全实现，支持通过 `compositeSort()` 方法传入权重列表。

**使用方式**：
```java
List<String> dimensions = Arrays.asList("price", "sales", "rating");
List<Double> weights = Arrays.asList(0.3, 0.5, 0.2); // 价格30%，销量50%，评分20%
multiDimensionSortService.compositeSort("product", dimensions, weights, Aggregate.SUM);
```

**实现原理**：
- 使用 Redis 的 `ZUNIONSTORE` 命令的 `WEIGHTS` 参数
- 每个维度的分数会先乘以对应的权重，再进行聚合（SUM/MAX/MIN）
- 最终分数 = price×0.3 + sales×0.5 + rating×0.2

**注意事项**：
- 权重列表长度必须与维度列表长度一致
- 权重可以是任意正数，不需要总和为1（Redis会自动处理）
- 如果权重为null或空，则所有维度权重相等（默认权重为1.0）

### 4.3 数据更新后需要重新排序

**问题**：如果更新了某个维度的数据，综合排序结果会自动更新吗？

**答案**：**不会自动更新**，需要重新调用 `compositeSort()`。

```java
// 1. 更新销量数据
multiDimensionSortService.incrementDimensionScore("sales", "product1", 10.0);

// 2. 综合排序结果不会自动更新，需要重新执行
multiDimensionSortService.compositeSort("product", dimensions, weights, Aggregate.SUM);

// 3. 现在获取的结果才是最新的
List<String> result = multiDimensionSortService.getSortedResult("product", 0, 9);
```

**优化建议**：
- 如果数据更新频繁，可以考虑定时重新排序
- 或者使用事件驱动，数据更新时触发重新排序

### 4.4 聚合方式的选择

| 聚合方式 | 适用场景 | 示例 |
|---------|---------|------|
| **SUM** | 多维度分数相加 | 价格+销量+评分综合排序 |
| **MAX** | 取各维度最高分 | 取价格、销量、评分中的最高分 |
| **MIN** | 取各维度最低分 | 取价格、销量、评分中的最低分 |

**建议**：大多数场景使用 **SUM**（求和）。

### 4.5 分数归一化问题

**问题**：不同维度的分数范围可能差异很大（如价格100-1000，评分1-5），直接相加可能不公平。

**解决方案**：
1. **归一化处理**：将各维度分数归一化到相同范围（如0-100）
2. **使用权重**：通过权重调整各维度的重要性
3. **取倒数或负数**：对于"越小越好"的维度（如价格），使用倒数或负数

**示例**：
```java
// 价格归一化：将100-1000的价格范围映射到0-100
double normalizedPrice = (maxPrice - price) / (maxPrice - minPrice) * 100;

// 或者使用倒数（价格越低分数越高）
double priceScore = 1000.0 / price;
```

---

## 五、性能优化建议

### 5.1 批量操作

使用 `addDimensionDataBatch()` 批量添加数据，减少网络往返。

### 5.2 结果缓存

综合排序结果可以缓存一段时间，避免频繁重新计算：

```java
// 缓存综合排序结果（5分钟）
@Cacheable(value = "compositeSort", key = "#resultKey", unless = "#result == null || #result.isEmpty()")
public List<String> getSortedResult(String resultKey, long start, long end) {
    // ...
}
```

### 5.3 增量更新

如果只更新了少量数据，可以考虑增量更新而不是重新计算整个排序。

### 5.4 异步计算

对于大量数据的综合排序，可以考虑异步计算：

```java
@Async
public CompletableFuture<Long> compositeSortAsync(String resultKey, ...) {
    // 异步执行综合排序
}
```

---

## 六、总结

### 6.1 核心流程

```
1. 添加各维度数据
   ↓
2. 执行综合排序（compositeSort）- 生成结果key
   ↓
3. 获取排序结果（getSortedResult）- 查询结果key
```

### 6.2 关键点

1. ✅ **必须先执行 `compositeSort()`**，才能获取排序结果
2. ✅ **ZUNIONSTORE 返回的是合并后的成员数量**（Long值）
3. ⚠️ **权重功能尚未完全实现**，当前使用默认权重
4. ⚠️ **数据更新后需要重新排序**，结果不会自动更新

### 6.3 适用场景

- ✅ 商品综合排序（价格+销量+评分）
- ✅ 内容综合排序（热度+时间+质量）
- ✅ 用户综合排序（活跃度+贡献度+影响力）
- ✅ 其他需要多维度综合排序的场景

---

## 七、相关文档

- [Redis SortedSet 实现总结](./REDIS_SORTEDSET_IMPLEMENTATION.md)
- [Redis List、Set、SortedSet 当前用法总结](./REDIS_LIST_SET_SORTEDSET_CURRENT_USAGE.md)

