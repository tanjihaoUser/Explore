package com.wait.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.wait.service.RedisListQueueService;
import com.wait.util.BoundUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis List 消息队列服务实现
 * 使用 Redis List 实现简单的生产者-消费者消息队列
 * 
 * Redis 命令使用：
 * - LPUSH: 从左侧推入消息（生产者）
 * - RPOP: 从右侧弹出消息（消费者，非阻塞）
 * - BRPOP: 从右侧弹出消息（消费者，阻塞）
 * - LLEN: 获取队列长度
 * - LRANGE: 批量获取消息
 * - DEL: 清空队列
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisListQueueServiceImpl implements RedisListQueueService {

    private final BoundUtil boundUtil;

    private static final String QUEUE_PREFIX = "queue:";

    @Override
    public Long sendMessage(String queueName, String message) {
        if (queueName == null || message == null) {
            throw new IllegalArgumentException("Queue name and message cannot be null");
        }
        String key = QUEUE_PREFIX + queueName;
        Long size = boundUtil.leftPush(key, message);
        log.debug("Message sent to queue {}: {}, queue size: {}", queueName, message, size);
        return size;
    }

    @Override
    public Long sendMessages(String queueName, List<String> messages) {
        if (queueName == null || messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("Queue name and messages cannot be null or empty");
        }
        String key = QUEUE_PREFIX + queueName;
        String[] messageArray = messages.toArray(new String[0]);
        Long size = boundUtil.leftPush(key, messageArray);
        log.debug("{} messages sent to queue {}, queue size: {}", messages.size(), queueName, size);
        return size;
    }

    @Override
    public String receiveMessage(String queueName) {
        if (queueName == null) {
            throw new IllegalArgumentException("Queue name cannot be null");
        }
        String key = QUEUE_PREFIX + queueName;
        String message = boundUtil.rightPop(key, String.class);
        if (message != null) {
            log.debug("Message received from queue {}: {}", queueName, message);
        }
        return message;
    }

    @Override
    public String receiveMessage(String queueName, long timeout, TimeUnit unit) {
        if (queueName == null) {
            throw new IllegalArgumentException("Queue name cannot be null");
        }
        String key = QUEUE_PREFIX + queueName;
        String message = boundUtil.blockRightPop(key, timeout, unit, String.class);
        if (message != null) {
            log.debug("Message received from queue {} (blocking): {}", queueName, message);
        }
        return message;
    }

    @Override
    public List<String> receiveMessages(String queueName, int count) {
        if (queueName == null || count <= 0) {
            throw new IllegalArgumentException("Queue name cannot be null and count must be positive");
        }
        String key = QUEUE_PREFIX + queueName;
        List<String> messages = new ArrayList<>();
        
        // 批量接收消息
        for (int i = 0; i < count; i++) {
            String message = boundUtil.rightPop(key, String.class);
            if (message == null) {
                break;
            }
            messages.add(message);
        }
        
        if (!messages.isEmpty()) {
            log.debug("{} messages received from queue {}", messages.size(), queueName);
        }
        return messages;
    }

    @Override
    public Long getQueueSize(String queueName) {
        if (queueName == null) {
            throw new IllegalArgumentException("Queue name cannot be null");
        }
        String key = QUEUE_PREFIX + queueName;
        return boundUtil.listSize(key);
    }

    @Override
    public Boolean clearQueue(String queueName) {
        if (queueName == null) {
            throw new IllegalArgumentException("Queue name cannot be null");
        }
        String key = QUEUE_PREFIX + queueName;
        Boolean result = boundUtil.del(key);
        if (Boolean.TRUE.equals(result)) {
            log.info("Queue {} cleared", queueName);
        }
        return result;
    }
}

