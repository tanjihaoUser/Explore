## 2025-11-29 核心功能扩展与前端集成

### 一、新增核心业务功能

#### 1. 评论系统（Comment System）
- **新增 `CommentController`：**
  - 实现完整的评论管理功能：
    - 创建评论、获取评论列表、获取评论详情
    - 删除评论、更新评论
    - 支持分页查询和按帖子ID查询
  - 所有接口统一使用 `/api/comments` 路径前缀

- **新增 `CommentService` 及相关实现：**
  - `CommentServiceImpl`：实现评论的增删改查核心业务逻辑
  - 支持评论计数统计和缓存优化
  - 与帖子服务集成，自动更新帖子评论数

- **新增评论数据实体和 Mapper：**
  - `Comment`：评论实体，包含帖子ID、用户ID、内容、创建时间等字段
  - `CommentMapper`：评论数据访问层，支持批量查询和统计

#### 2. 排行榜系统（Ranking System）
- **新增 `RankingController`：**
  - 实现多种排行榜功能：
    - 热门排行榜（按热度分数排序）
    - 最新排行榜（按发布时间排序）
    - 支持分页查询和自定义排序规则
  - 所有接口统一使用 `/api/ranking` 路径前缀

- **新增排行榜服务：**
  - `RankingService` / `RankingServiceImpl`：基础排行榜服务
  - `HotRankingService` / `HotRankingServiceImpl`：热度排行榜服务
  - 使用 Redis Sorted Set 实现高性能排行榜
  - 支持点赞、收藏等操作自动更新排行榜分数

- **排行榜集成：**
  - 在 `RelationService` 中集成排行榜更新逻辑
  - 点赞/取消点赞、收藏/取消收藏时自动更新排行榜分数
  - 使用异步更新机制，不影响主流程性能

#### 3. 用户管理系统（User Management）
- **新增 `UserController`：**
  - 实现用户管理功能：
    - 用户注册、登录、获取用户信息
    - 用户资料更新、用户列表查询
    - 支持用户验证和密码管理
  - 所有接口统一使用 `/api/users` 路径前缀

- **新增 `UserService` 及相关实现：**
  - `UserServiceImpl`：实现用户管理的核心业务逻辑
  - 支持用户注册、登录验证、密码加密（BCrypt）
  - 集成用户会话管理

- **用户实体增强：**
  - `UserBase` 实体新增密码相关字段（`passwordHash`、`salt`、`passwordUpdateTime`）
  - `UserBaseMapper` 新增用户注册、验证相关方法
  - 支持用户名和邮箱唯一性检查

#### 4. 时间线服务增强（Timeline Service）
- **新增 `TimelineSortedSetService`：**
  - 使用 Redis Sorted Set 实现高性能时间线功能
  - 支持用户时间线、全局时间线、关注用户时间线
  - 实现时间线合并算法，支持多源时间线聚合
  - 自动限制时间线大小，防止内存无限增长

- **时间线 Lua 脚本：**
  - `publish_to_timeline.lua`：发布帖子到时间线（原子操作）
  - `remove_from_timeline.lua`：从时间线移除帖子（原子操作）
  - 使用 Sorted Set 的分数（时间戳）实现按时间排序

- **`TimeLineScripts` 配置更新：**
  - 新增时间线相关脚本的注册和管理
  - 统一管理所有时间线操作的 Lua 脚本

#### 5. 黑名单功能完善（User Block）
- **新增 `UserBlock` 实体和 Mapper：**
  - `UserBlock`：用户黑名单实体
  - `UserBlockMapper`：黑名单数据访问层
  - 支持黑名单的持久化存储

- **`RelationPersistenceService` 完善：**
  - 实现黑名单的数据库持久化逻辑
  - 支持批量写入和存在性检查
  - 避免重复插入和无效删除

### 二、API 路径统一化

- **所有 Controller 统一使用 `/api` 前缀：**
  - `/api/posts` - 帖子相关接口
  - `/api/comments` - 评论相关接口
  - `/api/relation` - 关系相关接口（关注、点赞、收藏、黑名单）
  - `/api/sessions` - 会话相关接口
  - `/api/users` - 用户相关接口
  - `/api/ranking` - 排行榜相关接口
  - `/api/redis` - Redis 测试接口
  - `/api/typeCache` - 类型缓存测试接口

### 三、服务层优化

#### 1. PostService 增强
- **新增方法：**
  - `getPostById`：根据ID获取单个帖子
  - `getPostsByIdsWithRelation`：批量获取帖子并填充关系数据（点赞、收藏状态等）
  - 支持在查询帖子列表时自动填充用户关系数据

- **PostController 优化：**
  - 新增 `GET /api/posts/{postId}` 接口
  - `GET /api/posts/user/{userId}` 接口支持 `currentUserId` 参数，自动填充关系数据
  - 优化帖子查询性能，减少 N+1 查询问题

#### 2. RelationService 增强
- **新增方法：**
  - `batchCheckFavorited`：批量检查收藏状态
  - 集成排行榜更新逻辑，点赞/收藏操作自动更新排行榜

- **排行榜集成：**
  - 点赞/取消点赞时调用 `rankingService` 和 `hotRankingService` 更新分数
  - 收藏/取消收藏时调用 `rankingService` 和 `hotRankingService` 更新分数
  - 使用异常捕获机制，排行榜更新失败不影响主流程

#### 3. SessionService 增强
- **用户验证集成：**
  - `createSession` 方法新增用户凭据验证
  - 调用 `UserService.validateUser` 验证用户名和密码
  - 验证成功后使用实际用户ID创建会话
  - 增强安全性，防止未授权登录

### 四、工具类增强

#### 1. BoundUtil 新增 Sorted Set 方法
- **新增方法：**
  - `zRevRank`：获取成员在 Sorted Set 中的排名（降序）
  - `zRangeByScore`：按分数范围查询成员
  - `zUnionAndStore`：合并多个 Sorted Set 并存储到目标key
  - 支持聚合方式配置（MAX、MIN、SUM）

- **Set 操作优化：**
  - `sMembers`、`zRange`、`zRevRange` 等方法优化返回类型
  - 使用 `HashSet` 和 `LinkedHashSet` 替代 `ConcurrentHashMap`，提升性能
  - 保持 Redis 返回的顺序（Sorted Set 使用 `LinkedHashSet`）

#### 2. ResponseUtil 增强
- **新增错误处理方法：**
  - `error`：通用错误响应（支持状态码、消息、数据）
  - `internalError`：内部服务器错误（500）
  - `notFound`：资源未找到（404）
  - `unauthorized`：未授权（401）
  - `forbidden`：禁止访问（403）
  - `badRequest`：参数错误（400，支持验证错误详情）
  - 统一错误响应格式，提升 API 一致性

### 五、依赖和配置更新

#### 1. 新增依赖
- **Spring Security：**
  - 添加 `spring-boot-starter-security` 依赖
  - 支持用户认证和授权功能

- **Spring Boot Actuator：**
  - 添加 `spring-boot-starter-actuator` 依赖
  - 支持健康检查、指标监控等功能

#### 2. 配置更新
- **application.yml 更新：**
  - 新增服务器端口配置（8080）
  - 新增 Actuator 配置（健康检查、指标端点）
  - 更新 Druid 监控配置（用户名、密码、IP限制）
  - 更新日志配置（Spring Security、CORS、自定义包日志级别）

- **新增配置类：**
  - `CorsConfig`：跨域资源共享配置
  - `SecurityConfig`：Spring Security 安全配置

### 六、MyBatis Mapper 优化

#### 1. PostMapper 增强
- **新增查询方法：**
  - `selectUserIdsByIds`：批量查询帖子的用户ID映射（性能优化）
  - `selectUserStatistics`：统计用户的所有帖子数据（帖子数、点赞总数、评论总数）

- **查询优化：**
  - 所有查询统一使用 `BaseResultMap`，避免字段映射问题
  - 移除不必要的 `LEFT JOIN user_base`，减少查询复杂度
  - 优化排序逻辑（按 `updated_at` 和 `id` 降序）

#### 2. UserBaseMapper 增强
- **新增方法：**
  - `insert`：插入新用户
  - `countByUsername`：检查用户名是否存在
  - `countByEmail`：检查邮箱是否存在
  - `selectAll`：查询所有用户

- **字段映射更新：**
  - 新增 `password_hash`、`salt`、`password_update_time` 字段映射
  - `updateById` 方法支持动态更新（使用 `<set>` 标签）

#### 3. PostLikeMapper 和 PostFavoriteMapper
- **新增方法：**
  - `selectAll`：查询所有点赞/收藏记录（用于数据同步）

### 七、文档更新

#### 1. Redis 命令文档增强
- **所有 Redis 命令文档新增使用频率说明：**
  - `REDIS_STRING_COMMANDS.md`：String 命令使用频率标注
  - `REDIS_HASH_COMMANDS.md`：Hash 命令使用频率标注
  - `REDIS_LIST_COMMANDS.md`：List 命令使用频率标注
  - `REDIS_SET_COMMANDS.md`：Set 命令使用频率标注
  - `REDIS_SORTED_SET_COMMANDS.md`：新增 Sorted Set 命令文档

- **使用频率标记：**
  - ⭐⭐⭐ 极高频率：几乎每个使用场景都会使用
  - ⭐⭐ 高频：大多数场景会使用
  - ⭐ 中频：特定场景使用
  - 无标记：低频或特殊场景使用

#### 2. 新增文档
- `BCRYPT_PASSWORD_EXPLANATION.md`：BCrypt 密码加密说明文档
- `TEST_GUIDE.md`：测试指南文档
- `TEST_REPORT.md`：测试报告文档
- `TEST_RESULT_REPORT.md`：测试结果报告文档
- `前端组件命名规范.md`：前端组件命名规范文档

### 八、前端集成

- **新增完整前端项目：**
  - 基于 Vue.js 的前端应用
  - 包含用户登录、注册、帖子列表、评论等核心功能
  - 支持响应式设计和现代化 UI

- **前端功能模块：**
  - 用户认证（登录、注册）
  - 帖子管理（发布、查看、点赞、收藏）
  - 评论系统（查看、发布评论）
  - 用户资料（查看、编辑）
  - 排行榜展示
  - 时间线展示

### 九、测试代码

- **新增测试类：**
  - `BCryptVerificationTest`：BCrypt 密码验证测试
  - `ConcurrencyTest`：并发测试
  - `DataLoadTest`：数据加载测试
  - `DataValidationTest`：数据验证测试
  - `FunctionTest`：功能测试
  - `PasswordTest`：密码测试

### 十、其他优化

#### 1. 代码质量提升
- 统一代码格式和风格
- 优化异常处理机制
- 改进日志记录（使用 `@Slf4j`）
- 增强方法注释和文档

#### 2. 性能优化
- 批量查询优化（减少数据库查询次数）
- 缓存策略优化（关系数据缓存）
- 异步更新机制（排行榜更新不影响主流程）

#### 3. 安全性增强
- 用户密码使用 BCrypt 加密
- 会话管理增强（用户验证）
- Spring Security 集成（准备就绪）

### 总结：主要改进点

1. **核心业务功能完善：** 新增评论系统、排行榜系统、用户管理系统，形成完整的社交媒体应用核心功能。

2. **时间线服务增强：** 使用 Redis Sorted Set 实现高性能时间线功能，支持多源时间线合并。

3. **API 统一化：** 所有接口统一使用 `/api` 前缀，提升 API 规范性和可维护性。

4. **服务层优化：** 增强现有服务功能，支持关系数据填充、排行榜集成、用户验证等。

5. **工具类增强：** BoundUtil 新增 Sorted Set 相关方法，ResponseUtil 新增错误处理方法。

6. **依赖和配置更新：** 集成 Spring Security 和 Actuator，支持安全认证和监控功能。

7. **文档完善：** Redis 命令文档增加使用频率说明，新增多个技术文档。

8. **前端集成：** 新增完整的 Vue.js 前端应用，实现前后端分离架构。

9. **测试覆盖：** 新增多个测试类，提升代码质量和可靠性。

10. **代码质量提升：** 统一代码风格，优化异常处理，增强安全性。

