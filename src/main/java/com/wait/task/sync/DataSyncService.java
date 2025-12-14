package com.wait.task.sync;

import java.util.List;

/**
 * 数据同步服务接口
 * 定义从数据库同步数据到 Redis 的统一接口
 * 
 * 使用场景：
 * - 数据库作为数据源（Write-Behind模式）
 * - Redis 作为缓存，定时从数据库同步，保证数据最终一致性
 * 
 * 业界常见做法：
 * 1. Write-Behind模式：日常操作只更新Redis，定时任务从数据库同步
 * 2. 统一管理：所有同步任务在一个服务中管理
 * 3. 模板方法模式：提取公共流程，子类实现具体逻辑
 */
public interface DataSyncService<T> {

    /**
     * 获取 Redis Sorted Set 的 key
     * 
     * @return Redis key，如 "post:ranking:likes"
     */
    String getRedisKey();

    /**
     * 从数据库查询所有需要同步的数据
     * 
     * @return 数据列表，每个元素包含资源ID和数量
     */
    List<T> queryDataFromDatabase();

    /**
     * 从数据对象中提取资源ID
     * 
     * @param data 数据对象
     * @return 资源ID（如帖子ID）
     */
    Long extractResourceId(T data);

    /**
     * 从数据对象中提取数量
     * 
     * @param data 数据对象
     * @return 数量（如点赞数、收藏数、评论数）
     */
    Integer extractCount(T data);

    /**
     * 获取任务名称（用于日志）
     * 
     * @return 任务名称
     */
    String getTaskName();

    /**
     * 获取批量处理大小
     * 
     * @return 批量处理大小，默认1000
     */
    default int getBatchSize() {
        return 1000;
    }
}

