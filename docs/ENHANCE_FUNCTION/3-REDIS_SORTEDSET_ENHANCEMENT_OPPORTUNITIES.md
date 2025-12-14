# Redis SortedSet 功能增强建议

## 概述

本文档列出基于当前项目情况，可以为 Redis SortedSet 数据结构增加的功能点。当前项目已使用 SortedSet 实现排行榜、热度榜、时间线等功能，但可以进一步扩展更多高级应用场景。

---

## 一、延迟队列系统

### 1.1 精确延迟队列

**功能描述**: 基于 SortedSet 实现精确的延迟任务队列，任务在指定时间执行。

**应用场景**:
- 订单15分钟未支付自动取消
- 优惠券过期提醒
- 定时推送消息
- 定时数据同步
- 定时任务调度

**实现流程**:
1. **延迟队列**: `delay:queue` - SortedSet，分数为执行时间戳（毫秒）
2. **添加任务**: `ZADD delay:queue {executeTime} {taskId}` 添加延迟任务
3. **定时扫描**: 定时任务（每秒或每几秒）扫描到期任务
4. **获取到期任务**: `ZRANGEBYSCORE delay:queue 0 {currentTime} LIMIT 0 10` 获取前10个到期任务
5. **执行任务**: 执行任务逻辑
6. **删除任务**: `ZREM delay:queue {taskId}` 从队列删除

**涉及 Redis 命令**:
- `ZADD delay:queue {executeTime} {taskId}` - 添加延迟任务
- `ZRANGEBYSCORE delay:queue 0 {currentTime} LIMIT 0 10` - 获取到期任务
- `ZREM delay:queue {taskId}` - 删除任务
- `ZCARD delay:queue` - 获取队列长度
- `ZSCORE delay:queue {taskId}` - 获取任务执行时间

**优势**:
- 精确控制执行时间（毫秒级）
- 支持大量延迟任务
- 天然排序，优先处理到期任务
- 支持任务查询和取消

---

### 1.2 循环任务队列

**功能描述**: 支持任务执行后重新加入队列，实现循环执行。

**应用场景**:
- 定时数据同步
- 定时健康检查
- 定时数据清理
- 定时报表生成

**实现流程**:
1. **任务队列**: `schedule:queue` - SortedSet，分数为下次执行时间
2. **添加任务**: `ZADD schedule:queue {nextExecuteTime} {taskId}` 添加任务
3. **获取到期任务**: `ZRANGEBYSCORE schedule:queue 0 {currentTime} LIMIT 0 10`
4. **执行任务**: 执行任务逻辑
5. **重新调度**: `ZADD schedule:queue {nextExecuteTime} {taskId}` 计算下次执行时间并重新加入

**涉及 Redis 命令**:
- `ZADD schedule:queue {nextExecuteTime} {taskId}` - 添加/更新任务
- `ZRANGEBYSCORE schedule:queue 0 {currentTime} LIMIT 0 10` - 获取到期任务
- `ZREM schedule:queue {taskId}` - 删除任务（停止循环）
- `ZSCORE schedule:queue {taskId}` - 获取下次执行时间

**优势**:
- 支持循环任务
- 灵活调整执行频率
- 支持任务暂停和恢复

---

## 二、时间窗口统计

### 2.1 滑动窗口统计

**功能描述**: 基于 SortedSet 实现滑动窗口数据统计。

**应用场景**:
- 最近7天访问统计
- 最近30天销售额统计
- 最近1小时请求数统计
- 实时数据监控

**实现流程**:
1. **时间窗口**: `stats:window:{metric}` - SortedSet，分数为时间戳
2. **添加数据**: `ZADD stats:window:{metric} {timestamp} {value}` 添加数据点
3. **查询窗口**: `ZRANGEBYSCORE stats:window:{metric} {startTime} {endTime}` 获取时间范围内的数据
4. **清理过期**: `ZREMRANGEBYSCORE stats:window:{metric} 0 {expireTime}` 删除过期数据
5. **统计计算**: 对查询结果进行统计（求和、平均、最大、最小等）

**涉及 Redis 命令**:
- `ZADD stats:window:{metric} {timestamp} {value}` - 添加数据点
- `ZRANGEBYSCORE stats:window:{metric} {startTime} {endTime}` - 查询时间范围
- `ZREMRANGEBYSCORE stats:window:{metric} 0 {expireTime}` - 清理过期数据
- `ZCARD stats:window:{metric}` - 获取数据点数量
- `ZCOUNT stats:window:{metric} {startTime} {endTime}` - 统计时间范围内数量

**优势**:
- 精确的时间窗口控制
- 自动清理过期数据
- 支持复杂统计计算

---

### 2.2 实时排行榜（多时间段）

**功能描述**: 在现有排行榜基础上，扩展更多时间段的实时统计。

**应用场景**:
- 今日/本周/本月/本年排行榜
- 实时热门内容
- 实时趋势分析

**实现流程**:
1. **多时间段排行榜**: 
   - `ranking:today` - 今日榜
   - `ranking:week` - 本周榜
   - `ranking:month` - 本月榜
2. **更新分数**: `ZINCRBY ranking:today {score} {itemId}` 更新今日分数
3. **查询排行榜**: `ZREVRANGE ranking:today 0 9` 获取前10名
4. **定时清理**: 定时 `ZREMRANGEBYRANK` 清理低分数据
5. **时间段切换**: 定时将今日数据合并到本周/本月

**涉及 Redis 命令**:
- `ZINCRBY ranking:today {score} {itemId}` - 更新分数
- `ZREVRANGE ranking:today 0 9` - 获取排行榜
- `ZREMRANGEBYRANK ranking:today 0 -1001` - 只保留前1000名
- `ZUNIONSTORE ranking:week ranking:today ranking:yesterday ...` - 合并多天数据

**优势**:
- 支持多时间段统计
- 实时更新
- 自动清理低分数据

---

## 三、多维度排序

### 3.1 综合排序系统

**功能描述**: 基于多个 SortedSet 实现多维度综合排序。

**应用场景**:
- 商品排序（价格+销量+评分）
- 内容排序（热度+时间+质量）
- 用户排序（活跃度+贡献度+影响力）

**实现流程**:
1. **多维度数据**: 
   - `sort:price` - 价格排序（分数为价格）
   - `sort:sales` - 销量排序（分数为销量）
   - `sort:rating` - 评分排序（分数为评分）
2. **综合排序**: `ZUNIONSTORE sort:composite 3 sort:price sort:sales sort:rating WEIGHTS 0.3 0.4 0.3 AGGREGATE SUM`
3. **查询结果**: `ZREVRANGE sort:composite 0 19` 获取综合排序结果
4. **动态调整**: 根据业务需求调整权重

**涉及 Redis 命令**:
- `ZADD sort:price {price} {itemId}` - 添加价格数据
- `ZADD sort:sales {sales} {itemId}` - 添加销量数据
- `ZUNIONSTORE sort:composite 3 sort:price sort:sales sort:rating WEIGHTS 0.3 0.4 0.3 AGGREGATE SUM` - 综合排序
- `ZREVRANGE sort:composite 0 19` - 获取排序结果
- `ZINTERSTORE sort:intersect 2 sort:price sort:sales AGGREGATE MIN` - 交集排序

**优势**:
- 支持复杂排序规则
- 灵活调整权重
- 支持实时更新

---

### 3.2 条件筛选排序

**功能描述**: 结合 Set 和 SortedSet，实现条件筛选后的排序。

**应用场景**:
- 关注用户的内容排序
- 特定标签的内容排序
- 特定地区的内容排序

**实现流程**:
1. **筛选条件**: `filter:users` - Set 存储符合条件的用户ID
2. **排序数据**: `sort:content` - SortedSet 存储内容排序
3. **内容作者**: Hash 存储 `content:author:{contentId} = {userId}`
4. **筛选排序**:
   - 找出符合条件的用户内容: 遍历 `filter:users`，查询每个用户的内容
   - 或者使用 `ZINTERSTORE` 结合多个条件

**涉及 Redis 命令**:
- `SINTER filter:users filter:tags` - 找出符合条件的集合
- `ZINTERSTORE result:key 2 sort:content filter:users` - 筛选后排序
- `ZUNIONSTORE result:key 2 sort:content filter:users` - 合并后排序
- `ZREVRANGE result:key 0 19` - 获取排序结果

**优势**:
- 支持复杂条件筛选
- 结合 Set 和 SortedSet 优势
- 实时查询结果

---

## 四、范围查询扩展

### 4.1 价格区间查询

**功能描述**: 基于 SortedSet 实现商品价格区间查询。

**应用场景**:
- 商品价格筛选
- 积分范围查询
- 分数段统计

**实现流程**:
1. **价格排序**: `product:price` - SortedSet，分数为价格
2. **区间查询**: `ZRANGEBYSCORE product:price {minPrice} {maxPrice}` 查询价格区间
3. **限制数量**: `ZRANGEBYSCORE product:price {minPrice} {maxPrice} LIMIT {offset} {count}` 分页查询
4. **统计数量**: `ZCOUNT product:price {minPrice} {maxPrice}` 统计区间内数量

**涉及 Redis 命令**:
- `ZADD product:price {price} {productId}` - 添加商品价格
- `ZRANGEBYSCORE product:price {minPrice} {maxPrice}` - 查询价格区间
- `ZRANGEBYSCORE product:price {minPrice} {maxPrice} LIMIT {offset} {count}` - 分页查询
- `ZCOUNT product:price {minPrice} {maxPrice}` - 统计数量
- `ZREVRANGEBYSCORE product:price {maxPrice} {minPrice}` - 倒序查询

**优势**:
- 高效的范围查询
- 支持分页
- 支持统计

---

### 4.2 时间范围查询扩展

**功能描述**: 在现有时间线功能基础上，扩展更多时间范围查询功能。

**应用场景**:
- 查询某时间段的内容
- 查询某时间点的数据
- 时间范围数据统计

**实现流程**:
1. **时间线数据**: `timeline:content` - SortedSet，分数为时间戳
2. **时间范围查询**: `ZRANGEBYSCORE timeline:content {startTime} {endTime}` 查询时间范围
3. **最近N天**: `ZRANGEBYSCORE timeline:content {sevenDaysAgo} {currentTime}` 查询最近7天
4. **时间点查询**: `ZRANGEBYSCORE timeline:content {time} {time}` 查询特定时间点

**涉及 Redis 命令**:
- `ZRANGEBYSCORE timeline:content {startTime} {endTime}` - 时间范围查询
- `ZREVRANGEBYSCORE timeline:content {endTime} {startTime}` - 倒序时间范围查询
- `ZCOUNT timeline:content {startTime} {endTime}` - 统计时间范围内数量
- `ZRANGEBYSCORE timeline:content {startTime} {endTime} LIMIT {offset} {count}` - 分页查询

**优势**:
- 精确的时间范围控制
- 支持多种时间查询
- 高效的时间排序

---

## 五、实时计数器扩展

### 5.1 多维度实时统计

**功能描述**: 基于 SortedSet 实现多维度实时统计和排序。

**应用场景**:
- 实时访问量统计
- 实时点赞数统计
- 实时评论数统计
- 实时销售额统计

**实现流程**:
1. **统计维度**: 
   - `counter:visit:content` - 内容访问量统计
   - `counter:visit:user` - 用户访问量统计
   - `counter:visit:category` - 分类访问量统计
2. **更新计数**: `ZINCRBY counter:visit:content 1 {contentId}` 增加访问量
3. **查询统计**: `ZREVRANGE counter:visit:content 0 9` 获取访问量前10
4. **获取具体值**: `ZSCORE counter:visit:content {contentId}` 获取具体访问量

**涉及 Redis 命令**:
- `ZINCRBY counter:visit:content 1 {contentId}` - 增加计数
- `ZREVRANGE counter:visit:content 0 9` - 获取前10名
- `ZSCORE counter:visit:content {contentId}` - 获取具体值
- `ZRANK counter:visit:content {contentId}` - 获取排名
- `ZREMRANGEBYRANK counter:visit:content 0 -1001` - 只保留前1000名

**优势**:
- 实时更新
- 自动排序
- 支持多维度统计

---

### 5.2 实时趋势分析

**功能描述**: 基于时间序列的 SortedSet，实现实时趋势分析。

**应用场景**:
- 访问量趋势
- 销售额趋势
- 用户增长趋势
- 内容热度趋势

**实现流程**:
1. **时间序列**: `trend:{metric}` - SortedSet，分数为时间戳，值为数据值
2. **添加数据点**: `ZADD trend:visit {timestamp} {value}` 添加时间点数据
3. **查询趋势**: `ZRANGEBYSCORE trend:visit {startTime} {endTime}` 获取时间范围内的数据
4. **计算趋势**: 对查询结果计算增长率、平均值等
5. **清理旧数据**: `ZREMRANGEBYSCORE trend:visit 0 {expireTime}` 清理过期数据

**涉及 Redis 命令**:
- `ZADD trend:{metric} {timestamp} {value}` - 添加数据点
- `ZRANGEBYSCORE trend:{metric} {startTime} {endTime}` - 查询时间范围
- `ZREMRANGEBYSCORE trend:{metric} 0 {expireTime}` - 清理过期数据
- `ZREVRANGE trend:{metric} 0 99` - 获取最近100个数据点

**优势**:
- 支持趋势分析
- 自动时间排序
- 支持历史数据查询

---

## 六、聚合运算扩展

### 6.1 多集合聚合排序

**功能描述**: 在现有 ZUNIONSTORE 基础上，扩展更多聚合运算。

**应用场景**:
- 多用户时间线聚合（已有，可扩展）
- 多标签内容聚合
- 多维度数据聚合

**实现流程**:
1. **多个源集合**: 
   - `sort:source1` - 第一个排序集合
   - `sort:source2` - 第二个排序集合
   - `sort:source3` - 第三个排序集合
2. **聚合方式**:
   - **并集（MAX）**: `ZUNIONSTORE result 3 sort:source1 sort:source2 sort:source3 AGGREGATE MAX` 取最大值
   - **并集（MIN）**: `ZUNIONSTORE result 3 sort:source1 sort:source2 sort:source3 AGGREGATE MIN` 取最小值
   - **并集（SUM）**: `ZUNIONSTORE result 3 sort:source1 sort:source2 sort:source3 AGGREGATE SUM` 求和
   - **交集**: `ZINTERSTORE result 2 sort:source1 sort:source2 AGGREGATE MAX` 交集聚合
3. **权重设置**: `ZUNIONSTORE result 3 sort:source1 sort:source2 sort:source3 WEIGHTS 0.5 0.3 0.2 AGGREGATE SUM` 设置权重

**涉及 Redis 命令**:
- `ZUNIONSTORE result {count} {key1} {key2} ... AGGREGATE MAX` - 并集取最大值
- `ZUNIONSTORE result {count} {key1} {key2} ... AGGREGATE MIN` - 并集取最小值
- `ZUNIONSTORE result {count} {key1} {key2} ... AGGREGATE SUM` - 并集求和
- `ZINTERSTORE result {count} {key1} {key2} ... AGGREGATE MAX` - 交集聚合
- `ZUNIONSTORE result {count} {key1} {key2} ... WEIGHTS {w1} {w2} ... AGGREGATE SUM` - 加权聚合

**优势**:
- 支持多种聚合方式
- 支持权重设置
- 灵活的数据组合

---

### 6.2 增量聚合更新

**功能描述**: 支持增量更新聚合结果，避免全量重新计算。

**应用场景**:
- 实时聚合时间线
- 实时聚合排行榜
- 实时数据汇总

**实现流程**:
1. **源数据更新**: `ZADD source:data {score} {member}` 更新源数据
2. **增量更新聚合**: 
   - 如果 member 已存在: `ZINCRBY aggregate:result {deltaScore} {member}` 增量更新
   - 如果 member 不存在: `ZADD aggregate:result {score} {member}` 添加新成员
3. **定期全量校验**: 定期 `ZUNIONSTORE` 全量重新计算，保证一致性

**涉及 Redis 命令**:
- `ZADD source:data {score} {member}` - 更新源数据
- `ZINCRBY aggregate:result {deltaScore} {member}` - 增量更新聚合
- `ZSCORE aggregate:result {member}` - 检查是否存在
- `ZUNIONSTORE aggregate:result {count} {source1} {source2} ...` - 全量重新计算

**优势**:
- 实时更新聚合结果
- 减少计算开销
- 支持增量更新

---

## 七、地理位置相关（GeoHash）

### 7.1 附近内容查询

**功能描述**: 结合 Redis GeoHash（基于 SortedSet），实现附近内容查询。

**应用场景**:
- 附近的人
- 附近的商家
- 附近的内容
- 地理位置推荐

**实现流程**:
1. **地理位置数据**: 使用 `GEOADD` 添加地理位置（底层是 SortedSet）
2. **附近查询**: `GEORADIUS location:content {longitude} {latitude} {radius} m` 查询附近内容
3. **距离排序**: `GEORADIUS ... WITHCOORD WITHDIST` 返回坐标和距离
4. **距离计算**: `GEODIST location:content {member1} {member2}` 计算两点距离

**涉及 Redis 命令**:
- `GEOADD location:content {longitude} {latitude} {member}` - 添加地理位置
- `GEORADIUS location:content {longitude} {latitude} {radius} m` - 查询附近内容
- `GEODIST location:content {member1} {member2}` - 计算距离
- `GEOPOS location:content {member}` - 获取坐标
- `ZRANGEBYSCORE location:content ...` - 基于 SortedSet 的底层操作

**优势**:
- 高效的地理位置查询
- 支持距离计算
- 支持范围查询

**注意**: GeoHash 功能需要 Redis 3.2+，底层基于 SortedSet 实现。

---

## 八、总结

### 8.1 推荐优先级

**高优先级**（立即实现）:
1. 精确延迟队列 - 解决定时任务需求
2. 时间窗口统计 - 完善数据分析
3. 多维度排序 - 增强排序功能

**中优先级**（后续实现）:
4. 范围查询扩展 - 增强查询功能
5. 实时趋势分析 - 数据可视化
6. 多集合聚合扩展 - 复杂数据聚合

**低优先级**（可选实现）:
7. 循环任务队列 - 定时任务扩展
8. 地理位置查询 - 位置相关功能

### 8.2 实现建议

1. **充分利用分数**: SortedSet 的分数可以表示时间、价格、权重等多种含义
2. **定期清理**: 使用 `ZREMRANGEBYSCORE`、`ZREMRANGEBYRANK` 定期清理过期数据
3. **聚合优化**: 大集合聚合可能较慢，考虑异步处理或增量更新
4. **分页查询**: 使用 `LIMIT` 参数实现分页，避免一次性获取大量数据
5. **Lua 脚本**: 复杂操作使用 Lua 脚本保证原子性

### 8.3 注意事项

1. **内存占用**: SortedSet 存储大量数据会占用较多内存，需要定期清理
2. **聚合性能**: 多个大集合的聚合运算可能较慢，考虑异步处理
3. **分数精度**: 分数是浮点数，注意精度问题（金额等场景使用整数）
4. **原子性**: 关键操作使用 Lua 脚本保证原子性
5. **数据一致性**: 重要数据需要同步到数据库，保证一致性

