package com.wait.sync.write;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 关系数据写回策略 - 定时定量混合方案
 * 
 * 适用于点赞、收藏等高频操作的关系数据持久化
 * 
 * 策略特点：
 * 1. 定时批量写入：每N秒/分钟批量写入一次
 * 2. 定量批量写入：当缓冲达到M条时立即写入
 * 3. 混合触发：定时 + 定量双重触发，兼顾性能和实时性
 * 4. 去重合并：同一key的多次操作只保留最新状态
 * 
 * 复用IncrementalWriteStrategy的核心思路，但针对关系数据（Set操作）进行优化
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RelationWriteBehindStrategy {

    @Qualifier("refreshScheduler")
    private final ThreadPoolTaskScheduler taskScheduler;

    /** 定时批量写入延迟时间：默认30秒 */
    private static final long DEFAULT_FLUSH_DELAY_MS = TimeUnit.SECONDS.toMillis(30);

    /** 定量批量写入阈值：默认100条 */
    private static final int DEFAULT_BATCH_SIZE_THRESHOLD = 100;

    /**
     * 关系数据批量任务
     * 
     * @param <K> 操作key类型（如 "postId:userId"）
     * @param <V> 操作值类型（如 Boolean表示点赞/取消点赞）
     */
    @Data
    public static class RelationBatchTask<K, V> {
        /** 操作缓冲：key = 操作标识, value = 操作值 */
        private final Map<K, V> operations = new ConcurrentHashMap<>();

        /** 任务创建时间 */
        private long createTime = System.currentTimeMillis();

        /** 最后一次更新时间 */
        private long lastUpdateTime = System.currentTimeMillis();

        /**
         * 添加操作（去重：同一key的多次操作只保留最新状态）
         */
        public void addOperation(K key, V value) {
            operations.put(key, value);
            lastUpdateTime = System.currentTimeMillis();
        }

        /**
         * 获取操作数量
         */
        public int getOperationCount() {
            return operations.size();
        }

        /**
         * 检查是否有待处理的操作
         */
        public boolean hasPendingOperations() {
            return !operations.isEmpty();
        }

        /**
         * 清空所有操作
         */
        public void clear() {
            operations.clear();
        }

        /**
         * 创建快照（用于批量写入时避免并发修改）
         */
        public Map<K, V> snapshot() {
            return new ConcurrentHashMap<>(operations);
        }
    }

    /**
     * 批量任务管理器
     */
    @Data
    public static class BatchTaskManager<K, V> {
        /** 批量任务缓冲 */
        private final RelationBatchTask<K, V> batchTask = new RelationBatchTask<>();

        /** 定时刷库任务 */
        private volatile ScheduledFuture<?> scheduledFlushTask;

        /** 定时批量写入延迟时间 */
        private final long flushDelayMs;

        /** 定量批量写入阈值 */
        private final int batchSizeThreshold;

        /** 批量写入回调 */
        private final Consumer<Map<K, V>> flushCallback;

        /** 任务调度器 */
        private final ThreadPoolTaskScheduler taskScheduler;

        public BatchTaskManager(long flushDelayMs, int batchSizeThreshold,
                Consumer<Map<K, V>> flushCallback, ThreadPoolTaskScheduler taskScheduler) {
            this.flushDelayMs = flushDelayMs;
            this.batchSizeThreshold = batchSizeThreshold;
            this.flushCallback = flushCallback;
            this.taskScheduler = taskScheduler;
        }

        /**
         * 添加操作并检查是否需要立即刷库
         */
        public void addOperation(K key, V value) {
            batchTask.addOperation(key, value);

            // 检查是否达到定量阈值
            if (batchTask.getOperationCount() >= batchSizeThreshold) {
                log.debug("Batch size threshold reached ({}), triggering immediate flush",
                        batchTask.getOperationCount());
                flushBatchToDatabase();
            } else {
                // 未达到阈值，确保定时任务已启动
                scheduleBatchFlushTask();
            }
        }

        /**
         * 启动定时批量刷库任务
         */
        private void scheduleBatchFlushTask() {
            // 如果任务已存在且未完成，不重置定时器，保持固定刷新周期
            if (scheduledFlushTask != null && !scheduledFlushTask.isDone()
                    && !scheduledFlushTask.isCancelled()) {
                return;
            }

            // 任务不存在或已取消/完成，创建新任务
            scheduledFlushTask = taskScheduler.schedule(
                    this::flushBatchToDatabase,
                    new Date(System.currentTimeMillis() + flushDelayMs));

            log.debug("Scheduled batch flush task, delay: {}ms", flushDelayMs);
        }

        /**
         * 批量刷写到数据库
         */
        public void flushBatchToDatabase() {
            // 1. 获取当前缓冲的任务快照
            Map<K, V> snapshot;
            synchronized (batchTask) {
                if (!batchTask.hasPendingOperations()) {
                    log.debug("No pending operations to flush");
                    return;
                }

                // 创建快照，避免在写入过程中新操作影响
                snapshot = batchTask.snapshot();

                // 清空原任务，准备接收新操作
                batchTask.clear();
            }

            // 2. 取消已存在的定时任务（因为已经手动触发了）
            if (scheduledFlushTask != null && !scheduledFlushTask.isDone()) {
                scheduledFlushTask.cancel(false);
                scheduledFlushTask = null;
            }

            // 3. 执行批量写入回调
            try {
                flushCallback.accept(snapshot);
                log.info("Batch flush completed: {} operations", snapshot.size());
            } catch (Exception e) {
                log.error("Failed to flush batch to database", e);
                // 写入失败时，将任务重新放回缓冲队列（补偿机制）
                synchronized (batchTask) {
                    for (Map.Entry<K, V> entry : snapshot.entrySet()) {
                        batchTask.addOperation(entry.getKey(), entry.getValue());
                    }
                }
                throw e;
            }
        }
    }

    /**
     * 创建批量任务管理器
     * 
     * @param flushDelayMs 定时批量写入延迟时间（毫秒）
     * @param batchSizeThreshold 定量批量写入阈值
     * @param flushCallback 批量写入回调
     * @return 批量任务管理器
     */
    public <K, V> BatchTaskManager<K, V> createBatchTaskManager(long flushDelayMs,
            int batchSizeThreshold, Consumer<Map<K, V>> flushCallback) {
        return new BatchTaskManager<>(flushDelayMs, batchSizeThreshold, flushCallback, taskScheduler);
    }

    /**
     * 创建批量任务管理器（使用默认配置）
     * 
     * @param flushCallback 批量写入回调
     * @return 批量任务管理器
     */
    public <K, V> BatchTaskManager<K, V> createBatchTaskManager(Consumer<Map<K, V>> flushCallback) {
        return createBatchTaskManager(DEFAULT_FLUSH_DELAY_MS, DEFAULT_BATCH_SIZE_THRESHOLD,
                flushCallback);
    }
}

