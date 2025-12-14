package com.wait.task.persist;

import java.util.Collections;
import java.util.List;

/**
 * 数据持久化服务接口
 * 定义数据从 Redis 持久化到数据库的统一接口
 * 
 * 使用场景：
 * - Redis 中存储热数据（最近N天）
 * - 数据库存储冷数据（历史数据）
 * - 定时任务将过期数据从 Redis 迁移到数据库
 * 
 * 业界常见做法：
 * 1. 统一管理：所有数据持久化任务在一个服务中管理
 * 2. 模板方法模式：提取公共流程，子类实现具体逻辑
 * 3. 策略模式：不同的数据清理策略
 */
public interface DataPersistenceService<T> {

    /**
     * 获取 Redis key 的前缀模式（用于 SCAN 查找）
     * 
     * @return key 前缀模式，如 "uv:daily:*" 或 "browse:history:user:*"
     */
    String getKeyPattern();

    /**
     * 获取数据保留天数（Redis 中保留最近N天的数据）
     * 
     * @return 保留天数
     */
    int getKeepDays();

    /**
     * 从 Redis key 中解析出过期时间
     * 
     * @param key Redis key
     * @return 过期时间戳（毫秒），如果无法解析返回 null
     */
    Long parseExpireTimeFromKey(String key);

    /**
     * 从 Redis 中收集需要持久化的数据
     * 
     * @param key Redis key
     * @param expireTime 过期时间戳（毫秒）
     * @return 需要持久化的数据列表
     */
    List<T> collectDataFromRedis(String key, long expireTime);

    /**
     * 批量插入数据到数据库
     * 
     * @param dataList 数据列表
     * @return 成功插入的数量
     */
    int batchInsertToDatabase(List<T> dataList);

    /**
     * 从 Redis 中删除已持久化的数据
     * 
     * @param key Redis key
     * @param expireTime 过期时间戳（毫秒）
     * @return 删除的数据数量
     */
    long deleteFromRedis(String key, long expireTime);

    /**
     * 获取任务名称（用于日志）
     * 
     * @return 任务名称
     */
    String getTaskName();

    /**
     * 获取所有需要处理的 key 模式列表（可选方法）
     * 如果返回 null 或空列表，则使用 getKeyPattern() 返回的单个模式
     * 
     * @return key 模式列表，如果为 null 或空列表则使用 getKeyPattern()
     */
    default List<String> getKeyPatterns() {
        return Collections.emptyList(); // 默认返回空列表，使用单个模式
    }
}

