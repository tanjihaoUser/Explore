package com.wait.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import com.wait.service.DelayQueueService;
import com.wait.service.DelayTaskHandler;
import com.wait.util.BoundUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 延迟队列服务实现
 * 基于 Redis SortedSet 实现精确的延迟任务队列
 * Redis 命令使用：
 * - ZADD: 添加延迟任务（分数为执行时间戳）
 * - ZRANGEBYSCORE: 获取到期任务
 * - ZREM: 删除任务
 * - ZCARD: 获取队列长度
 * - ZSCORE: 获取任务执行时间
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DelayQueueServiceImpl implements DelayQueueService {

    private final BoundUtil boundUtil;

    @Qualifier("refreshScheduler")
    private final ThreadPoolTaskScheduler taskScheduler;

    private static final String QUEUE_PREFIX = "delay:queue:";

    /** 默认轮询间隔：1秒 */
    private static final long DEFAULT_POLL_INTERVAL_MS = 1000;

    /** 默认每次获取任务数量 */
    private static final int DEFAULT_BATCH_SIZE = 100;

    /** 任务处理器映射：queueName -> handler */
    private final Map<String, DelayTaskHandler> handlers = new ConcurrentHashMap<>();

    /** 消费任务映射：queueName -> scheduledFuture */
    private final Map<String, ScheduledFuture<?>> consumingTasks = new ConcurrentHashMap<>();

    @Override
    public Boolean addTask(String queueName, String taskId, long executeTime) {
        if (queueName == null || taskId == null) {
            throw new IllegalArgumentException("Queue name and task ID cannot be null");
        }

        String key = QUEUE_PREFIX + queueName;
        Boolean added = boundUtil.zAdd(key, taskId, executeTime);

        if (Boolean.TRUE.equals(added)) {
            log.debug("Task added to delay queue: queue={}, taskId={}, executeTime={}",
                    queueName, taskId, executeTime);
        }

        return added;
    }

    @Override
    public Boolean addTaskWithDelay(String queueName, String taskId, long delaySeconds) {
        if (queueName == null || taskId == null || delaySeconds < 0) {
            throw new IllegalArgumentException("Invalid parameters");
        }

        long executeTime = System.currentTimeMillis() + delaySeconds * 1000;
        return addTask(queueName, taskId, executeTime);
    }

    @Override
    public List<String> getReadyTasks(String queueName, int limit) {
        if (queueName == null || limit <= 0) {
            throw new IllegalArgumentException("Queue name cannot be null and limit must be positive");
        }

        String key = QUEUE_PREFIX + queueName;
        long currentTime = System.currentTimeMillis();

        // ZRANGEBYSCORE key 0 currentTime LIMIT 0 limit
        // 获取分数在 [0, currentTime] 范围内的任务（已到期）
        List<String> readyTasks = boundUtil.zRangeByScore(key, 0, currentTime, String.class);

        if (readyTasks == null || readyTasks.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>(readyTasks);
        // 限制返回数量
        if (result.size() > limit) {
            result = result.subList(0, limit);
        }

        log.debug("Found {} ready tasks in queue {}", result.size(), queueName);
        return result;
    }

    @Override
    public List<String> pollReadyTasks(String queueName, int limit) {
        if (queueName == null || limit <= 0) {
            throw new IllegalArgumentException("Queue name cannot be null and limit must be positive");
        }

        // 获取到期任务
        List<String> readyTasks = getReadyTasks(queueName, limit);

        if (readyTasks.isEmpty()) {
            return new ArrayList<>();
        }

        // 删除已获取的任务
        String key = QUEUE_PREFIX + queueName;
        String[] taskIds = readyTasks.toArray(new String[0]);
        boundUtil.zRem(key, taskIds);

        log.debug("Polled {} tasks from queue {}", readyTasks.size(), queueName);
        return readyTasks;
    }

    @Override
    public Boolean removeTask(String queueName, String taskId) {
        if (queueName == null || taskId == null) {
            throw new IllegalArgumentException("Queue name and task ID cannot be null");
        }

        String key = QUEUE_PREFIX + queueName;
        Long removed = boundUtil.zRem(key, taskId);
        boolean success = removed != null && removed > 0;

        if (success) {
            log.debug("Task removed from delay queue: queue={}, taskId={}", queueName, taskId);
        }

        return success;
    }

    @Override
    public Long getTaskExecuteTime(String queueName, String taskId) {
        if (queueName == null || taskId == null) {
            throw new IllegalArgumentException("Queue name and task ID cannot be null");
        }

        String key = QUEUE_PREFIX + queueName;
        Double score = boundUtil.zScore(key, taskId);

        if (score == null) {
            return null;
        }

        return score.longValue();
    }

    @Override
    public Long getQueueSize(String queueName) {
        if (queueName == null) {
            throw new IllegalArgumentException("Queue name cannot be null");
        }

        String key = QUEUE_PREFIX + queueName;
        return boundUtil.zCard(key);
    }

    @Override
    public Boolean clearQueue(String queueName) {
        if (queueName == null) {
            throw new IllegalArgumentException("Queue name cannot be null");
        }

        String key = QUEUE_PREFIX + queueName;
        Boolean result = boundUtil.del(key);

        if (Boolean.TRUE.equals(result)) {
            log.info("Delay queue cleared: {}", queueName);
        }

        return result;
    }

    @Override
    public void registerHandler(String queueName, DelayTaskHandler handler) {
        if (queueName == null || handler == null) {
            throw new IllegalArgumentException("Queue name and handler cannot be null");
        }
        handlers.put(queueName, handler);
        log.info("Handler registered for queue: {}", queueName);
    }

    @Override
    public void startConsuming(String queueName) {
        if (queueName == null) {
            throw new IllegalArgumentException("Queue name cannot be null");
        }

        // 检查是否已有消费任务
        if (consumingTasks.containsKey(queueName)) {
            log.warn("Queue {} is already being consumed", queueName);
            return;
        }

        // 检查是否有处理器
        DelayTaskHandler handler = handlers.get(queueName);
        if (handler == null) {
            throw new IllegalStateException("No handler registered for queue: " + queueName);
        }

        // 创建定时轮询任务
        ScheduledFuture<?> future = taskScheduler.scheduleWithFixedDelay(
                () -> consumeTasks(queueName, handler),
                DEFAULT_POLL_INTERVAL_MS);

        consumingTasks.put(queueName, future);
        log.info("Started consuming queue: {}", queueName);
    }

    @Override
    public void stopConsuming(String queueName) {
        if (queueName == null) {
            throw new IllegalArgumentException("Queue name cannot be null");
        }

        ScheduledFuture<?> future = consumingTasks.remove(queueName);
        if (future != null) {
            future.cancel(false);
            log.info("Stopped consuming queue: {}", queueName);
        } else {
            log.warn("Queue {} is not being consumed", queueName);
        }
    }

    @Override
    public Boolean isConsuming(String queueName) {
        if (queueName == null) {
            return false;
        }
        ScheduledFuture<?> future = consumingTasks.get(queueName);
        return future != null && !future.isCancelled() && !future.isDone();
    }

    /**
     * 消费任务（定时轮询执行）
     */
    private void consumeTasks(String queueName, DelayTaskHandler handler) {
        try {
            // 获取到期任务
            List<String> readyTasks = pollReadyTasks(queueName, DEFAULT_BATCH_SIZE);

            if (readyTasks.isEmpty()) {
                return;
            }

            log.debug("Consuming {} tasks from queue {}", readyTasks.size(), queueName);

            // 处理每个任务
            for (String taskId : readyTasks) {
                try {
                    boolean success = handler.handle(taskId);
                    if (success) {
                        log.debug("Task handled successfully: queue={}, taskId={}", queueName, taskId);
                    } else {
                        log.warn("Task handling failed (will retry): queue={}, taskId={}", queueName, taskId);
                        // 处理失败，重新添加到队列（延迟1秒后重试）
                        long retryTime = System.currentTimeMillis() + 1000;
                        addTask(queueName, taskId, retryTime);
                    }
                } catch (Exception e) {
                    log.error("Error handling task: queue={}, taskId={}", queueName, taskId, e);
                    // 异常情况，重新添加到队列（延迟5秒后重试）
                    long retryTime = System.currentTimeMillis() + 5000;
                    addTask(queueName, taskId, retryTime);
                }
            }
        } catch (Exception e) {
            log.error("Error consuming tasks from queue: {}", queueName, e);
        }
    }
}
