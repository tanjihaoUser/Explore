# Redis Set 功能增强建议

## 概述

本文档列出基于当前项目情况，可以为 Redis Set 数据结构增加的功能点。当前项目已使用 Set 存储关注、点赞、收藏、黑名单等关系数据，但功能相对简单，可以进一步扩展。

---

## 一、集合运算功能

### 1.1 共同关注/共同粉丝扩展

**功能描述**: 在现有共同关注功能基础上，扩展更多集合运算功能。

**应用场景**:
- 找出多个用户的共同关注
- 找出多个用户的共同粉丝
- 找出用户A关注但用户B未关注的用户
- 找出用户A和用户B的所有关注（并集）

**实现流程**:
1. **共同关注（交集）**: 使用 `SINTER` 找出多个用户的共同关注
2. **关注并集**: 使用 `SUNION` 找出多个用户的所有关注
3. **关注差集**: 使用 `SDIFF` 找出差异关注
4. **结果存储**: 使用 `SINTERSTORE`、`SUNIONSTORE`、`SDIFFSTORE` 存储结果

**涉及 Redis 命令**:
- `SINTER user:follow:{userId1} user:follow:{userId2} ...` - 多个用户共同关注
- `SUNION user:follow:{userId1} user:follow:{userId2}` - 关注并集
- `SDIFF user:follow:{userId1} user:follow:{userId2}` - 关注差集
- `SINTERSTORE result:key user:follow:{userId1} user:follow:{userId2}` - 存储交集结果
- `SUNIONSTORE result:key user:follow:{userId1} user:follow:{userId2}` - 存储并集结果

**优势**:
- 支持复杂的社交关系分析
- 可以用于推荐系统
- 支持批量用户分析

---

### 1.2 用户相似度计算

**功能描述**: 基于用户标签、兴趣等计算用户相似度。

**应用场景**:
- 用户画像相似度
- 内容推荐
- 好友推荐
- 相似用户发现

**实现流程**:
1. **用户标签**: `user:tag:{userId}` - Set 存储用户标签
2. **计算交集**: `SINTER user:tag:{userId1} user:tag:{userId2}` 找出共同标签
3. **计算并集**: `SUNION user:tag:{userId1} user:tag:{userId2}` 找出所有标签
4. **相似度**: 交集大小 / 并集大小 = 相似度（Jaccard 相似度）

**涉及 Redis 命令**:
- `SINTER user:tag:{userId1} user:tag:{userId2}` - 共同标签
- `SUNION user:tag:{userId1} user:tag:{userId2}` - 所有标签
- `SCARD user:tag:{userId}` - 标签数量
- `SINTERSTORE temp:key user:tag:{userId1} user:tag:{userId2}` - 存储交集用于计算

**优势**:
- 支持用户画像分析
- 可以用于推荐算法
- 支持实时计算

---

### 1.3 内容推荐算法

**功能描述**: 基于用户兴趣和内容标签，推荐相关内容。

**应用场景**:
- 推荐用户可能感兴趣的内容
- 推荐相似内容
- 推荐相关用户

**实现流程**:
1. **用户兴趣**: `user:interest:{userId}` - Set 存储用户兴趣标签
2. **内容标签**: `content:tag:{contentId}` - Set 存储内容标签
3. **标签内容**: `tag:content:{tagId}` - Set 存储包含该标签的内容ID
4. **推荐逻辑**:
   - 找出用户兴趣标签: `SMEMBERS user:interest:{userId}`
   - 找出每个标签的内容: `SMEMBERS tag:content:{tagId}`
   - 合并内容: `SUNION tag:content:{tag1} tag:content:{tag2} ...`
   - 过滤已读: `SDIFF recommended:content user:read:{userId}`

**涉及 Redis 命令**:
- `SMEMBERS user:interest:{userId}` - 获取用户兴趣
- `SMEMBERS tag:content:{tagId}` - 获取标签内容
- `SUNION tag:content:{tag1} tag:content:{tag2}` - 合并推荐内容
- `SDIFF recommended:content user:read:{userId}` - 过滤已读内容
- `SISMEMBER user:read:{userId} {contentId}` - 检查是否已读

**优势**:
- 支持个性化推荐
- 实时计算推荐结果
- 支持多维度推荐

---

## 二、随机推荐功能

### 2.1 随机用户推荐

**功能描述**: 从候选用户中随机推荐，用于发现新用户。

**应用场景**:
- 推荐可能认识的人
- 推荐新用户
- 随机匹配
- 探索功能

**实现流程**:
1. **候选池**: `recommend:candidate:{userId}` - Set 存储候选用户ID
2. **已推荐**: `recommend:shown:{userId}` - Set 存储已推荐用户ID
3. **推荐逻辑**:
   - 过滤已推荐: `SDIFF recommend:candidate:{userId} recommend:shown:{userId}`
   - 随机获取: `SRANDMEMBER filtered:candidates 10` 随机获取10个
   - 记录已推荐: `SADD recommend:shown:{userId} {recommendedUserId}`

**涉及 Redis 命令**:
- `SRANDMEMBER recommend:candidate:{userId} 10` - 随机获取10个候选（不删除）
- `SPOP recommend:candidate:{userId}` - 随机获取并删除
- `SDIFF recommend:candidate:{userId} recommend:shown:{userId}` - 过滤已推荐
- `SADD recommend:shown:{userId} {userId}` - 记录已推荐

**优势**:
- 支持随机推荐
- 避免重复推荐
- 支持探索新内容

---

### 2.2 随机抽奖系统

**功能描述**: 基于 Set 实现随机抽奖功能。

**应用场景**:
- 活动抽奖
- 幸运用户抽取
- 随机奖励发放
- 抽签系统

**实现流程**:
1. **抽奖池**: `lottery:pool:{activityId}` - Set 存储参与者ID
2. **参与抽奖**: `SADD lottery:pool:{activityId} {userId}`
3. **抽取中奖者**: `SPOP lottery:pool:{activityId}` 随机抽取并删除
4. **多次抽取**: 可以连续 `SPOP` 抽取多个中奖者
5. **记录中奖**: `SADD lottery:winners:{activityId} {userId}` 记录中奖者

**涉及 Redis 命令**:
- `SADD lottery:pool:{activityId} {userId}` - 参与抽奖
- `SPOP lottery:pool:{activityId}` - 随机抽取中奖者（删除）
- `SRANDMEMBER lottery:pool:{activityId}` - 随机查看（不删除）
- `SCARD lottery:pool:{activityId}` - 获取参与人数
- `SADD lottery:winners:{activityId} {userId}` - 记录中奖者

**优势**:
- 公平随机
- 支持多次抽取
- 防止重复中奖

---

## 三、去重统计功能

### 3.1 独立访客统计（UV）

**功能描述**: 统计独立访客数量，用于数据分析。

**应用场景**:
- 网站日UV统计
- 文章阅读UV统计
- 活动参与UV统计
- 广告点击UV统计

**实现流程**:
1. **日UV**: `uv:date:{date}` - Set 存储用户ID或IP
2. **添加访问**: `SADD uv:date:{date} {userId}` 记录访问（自动去重）
3. **统计UV**: `SCARD uv:date:{date}` 获取独立访客数
4. **合并统计**: `SUNIONSTORE uv:week:{week} uv:date:{date1} uv:date:{date2} ...` 合并多天数据

**涉及 Redis 命令**:
- `SADD uv:date:{date} {userId}` - 记录访问（自动去重）
- `SCARD uv:date:{date}` - 获取UV数量
- `SUNIONSTORE uv:week:{week} uv:date:{date1} uv:date:{date2}` - 合并多天UV
- `SISMEMBER uv:date:{date} {userId}` - 检查是否已访问

**优势**:
- 自动去重
- 精确统计
- 支持合并统计

**注意**: 如果数据量很大，可以考虑使用 HyperLogLog（`PFADD`）节省内存，但会有小误差。

---

### 3.2 阅读去重统计

**功能描述**: 统计内容的独立阅读用户数，防止重复统计。

**应用场景**:
- 文章阅读数统计
- 视频观看数统计
- 内容访问统计

**实现流程**:
1. **阅读记录**: `content:read:{contentId}` - Set 存储阅读用户ID
2. **记录阅读**: `SADD content:read:{contentId} {userId}` 记录阅读（自动去重）
3. **统计阅读数**: `SCARD content:read:{contentId}` 获取独立阅读数
4. **检查是否已读**: `SISMEMBER content:read:{contentId} {userId}` 检查是否已读

**涉及 Redis 命令**:
- `SADD content:read:{contentId} {userId}` - 记录阅读
- `SCARD content:read:{contentId}` - 获取阅读数
- `SISMEMBER content:read:{contentId} {userId}` - 检查是否已读
- `SMEMBERS content:read:{contentId}` - 获取所有阅读用户

**优势**:
- 自动去重
- 精确统计
- 支持已读状态查询

---

## 四、标签系统扩展

### 4.1 多标签内容查询

**功能描述**: 基于内容标签，支持多标签组合查询。

**应用场景**:
- 同时包含多个标签的内容
- 包含任一标签的内容
- 标签组合推荐
- 标签筛选

**实现流程**:
1. **内容标签**: `content:tag:{contentId}` - Set 存储内容标签
2. **标签内容**: `tag:content:{tagId}` - Set 存储包含该标签的内容ID
3. **多标签交集**: `SINTER tag:content:{tag1} tag:content:{tag2}` 找出同时包含多个标签的内容
4. **多标签并集**: `SUNION tag:content:{tag1} tag:content:{tag2}` 找出包含任一标签的内容
5. **标签差集**: `SDIFF tag:content:{tag1} tag:content:{tag2}` 找出只包含tag1不包含tag2的内容

**涉及 Redis 命令**:
- `SINTER tag:content:{tag1} tag:content:{tag2}` - 多标签交集
- `SUNION tag:content:{tag1} tag:content:{tag2}` - 多标签并集
- `SDIFF tag:content:{tag1} tag:content:{tag2}` - 标签差集
- `SINTERSTORE result:key tag:content:{tag1} tag:content:{tag2}` - 存储交集结果

**优势**:
- 支持复杂标签查询
- 支持标签组合
- 实时查询结果

---

### 4.2 标签推荐

**功能描述**: 基于用户已有标签，推荐相关标签。

**应用场景**:
- 推荐用户可能感兴趣的标签
- 推荐相关内容标签
- 标签扩展

**实现流程**:
1. **用户标签**: `user:tag:{userId}` - Set 存储用户已有标签
2. **标签关联**: `tag:related:{tagId}` - Set 存储相关标签ID
3. **推荐逻辑**:
   - 找出用户所有标签的相关标签: `SUNION tag:related:{tag1} tag:related:{tag2} ...`
   - 过滤已有标签: `SDIFF recommended:tags user:tag:{userId}`
   - 随机推荐: `SRANDMEMBER filtered:tags 10` 随机推荐10个

**涉及 Redis 命令**:
- `SMEMBERS user:tag:{userId}` - 获取用户标签
- `SMEMBERS tag:related:{tagId}` - 获取相关标签
- `SUNION tag:related:{tag1} tag:related:{tag2}` - 合并相关标签
- `SDIFF recommended:tags user:tag:{userId}` - 过滤已有标签
- `SRANDMEMBER filtered:tags 10` - 随机推荐

**优势**:
- 支持标签扩展
- 个性化推荐
- 发现新兴趣

---

## 五、权限和访问控制

### 5.1 角色权限系统

**功能描述**: 基于 Set 实现角色权限管理。

**应用场景**:
- 用户角色管理
- 资源权限控制
- 功能权限控制

**实现流程**:
1. **用户角色**: `user:role:{userId}` - Set 存储用户角色
2. **角色权限**: `role:permission:{roleId}` - Set 存储角色权限
3. **资源权限**: `resource:permission:{resourceId}` - Set 存储资源所需权限
4. **权限检查**:
   - 获取用户角色: `SMEMBERS user:role:{userId}`
   - 获取角色权限: `SUNION role:permission:{role1} role:permission:{role2}`
   - 检查资源权限: `SINTER user:permissions resource:permission:{resourceId}`

**涉及 Redis 命令**:
- `SADD user:role:{userId} {roleId}` - 添加用户角色
- `SMEMBERS user:role:{userId}` - 获取用户角色
- `SMEMBERS role:permission:{roleId}` - 获取角色权限
- `SUNION role:permission:{role1} role:permission:{role2}` - 合并角色权限
- `SINTER user:permissions resource:permission:{resourceId}` - 检查权限

**优势**:
- 灵活的权限管理
- 支持多角色
- 实时权限检查

---

### 5.2 黑白名单扩展

**功能描述**: 在现有黑名单基础上，扩展白名单和批量检查功能。

**应用场景**:
- 内容审核白名单
- API 访问白名单
- 批量用户过滤
- 风控系统

**实现流程**:
1. **黑名单**: `blacklist:{type}` - Set 存储黑名单ID（已有）
2. **白名单**: `whitelist:{type}` - Set 存储白名单ID（新增）
3. **批量检查**: 使用 `SMISMEMBER` 批量检查多个ID
4. **名单合并**: `SUNION blacklist:ip blacklist:user` 合并多个黑名单
5. **名单过滤**: `SDIFF candidate:list blacklist:user` 过滤黑名单

**涉及 Redis 命令**:
- `SADD whitelist:{type} {id}` - 添加白名单
- `SISMEMBER whitelist:{type} {id}` - 检查白名单
- `SMISMEMBER blacklist:{type} {id1} {id2} ...` - 批量检查黑名单（Redis 6.2+）
- `SUNION blacklist:ip blacklist:user` - 合并黑名单
- `SDIFF candidate:list blacklist:user` - 过滤黑名单

**优势**:
- 支持白名单机制
- 支持批量检查（Redis 6.2+）
- 支持名单合并和过滤

---

## 六、数据同步和备份

### 6.1 增量数据同步

**功能描述**: 基于 Set 的差集运算，实现增量数据同步。

**应用场景**:
- 数据库增量同步
- 缓存增量更新
- 数据差异分析

**实现流程**:
1. **源数据**: `source:data` - Set 存储源数据ID
2. **目标数据**: `target:data` - Set 存储目标数据ID
3. **计算差异**: `SDIFF source:data target:data` 找出需要同步的数据
4. **同步数据**: 根据差异ID同步数据
5. **更新目标**: `SADD target:data {syncedId}` 更新目标数据

**涉及 Redis 命令**:
- `SDIFF source:data target:data` - 计算差异
- `SINTER source:data target:data` - 找出共同数据
- `SUNION source:data target:data` - 合并数据
- `SDIFFSTORE sync:diff source:data target:data` - 存储差异结果

**优势**:
- 支持增量同步
- 减少同步数据量
- 支持差异分析

---

## 七、总结

### 7.1 推荐优先级

**高优先级**（立即实现）:
1. 集合运算功能扩展 - 充分利用 Set 的交并差运算
2. 随机用户推荐 - 提升用户体验
3. 独立访客统计 - 完善数据分析

**中优先级**（后续实现）:
4. 多标签内容查询 - 增强内容检索
5. 标签推荐系统 - 个性化推荐
6. 角色权限系统 - 完善权限管理

**低优先级**（可选实现）:
7. 随机抽奖系统 - 活动功能
8. 增量数据同步 - 数据同步优化

### 7.2 实现建议

1. **充分利用集合运算**: Set 的最大优势是集合运算，应该充分利用
2. **结合现有功能**: 在现有关注、点赞等功能基础上扩展
3. **性能优化**: 大集合使用 `SSCAN` 分页，避免 `SMEMBERS` 阻塞
4. **内存管理**: 定期清理过期数据，控制 Set 大小
5. **批量操作**: 使用 Pipeline 优化批量操作性能

### 7.3 注意事项

1. **大集合处理**: 大集合使用 `SSCAN` 游标遍历，避免 `SMEMBERS` 阻塞
2. **集合运算性能**: 多个大集合的运算可能较慢，考虑异步处理
3. **内存占用**: Set 存储大量数据会占用较多内存，需要定期清理
4. **原子性**: 关键操作使用 Lua 脚本保证原子性
5. **数据一致性**: 重要数据需要同步到数据库，保证一致性

