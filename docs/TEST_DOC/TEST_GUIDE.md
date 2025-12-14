# Redis功能测试指南

## 概述

本指南介绍如何运行和使用Redis功能测试套件，包括数据加载、功能测试、并发测试和数据一致性验证。

## 测试文件说明

### 1. DataLoadTest.java
**功能**: 将数据库中的所有数据加载到Redis缓存中

**主要方法**:
- `loadAllDataToRedis()`: 加载所有数据到Redis
- `clearRedisTestData()`: 清理Redis测试数据（可选）

**使用场景**:
- 系统初始化时加载历史数据
- 数据迁移后验证数据完整性
- 测试前准备数据

### 2. FunctionTest.java
**功能**: 测试核心功能是否正常工作

**测试方法**:
- `testFollowFunction()`: 测试关注功能
- `testLikeFunction()`: 测试点赞功能
- `testFavoriteFunction()`: 测试收藏功能
- `testBlockFunction()`: 测试黑名单功能
- `testTimelineFunction()`: 测试时间线功能
- `testRankingFunction()`: 测试排行榜功能

**使用场景**:
- 功能回归测试
- 新功能验证
- 问题排查

### 3. ConcurrencyTest.java
**功能**: 模拟高并发场景，测试系统在高负载下的表现

**测试方法**:
- `testConcurrentLikes()`: 并发点赞测试
- `testConcurrentFavorites()`: 并发收藏测试
- `testConcurrentLikeAndUnlike()`: 并发点赞/取消点赞测试
- `testConcurrentFollow()`: 并发关注测试

**测试参数**:
- 并发线程数: 50
- 每线程操作数: 20
- 总操作数: 1000

**使用场景**:
- 性能测试
- 并发安全性验证
- 压力测试

### 4. DataValidationTest.java
**功能**: 验证Redis和数据库数据一致性

**测试方法**:
- `validateAllData()`: 全面验证数据一致性

**验证内容**:
- 关注关系一致性
- 点赞关系一致性
- 收藏关系一致性
- 黑名单关系一致性
- 计数一致性

**使用场景**:
- 数据一致性检查
- 数据迁移验证
- 问题排查

## 快速开始

### 1. 环境准备

确保以下服务正常运行：
- MySQL数据库（端口3306）
- Redis服务（端口6379）
- 应用服务（端口8080）

### 2. 执行测试

#### 方式一：使用Maven命令

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=DataLoadTest
mvn test -Dtest=FunctionTest
mvn test -Dtest=ConcurrencyTest
mvn test -Dtest=DataValidationTest

# 运行特定测试方法
mvn test -Dtest=FunctionTest#testLikeFunction
```

#### 方式二：使用IDE

1. 在IDE中打开测试类
2. 右键点击测试类或测试方法
3. 选择"Run Test"或"Debug Test"

### 3. 测试执行顺序

建议按以下顺序执行测试：

1. **数据加载** (DataLoadTest)
   ```bash
   mvn test -Dtest=DataLoadTest#loadAllDataToRedis
   ```

2. **功能测试** (FunctionTest)
   ```bash
   mvn test -Dtest=FunctionTest
   ```

3. **并发测试** (ConcurrencyTest)
   ```bash
   mvn test -Dtest=ConcurrencyTest
   ```

4. **数据验证** (DataValidationTest)
   ```bash
   mvn test -Dtest=DataValidationTest#validateAllData
   ```

## 详细使用说明

### 数据加载测试

#### 执行数据加载
```bash
mvn test -Dtest=DataLoadTest#loadAllDataToRedis
```

#### 预期输出
```
========== 开始加载数据库数据到Redis ==========
1. 加载用户数据...
✓ 已加载 100 个用户到缓存
2. 加载帖子数据...
✓ 已加载 500 个帖子到缓存
3. 加载关注关系...
✓ 已加载 200 条关注关系到Redis
4. 加载点赞关系...
✓ 已加载 1000 条点赞关系到Redis
5. 加载收藏关系...
✓ 已加载 500 条收藏关系到Redis
6. 加载黑名单关系...
✓ 已加载 50 条黑名单关系到Redis
7. 加载时间线数据...
✓ 已加载 500 条帖子到时间线
8. 加载评论数据...
✓ 已统计 2000 条评论
========== 数据加载完成 ==========
```

#### 注意事项
- 数据加载可能需要较长时间，取决于数据量
- 如果Redis中已有数据，可能会产生重复（但不会影响功能）
- 建议在测试前清理Redis数据

#### 清理测试数据（可选）
```bash
mvn test -Dtest=DataLoadTest#clearRedisTestData
```

### 功能测试

#### 执行所有功能测试
```bash
mvn test -Dtest=FunctionTest
```

#### 执行单个功能测试
```bash
# 测试点赞功能
mvn test -Dtest=FunctionTest#testLikeFunction

# 测试收藏功能
mvn test -Dtest=FunctionTest#testFavoriteFunction
```

#### 测试结果解读
- ✓ 表示测试通过
- ✗ 表示测试失败
- 查看日志了解详细执行过程

### 并发测试

#### 执行并发测试
```bash
mvn test -Dtest=ConcurrencyTest
```

#### 测试结果示例
```
========== 并发点赞测试结果 ==========
总耗时: 1234 ms
成功操作: 800
重复操作: 200
失败操作: 0
最终点赞数: 800
点赞用户数: 800
QPS: 810.37
✓ 并发点赞测试通过，数据一致性验证成功
```

#### 性能指标
- **QPS**: 每秒查询数，应 > 1000
- **成功率**: 应接近100%
- **数据一致性**: 应100%一致

#### 调整测试参数
如需调整并发参数，修改 `ConcurrencyTest.java` 中的常量：
```java
private static final int CONCURRENT_THREADS = 50;  // 并发线程数
private static final int OPERATIONS_PER_THREAD = 20;  // 每线程操作数
```

### 数据一致性验证

#### 执行验证
```bash
mvn test -Dtest=DataValidationTest#validateAllData
```

#### 验证结果示例
```
========== 开始数据一致性验证 ==========
1. 验证关注关系...
✓ 关注关系验证完成，发现 0 处不一致
2. 验证点赞关系...
✓ 点赞关系验证完成，发现 0 处不一致
3. 验证收藏关系...
✓ 收藏关系验证完成，发现 0 处不一致
4. 验证黑名单关系...
✓ 黑名单关系验证完成，发现 0 处不一致
5. 验证计数一致性...
✓ 计数验证完成，发现 0 处不一致
========== 数据一致性验证完成 ==========
✓ 所有数据验证通过，未发现不一致
```

#### 错误处理
如果发现不一致，日志会显示详细信息：
```
用户 123 的关注列表不一致: DB=[456, 789], Redis=[456]
帖子 100 的点赞数不一致: DB=50, Redis=48
```

## 常见问题

### Q1: 测试失败，提示"测试数据不足"
**原因**: 数据库中没有足够的测试数据

**解决方案**:
1. 确保数据库中有用户和帖子数据
2. 至少需要2个用户和1个帖子才能运行完整测试
3. 可以通过数据加载测试先加载数据

### Q2: 并发测试时出现数据不一致
**原因**: 可能是并发冲突或数据同步问题

**解决方案**:
1. 检查Redis连接是否正常
2. 检查Lua脚本是否正确执行
3. 查看日志了解具体错误信息
4. 验证数据库和Redis的数据同步机制

### Q3: 数据验证发现不一致
**原因**: Redis和数据库数据不同步

**解决方案**:
1. 重新运行数据加载测试
2. 检查数据持久化服务是否正常
3. 检查异步任务是否正常执行
4. 查看相关日志了解同步失败原因

### Q4: 测试执行时间过长
**原因**: 数据量大或网络延迟

**解决方案**:
1. 减少测试数据量
2. 优化数据库查询
3. 检查网络连接
4. 调整并发参数

## 最佳实践

### 1. 测试前准备
- 清理Redis测试数据
- 确保数据库中有足够的测试数据
- 检查服务是否正常运行

### 2. 测试执行
- 按顺序执行测试（数据加载 → 功能测试 → 并发测试 → 验证）
- 观察日志输出，及时发现问题
- 记录测试结果和性能指标

### 3. 测试后处理
- 分析测试结果
- 记录发现的问题
- 更新测试报告

### 4. 持续集成
- 将测试集成到CI/CD流程
- 定期执行测试
- 监控测试结果趋势

## 测试报告

测试完成后，请填写 `TEST_REPORT.md` 文档，记录：
- 测试执行日期
- 测试结果汇总
- 发现的问题
- 性能指标
- 优化建议

## 相关文档

- [测试报告模板](./TEST_REPORT.md)
- [Redis命令文档](./REDIS_COMMAND/)
- [数据库批量操作文档](./DATABASE_BATCH_OPERATION/)
- [关系持久化策略文档](./RELATION_PERSISTENCE_STRATEGY.md)

## 技术支持

如遇到问题，请：
1. 查看日志文件
2. 检查配置文件
3. 参考相关文档
4. 联系开发团队

