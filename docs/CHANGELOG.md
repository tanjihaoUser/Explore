# Changelog

## 版本更新记录

本文档记录了项目的主要功能更新、修改和新增内容。

---

## 2024年最新更新

### 🎉 新增功能

#### 1. 浏览历史服务 (BrowseHistoryService)
- **功能描述**: 基于 Redis Sorted Set 实现用户浏览历史记录功能
- **主要特性**:
  - 自动去重：同一用户浏览同一帖子时，自动更新浏览时间
  - 时间范围查询：支持按时间范围查询浏览记录
  - 数据持久化：定期将浏览记录同步到数据库
  - 容量限制：默认最多保留1000条记录，自动清理旧数据
- **Redis 数据结构**: Sorted Set (ZADD, ZREVRANGE, ZRANGEBYSCORE)
- **相关文件**:
  - `BrowseHistoryServiceImpl.java`
  - `BrowseHistoryController.java`
  - `BrowseHistoryMapper.java`
  - `frontend/src/views/BrowseHistoryView.vue`

#### 2. 通知系统 (NotificationService)
- **功能描述**: 基于 Redis List 和 Set 实现完整的用户通知系统
- **主要特性**:
  - 通知发送：支持点赞、评论、关注等多种通知类型
  - 未读标记：使用 Redis Set 快速查询未读通知
  - 分页查询：支持分页获取通知列表
  - 批量操作：支持批量标记已读、删除通知
  - 原子操作：使用 Lua 脚本确保操作原子性
- **Redis 数据结构**: 
  - List (通知列表)
  - Set (未读通知集合)
- **相关文件**:
  - `NotificationServiceImpl.java`
  - `NotificationController.java`
  - `NotificationScripts.java` (Lua 脚本)
  - `frontend/src/api/notification.js`

#### 3. 用户推荐服务 (UserRecommendationService)
- **功能描述**: 基于 Redis Set 实现随机用户推荐功能
- **主要特性**:
  - 候选用户管理：维护候选推荐用户集合
  - 推荐去重：避免重复推荐相同用户
  - 随机推荐：从候选用户中随机推荐
  - 推荐标记：记录已推荐用户，避免重复
- **Redis 数据结构**: Set (SADD, SRANDMEMBER, SPOP, SDIFF)
- **相关文件**:
  - `UserRecommendationServiceImpl.java`
  - `RecommendationController.java`
  - `RecommendationScripts.java` (Lua 脚本)
  - `frontend/src/api/recommendation.js`
  - `frontend/src/views/DiscoverView.vue`

#### 4. 统计服务 (StatisticsService)
- **功能描述**: 基于时间窗口统计服务实现业务数据统计
- **主要特性**:
  - 浏览量统计：帖子浏览量、首页浏览量统计
  - 互动统计：点赞数、收藏数统计
  - 时间窗口统计：支持按小时、按天统计
  - 日期范围查询：支持指定日期范围统计
- **Redis 数据结构**: Sorted Set (时间窗口统计)
- **相关文件**:
  - `StatisticsServiceImpl.java`
  - `StatisticsController.java`
  - `frontend/src/api/statistics.js`
  - `frontend/src/views/StatisticsView.vue`

#### 5. UV 统计服务 (UVStatisticsService)
- **功能描述**: 基于 Redis HyperLogLog 实现独立访客(UV)统计
- **主要特性**:
  - 高效统计：使用 HyperLogLog 算法，内存占用极小
  - 去重统计：自动去重，准确统计独立访客数
  - 多维度统计：支持按资源类型、资源ID统计
  - 时间窗口统计：支持按小时、按天统计
- **Redis 数据结构**: HyperLogLog (PFADD, PFCOUNT)
- **相关文件**:
  - `UVStatisticsServiceImpl.java`
  - `UVStatisticsController.java`
  - `frontend/src/api/uvStatistics.js`
  - `frontend/src/views/UVStatisticsView.vue`

#### 6. 时间窗口统计服务 (TimeWindowStatisticsService)
- **功能描述**: 基于 Redis Sorted Set 实现时间窗口数据统计
- **主要特性**:
  - 时间窗口管理：支持按小时、按天等时间窗口统计
  - 数据点记录：记录每个时间点的数据值
  - 自动清理：自动清理过期数据，节省内存
  - 聚合统计：支持求和、计数等聚合操作
- **Redis 数据结构**: Sorted Set (ZADD, ZRANGEBYSCORE, ZREMRANGEBYSCORE)
- **相关文件**:
  - `TimeWindowStatisticsServiceImpl.java`
  - `TimeWindowStatisticsMapper.java`

#### 7. 延迟队列服务 (DelayQueueService)
- **功能描述**: 基于 Redis Sorted Set 实现精确的延迟任务队列
- **主要特性**:
  - 精确延迟：支持精确到秒的延迟任务执行
  - 任务调度：自动轮询并执行到期任务
  - 任务处理器：支持注册自定义任务处理器
  - 批量处理：支持批量获取和执行任务
- **Redis 数据结构**: Sorted Set (ZADD, ZRANGEBYSCORE, ZREM)
- **相关文件**:
  - `DelayQueueServiceImpl.java`
  - `DelayTaskHandler.java` (接口)
  - `DelayTaskExampleService.java` (示例实现)

#### 8. Redis 消息队列服务 (RedisMQService)
- **功能描述**: 基于 Redis List 实现消息队列功能
- **主要特性**:
  - 多主题队列：支持多个主题的消息队列
  - 消息持久化：消息存储在 Redis 中，支持持久化
  - 死信队列：处理失败的消息进入死信队列
  - 异步消费：支持异步消息消费
- **Redis 数据结构**: List (LPUSH, RPOP, BRPOP)
- **相关文件**:
  - `RedisMQServiceImpl.java`
  - `RedisListQueueService.java`
  - `MessageHandler.java` (接口)

#### 9. 多维度排序服务 (MultiDimensionSortService)
- **功能描述**: 基于 Redis Sorted Set 实现多维度排序功能
- **主要特性**:
  - 多维度排序：支持按多个维度（如时间、热度、评分）排序
  - 权重配置：支持为不同维度设置权重
  - 动态更新：支持动态更新排序分数
- **Redis 数据结构**: Sorted Set (ZADD, ZREVRANGE)
- **相关文件**:
  - `MultiDimensionSortServiceImpl.java`

#### 10. 关系数据验证服务 (RelationDataValidationService)
- **功能描述**: 验证 Redis 和数据库中的关系数据一致性
- **主要特性**:
  - 数据一致性验证：验证关注、点赞、收藏等关系数据
  - 差异检测：检测 Redis 和数据库之间的数据差异
  - 修复建议：提供数据修复建议
- **相关文件**:
  - `RelationDataValidationServiceImpl.java`
  - `ValidationResult.java` (DTO)

### 🔧 功能优化

#### 1. 前端功能增强
- **新增视图页面**:
  - `BrowseHistoryView.vue`: 浏览历史查看页面
  - `DiscoverView.vue`: 发现/推荐页面
  - `StatisticsView.vue`: 统计数据查看页面
  - `UVStatisticsView.vue`: UV统计查看页面
- **API 接口更新**:
  - `browseHistory.js`: 浏览历史相关接口
  - `notification.js`: 通知相关接口
  - `recommendation.js`: 推荐相关接口
  - `statistics.js`: 统计相关接口
  - `uvStatistics.js`: UV统计相关接口
- **组件优化**:
  - `MyAside.vue`: 侧边栏组件更新
  - `MyHeader.vue`: 头部组件更新
  - `HomeSimple.vue`: 首页优化
  - `PostDetail.vue`: 帖子详情页优化
- **路由配置**:
  - `router/index.js`: 新增路由配置

#### 2. 后端服务优化
- **Mapper 优化**: 
  - 批量操作优化：优化了批量插入、批量查询的性能
  - 存在性检查优化：批量查询替代逐条查询，性能提升 10-50 倍
- **服务层优化**:
  - `RelationService`: 关系服务优化
  - `CommentService`: 评论服务优化
  - `RankingService`: 排行榜服务优化
  - `PostService`: 帖子服务优化
  - `UserService`: 用户服务优化
- **配置优化**:
  - `TaskExecutorConfig.java`: 任务执行器配置优化
  - `application.yml`: 应用配置更新

#### 3. 数据持久化优化
- **批量持久化策略**: 
  - 定时 + 定量双重触发机制
  - 批量写入优化，减少数据库交互次数
  - 去重优化，避免无效写入

### 📚 文档更新

#### 新增文档
- **功能实现文档** (`docs/ENHANCE_FUNCTION/`):
  - `NOTIFICATION_IMPLEMENTATION.md`: 通知系统实现说明
  - `BROWSE_HISTORY_IMPLEMENTATION.md`: 浏览历史实现说明
  - `USER_RECOMMENDATION_IMPLEMENTATION.md`: 用户推荐实现说明
  - `STATISTICS_IMPLEMENTATION.md`: 统计服务实现说明
  - `UV_STATISTICS_IMPLEMENTATION.md`: UV统计实现说明
  - `TIME_WINDOW_STATISTICS_IMPLEMENTATION.md`: 时间窗口统计实现说明
  - `DELAY_QUEUE_IMPLEMENTATION.md`: 延迟队列实现说明
  - `REDIS_MQ_IMPLEMENTATION.md`: Redis消息队列实现说明
  - `MULTI_DIMENSION_SORT_IMPLEMENTATION.md`: 多维度排序实现说明
  - 以及其他相关实现文档

- **Redis 命令文档** (`docs/REDIS_COMMAND/`):
  - `REDIS_COMMON_COMMANDS_BEST_PRACTICES.md`: Redis 常用命令最佳实践

- **数据库批量操作文档** (`docs/DATABASE_BATCH_OPERATION/`):
  - `14-RELATION_DATA_VALIDATION_STRATEGY.md`: 关系数据验证策略

- **项目文档**:
  - `PROJECT_EVOLUTION_AND_ROADMAP.md`: 项目演进和路线图
  - `prompt.md`: 开发提示文档

#### 删除文档
- `BCRYPT_PASSWORD_EXPLANATION.md`: BCrypt 密码说明（已整合）
- `RELATION_BATCH_PERSISTENCE.md`: 关系批量持久化（已整合）
- `RELATION_PERSISTENCE_STRATEGY.md`: 关系持久化策略（已整合）
- `TEST_GUIDE.md`: 测试指南（已整合）
- `TEST_REPORT.md`: 测试报告（已整合）
- `TEST_RESULT_REPORT.md`: 测试结果报告（已整合）
- 其他旧版本文档

### 🗑️ 代码清理

#### 删除的文件
- `CorsConfig.java`: CORS 配置（已整合到其他配置）
- `MemoryMQServiceImpl.java`: 内存消息队列实现（已替换为 Redis 实现）

### 🔨 配置更新

#### .gitignore 优化
- 更完善的 IDE 配置忽略规则
- 更完善的日志文件忽略规则
- 新增系统文件忽略规则（.DS_Store 等）
- 新增临时文件忽略规则

#### 前端配置
- `frontend/.gitignore`: 新增前端 .gitignore 文件
- `frontend/package.json`: 依赖更新

### 🧪 测试代码

#### 新增测试
- `BrowseHistoryServiceTest.java`: 浏览历史服务测试
- `RelationDataValidationServiceTest.java`: 关系数据验证测试
- `ScheduledTaskTest.java`: 定时任务测试
- `TimeWindowStatisticsServiceTest.java`: 时间窗口统计测试
- `UVStatisticsServiceTest.java`: UV统计测试

#### 测试优化
- `DataLoadTest.java`: 数据加载测试优化
- `RTForString.java`: Redis 字符串测试优化

### 📦 新增实体和 DTO

#### 实体类
- `BrowseHistory.java`: 浏览历史实体
- `TimeWindowStatistics.java`: 时间窗口统计实体
- `UVStatistics.java`: UV统计实体

#### DTO 类
- `MQMessage.java`: 消息队列消息 DTO
- `NotificationDTO.java`: 通知 DTO
- `ValidationResult.java`: 验证结果 DTO
- `NotificationRequest.java`: 通知请求参数

#### 类型枚举
- `ResourceType.java`: 资源类型枚举

### 🔄 其他更新

#### Lua 脚本
- `lua/browse_history/`: 浏览历史相关 Lua 脚本
- `lua/notification/`: 通知相关 Lua 脚本
- `lua/recommendation/`: 推荐相关 Lua 脚本

#### Mapper XML
- `BrowseHistoryMapper.xml`: 浏览历史 Mapper
- `TimeWindowStatisticsMapper.xml`: 时间窗口统计 Mapper
- `UVStatisticsMapper.xml`: UV统计 Mapper

#### SQL 脚本
- `sql/`: 新增 SQL 脚本目录

---

## 技术栈总结

### Redis 数据结构使用情况
- **String**: 缓存、计数器
- **Hash**: 对象缓存
- **List**: 消息队列、通知列表
- **Set**: 关系数据、推荐候选、未读通知
- **Sorted Set**: 排行榜、时间线、浏览历史、延迟队列、时间窗口统计
- **HyperLogLog**: UV 统计

### 核心特性
- ✅ 原子操作：使用 Lua 脚本确保操作原子性
- ✅ 数据持久化：定期同步到数据库
- ✅ 批量操作：优化数据库交互性能
- ✅ 异步处理：使用线程池异步处理任务
- ✅ 延迟队列：支持精确延迟任务
- ✅ 消息队列：基于 Redis 的轻量级消息队列

---

## 后续计划

1. **性能优化**:
   - 进一步优化批量操作性能
   - 优化 Redis 内存使用
   - 增加缓存预热机制

2. **功能扩展**:
   - 完善通知推送功能（WebSocket）
   - 增强推荐算法（协同过滤）
   - 增加更多统计维度

3. **监控和运维**:
   - 增加 Redis 监控指标
   - 增加性能监控
   - 完善日志系统

---

*最后更新: 2024年*
