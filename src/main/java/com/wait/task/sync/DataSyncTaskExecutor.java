package com.wait.task.sync;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.wait.util.BoundUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 数据同步任务执行器
 * 统一管理所有从数据库同步到Redis的定时任务
 * 
 * 业界常见做法：
 * 1. 统一管理：所有同步任务在一个类中管理，便于监控和维护
 * 2. 模板方法模式：提取公共流程，各个 DataSyncService 实现具体逻辑
 * 3. 策略模式：不同的同步策略通过接口实现
 * 
 * 优势：
 * - 统一的任务调度和监控
 * - 统一的错误处理和日志记录
 * - 便于扩展和维护
 * - 避免任务冲突（通过不同的执行时间）
 */
@Slf4j
@Component
public class DataSyncTaskExecutor {

    private final BoundUtil boundUtil;
    private final List<DataSyncService<?>> syncServices;

    @Autowired
    public DataSyncTaskExecutor(BoundUtil boundUtil, List<DataSyncService<?>> syncServices) {
        this.boundUtil = boundUtil;
        this.syncServices = syncServices;
    }

    /**
     * 执行数据同步任务（模板方法）
     * 
     * @param service 数据同步服务
     * @param <T>     数据类型
     */
    private <T> void executeSyncTask(DataSyncService<T> service) {
        String taskName = service.getTaskName();
        log.info("Starting scheduled task: {}", taskName);

        long startTime = System.currentTimeMillis();

        try {
            // 1. 从数据库查询所有需要同步的数据
            List<T> dataList = service.queryDataFromDatabase();

            if (dataList == null || dataList.isEmpty()) {
                log.info("No data found to sync, sync completed");
                return;
            }

            log.info("Found {} records to sync from database", dataList.size());

            // 2. 构建 resourceId -> count 的映射
            Map<Long, Integer> countMap = dataList.stream()
                    .filter(data -> service.extractResourceId(data) != null)
                    .collect(Collectors.toMap(
                            service::extractResourceId,
                            service::extractCount,
                            (existing, replacement) -> existing // 如果有重复key，保留第一个
                    ));

            // 3. 批量更新 Redis Sorted Set
            String redisKey = service.getRedisKey();
            int batchSize = service.getBatchSize();
            int updatedCount = 0;

            for (Map.Entry<Long, Integer> entry : countMap.entrySet()) {
                Long resourceId = entry.getKey();
                Integer count = entry.getValue();

                // 使用ZADD更新分数（如果不存在则添加，存在则更新）
                boundUtil.zAdd(redisKey, resourceId, count.doubleValue());
                updatedCount++;

                // 每处理一定数量后记录日志
                if (updatedCount % batchSize == 0) {
                    log.debug("Synced {} records to Redis", updatedCount);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Sync task completed: {} - updated {} records, took {}ms", taskName, updatedCount, duration);

        } catch (Exception e) {
            log.error("Error in sync task: {}", taskName, e);
        }
    }

    /**
     * 点赞数同步任务
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void syncLikeCounts() {
        syncServices.stream()
                .filter(service -> "Like Count Sync".equals(service.getTaskName()))
                .findFirst()
                .ifPresent(this::executeSyncTask);
    }

    /**
     * 收藏数同步任务
     * 每天凌晨2点20分执行
     */
    @Scheduled(cron = "0 20 2 * * ?")
    public void syncFavoriteCounts() {
        syncServices.stream()
                .filter(service -> "Favorite Count Sync".equals(service.getTaskName()))
                .findFirst()
                .ifPresent(this::executeSyncTask);
    }

    /**
     * 评论数同步任务
     * 每天凌晨2点40分执行
     */
    @Scheduled(cron = "0 40 2 * * ?")
    public void syncCommentCounts() {
        syncServices.stream()
                .filter(service -> "Comment Count Sync".equals(service.getTaskName()))
                .findFirst()
                .ifPresent(this::executeSyncTask);
    }
}
