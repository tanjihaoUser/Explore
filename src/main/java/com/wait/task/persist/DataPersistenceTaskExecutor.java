package com.wait.task.persist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.wait.util.BoundUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 数据持久化任务执行器
 * 统一管理所有数据持久化定时任务
 * 
 * 业界常见做法：
 * 1. 统一管理：所有定时任务在一个类中管理，便于监控和维护
 * 2. 模板方法模式：提取公共流程，各个 DataPersistenceService 实现具体逻辑
 * 3. 策略模式：不同的数据清理策略通过接口实现
 * 
 * 优势：
 * - 统一的任务调度和监控
 * - 统一的错误处理和日志记录
 * - 便于扩展和维护
 * - 避免任务冲突（通过不同的执行时间）
 */
@Slf4j
@Component
public class DataPersistenceTaskExecutor {

    private final BoundUtil boundUtil;
    private final List<DataPersistenceService<?>> persistenceServices;

    @Autowired
    public DataPersistenceTaskExecutor(BoundUtil boundUtil, List<DataPersistenceService<?>> persistenceServices) {
        this.boundUtil = boundUtil;
        this.persistenceServices = persistenceServices;
    }

    private static final int BATCH_SIZE = 1000; // 批量插入大小
    private static final int SCAN_COUNT = 100; // SCAN 每次扫描的数量
    private static final int MAX_ITERATIONS = 100; // 最大循环次数，防止无限循环

    /**
     * 执行数据持久化任务（模板方法）
     * 循环处理直到所有需要清理的数据都被处理完毕
     * 
     * @param service 数据持久化服务
     * @param <T>     数据类型
     */
    private <T> void executePersistenceTask(DataPersistenceService<T> service) {
        String taskName = service.getTaskName();
        log.info("Starting scheduled task: {}", taskName);

        long startTime = System.currentTimeMillis();
        int keepDays = service.getKeepDays();
        long expireTime = System.currentTimeMillis() - (keepDays * 24L * 60 * 60 * 1000);

        int totalPersisted = 0;
        long totalDeleted = 0;
        int iteration = 0;
        String keyPattern = service.getKeyPattern();

        try {
            // 循环处理，直到没有新的 keys 需要处理
            while (iteration < MAX_ITERATIONS) {
                iteration++;
                log.debug("Iteration {}: scanning for keys to persist", iteration);

                // 第一阶段：查找所有需要处理的 Redis keys
                Set<String> allKeys = new HashSet<>();

                // 检查是否有多个 key 模式（通过接口的默认方法）
                List<String> patterns = service.getKeyPatterns();
                if (patterns != null && !patterns.isEmpty()) {
                    // 使用多个模式
                    for (String pattern : patterns) {
                        Set<String> keys = boundUtil.scanKeys(pattern, SCAN_COUNT);
                        if (keys != null) {
                            allKeys.addAll(keys);
                        }
                    }
                } else {
                    // 使用单个模式
                    Set<String> keys = boundUtil.scanKeys(keyPattern, SCAN_COUNT);
                    if (keys != null) {
                        allKeys.addAll(keys);
                    }
                }

                if (allKeys == null || allKeys.isEmpty()) {
                    if (iteration == 1) {
                        log.info("No keys found for pattern: {}, skipping persistence", keyPattern);
                    } else {
                        log.info("No more keys found after {} iterations, persistence completed", iteration - 1);
                    }
                    break; // 没有 keys 需要处理，退出循环
                }

                log.info("Iteration {}: Found {} keys for pattern: {} (using SCAN)", iteration, allKeys.size(),
                        keyPattern);

                // 第二阶段：收集所有需要持久化的数据
                Map<String, List<T>> keyToDataMap = new HashMap<>();
                for (String key : allKeys) {
                    try {
                        // 检查 key 是否过期（如果可以从 key 中解析过期时间）
                        Long keyExpireTime = service.parseExpireTimeFromKey(key);
                        if (keyExpireTime != null && keyExpireTime >= expireTime) {
                            continue; // 未过期，跳过
                        }

                        // 收集需要持久化的数据（如果无法从 key 解析过期时间，会在 collectDataFromRedis 中判断）
                        List<T> dataList = service.collectDataFromRedis(key, expireTime);
                        if (dataList != null && !dataList.isEmpty()) {
                            keyToDataMap.put(key, dataList);
                            log.debug("Collected {} data points for key: {}", dataList.size(), key);
                        }
                    } catch (Exception e) {
                        log.error("Failed to collect data for key: {}", key, e);
                        // 继续处理下一个key，不中断整个任务
                    }
                }

                if (keyToDataMap.isEmpty()) {
                    log.info("Iteration {}: No data to persist, all keys are up-to-date", iteration);
                    break; // 没有数据需要持久化，退出循环
                }

                // 第三阶段：批量插入到数据库
                List<T> allData = new ArrayList<>();
                for (List<T> dataList : keyToDataMap.values()) {
                    allData.addAll(dataList);
                }

                int iterationPersisted = 0;
                if (!allData.isEmpty()) {
                    log.info("Iteration {}: Starting to batch insert {} records to database...", iteration,
                            allData.size());
                    int batchCount = (allData.size() + BATCH_SIZE - 1) / BATCH_SIZE;

                    for (int i = 0; i < batchCount; i++) {
                        int fromIndex = i * BATCH_SIZE;
                        int toIndex = Math.min(fromIndex + BATCH_SIZE, allData.size());
                        List<T> batch = allData.subList(fromIndex, toIndex);

                        try {
                            int inserted = service.batchInsertToDatabase(batch);
                            iterationPersisted += inserted;
                            log.debug("Persisted {} records to database (batch {}/{})",
                                    inserted, i + 1, batchCount);
                        } catch (Exception e) {
                            log.error("Failed to insert batch {}/{} to database", i + 1, batchCount, e);
                            // 继续处理下一批，不中断整个任务
                        }
                    }
                    totalPersisted += iterationPersisted;
                    log.info("Iteration {}: Batch insert completed: {} records persisted (total: {})",
                            iteration, iterationPersisted, totalPersisted);
                }

                // 第四阶段：批量删除 Redis 中的旧数据
                long iterationDeleted = 0;
                for (Map.Entry<String, List<T>> entry : keyToDataMap.entrySet()) {
                    String key = entry.getKey();
                    try {
                        long deletedCount = service.deleteFromRedis(key, expireTime);
                        if (deletedCount > 0) {
                            iterationDeleted += deletedCount;
                            log.debug("Deleted {} old records from Redis for key: {}", deletedCount, key);
                        }
                    } catch (Exception e) {
                        log.error("Failed to delete data from Redis for key: {}", key, e);
                        // 继续处理下一个key，不中断整个任务
                    }
                }

                totalDeleted += iterationDeleted;
                if (iterationDeleted > 0) {
                    log.info("Iteration {}: Deleted {} old records from Redis ({} keys) (total deleted: {})",
                            iteration, iterationDeleted, keyToDataMap.size(), totalDeleted);
                }

                // 如果本次迭代没有处理任何数据，说明已经清理完毕
                if (iterationPersisted == 0 && iterationDeleted == 0) {
                    log.info("Iteration {}: No data processed, persistence completed", iteration);
                    break;
                }

                // 继续下一轮循环，检查是否还有新的 keys 需要处理
            }

            if (iteration >= MAX_ITERATIONS) {
                log.warn("Reached maximum iterations ({}), stopping persistence task", MAX_ITERATIONS);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info(
                    "Scheduled task completed: {} - persisted {} records, deleted {} from Redis, {} iterations, took {}ms",
                    taskName, totalPersisted, totalDeleted, iteration, duration);

        } catch (Exception e) {
            log.error("Error in scheduled task: {}", taskName, e);
        }
    }

    /**
     * 时间窗口统计数据持久化任务
     * 每天凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void persistTimeWindowStatistics() {
        persistenceServices.stream()
                .filter(service -> "Time Window Statistics Persistence".equals(service.getTaskName()))
                .findFirst()
                .ifPresent(this::executePersistenceTask);
    }

    /**
     * 浏览历史数据持久化任务
     * 每天凌晨4点执行
     */
    @Scheduled(cron = "0 0 4 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void persistBrowseHistory() {
        persistenceServices.stream()
                .filter(service -> "Browse History Persistence".equals(service.getTaskName()))
                .findFirst()
                .ifPresent(this::executePersistenceTask);
    }

    /**
     * UV统计数据持久化任务
     * 每天凌晨5点执行
     */
    @Scheduled(cron = "0 0 5 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void persistUVStatistics() {
        persistenceServices.stream()
                .filter(service -> "UV Statistics Persistence".equals(service.getTaskName()))
                .findFirst()
                .ifPresent(this::executePersistenceTask);
    }

}
