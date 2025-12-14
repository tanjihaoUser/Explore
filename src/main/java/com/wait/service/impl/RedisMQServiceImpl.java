package com.wait.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wait.entity.CacheSyncParam;
import com.wait.entity.dto.MQMessage;
import com.wait.service.MessageHandler;
import com.wait.service.MQService;
import com.wait.service.RedisListQueueService;
import com.wait.util.message.AsyncDataMsg;
import com.wait.util.message.CompensationMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service("redisMQService")
@Slf4j
public class RedisMQServiceImpl implements MQService {

    private final RedisListQueueService redisListQueueService;
    private final ObjectMapper objectMapper;

    // 队列名称前缀
    private static final String MESSAGE_QUEUE_PREFIX = "mq:message:";
    private static final String DLQ_QUEUE_NAME = "mq:dlq";

    // JSON 序列化器
    public RedisMQServiceImpl(RedisListQueueService redisListQueueService) {
        this.redisListQueueService = redisListQueueService;
        this.objectMapper = new ObjectMapper();
    }

    // 消费线程池
    private final ExecutorService consumerExecutor = Executors.newFixedThreadPool(2,
            r -> new Thread(r, "RedisMQ-Consumer"));

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            r -> new Thread(r, "RedisMQ-Scheduler")
    );

    private final AtomicBoolean running = new AtomicBoolean(false);

    // 消息处理器（由业务方注入）
    private MessageHandler messageHandler;

    @PostConstruct
    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            // 启动普通消息消费者
            consumerExecutor.execute(this::consumeMessages);
            // 启动死信队列消费者
            consumerExecutor.execute(this::consumeDLQMessages);
            // 启动监控任务
            scheduler.scheduleAtFixedRate(this::monitorQueue, 1, 1, TimeUnit.MINUTES);

            log.info("RedisMQ服务启动成功（已替换为Redis List队列）");
        }
    }

    @PreDestroy
    @Override
    public void shutdown() {
        if (running.compareAndSet(true, false)) {
            consumerExecutor.shutdown();
            scheduler.shutdown();
            try {
                if (!consumerExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    consumerExecutor.shutdownNow();
                }
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            log.info("RedisMQ服务已关闭");
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void sendMessage(String topic, String key, AsyncDataMsg message) {
        try {
            MQMessage mqMessage = new MQMessage(topic, key, message, System.currentTimeMillis());
            String messageJson = objectMapper.writeValueAsString(mqMessage);
            String queueName = MESSAGE_QUEUE_PREFIX + topic;
            
            Long queueSize = redisListQueueService.sendMessage(queueName, messageJson);
            log.debug("RedisMQ: 消息发送成功, topic: {}, key: {}, queue size: {}", topic, key, queueSize);
        } catch (Exception e) {
            log.error("RedisMQ: 消息发送失败, topic: {}, key: {}", topic, key, e);
            // 发送失败时可以考虑降级策略
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void sendDLMessage(String key, CompensationMsg message) {
        try {
            MQMessage dlqMessage = new MQMessage(DL_TOPIC, key, message, System.currentTimeMillis());
            String messageJson = objectMapper.writeValueAsString(dlqMessage);
            
            Long queueSize = redisListQueueService.sendMessage(DLQ_QUEUE_NAME, messageJson);
            log.debug("RedisMQ: 死信消息发送成功, key: {}, queue size: {}", key, queueSize);
        } catch (Exception e) {
            log.error("RedisMQ: 死信消息发送失败, key: {}", key, e);
        }
    }

    @Override
    public int getQueueSize() {
        // 返回所有主题队列的总大小（简化实现，实际可以更精确）
        try {
            Long dlqSize = redisListQueueService.getQueueSize(DLQ_QUEUE_NAME);
            return dlqSize != null ? dlqSize.intValue() : 0;
        } catch (Exception e) {
            log.error("RedisMQ: 获取队列大小失败", e);
            return 0;
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            // 检查死信队列大小，如果超过阈值则认为不健康
            Long dlqSize = redisListQueueService.getQueueSize(DLQ_QUEUE_NAME);
            return running.get() && (dlqSize == null || dlqSize < 10000);
        } catch (Exception e) {
            log.error("RedisMQ: 健康检查失败", e);
            return false;
        }
    }

    /**
     * 消费普通消息
     * 从所有主题队列中消费消息（简化实现，实际可以按主题分别消费）
     */
    private void consumeMessages() {
        // 这里简化实现，只消费默认主题队列
        // 实际应该维护一个主题列表，为每个主题启动独立的消费者
        String defaultQueue = MESSAGE_QUEUE_PREFIX + "default";
        
        while (running.get()) {
            try {
                // 使用阻塞弹出，超时时间1秒
                String messageJson = redisListQueueService.receiveMessage(defaultQueue, 1, TimeUnit.SECONDS);
                if (messageJson != null) {
                    MQMessage message = objectMapper.readValue(messageJson, MQMessage.class);
                    processMessage(message);
                }
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    break;
                }
                log.error("RedisMQ: 消息消费异常", e);
            }
        }
    }

    /**
     * 消费死信队列消息
     */
    private void consumeDLQMessages() {
        while (running.get()) {
            try {
                // 使用阻塞弹出，超时时间1秒
                String messageJson = redisListQueueService.receiveMessage(DLQ_QUEUE_NAME, 1, TimeUnit.SECONDS);
                if (messageJson != null) {
                    MQMessage dlqMessage = objectMapper.readValue(messageJson, MQMessage.class);
                    processDLQMessage(dlqMessage);
                }
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    break;
                }
                log.error("RedisMQ: 死信消息消费异常", e);
            }
        }
    }

    /**
     * 处理普通消息
     */
    private void processMessage(MQMessage message) {
        try {
            if (messageHandler != null) {
                messageHandler.handleMessage(message.getTopic(), message.getKey(), message.getMessage());
            } else {
                log.warn("RedisMQ: 未设置消息处理器，消息被忽略, topic: {}, key: {}",
                        message.getTopic(), message.getKey());
            }
        } catch (Exception e) {
            log.error("RedisMQ: 消息处理失败, topic: {}, key: {}",
                    message.getTopic(), message.getKey(), e);
            // 处理失败的消息进入死信队列
            CompensationMsg<?> compensationMsg = CompensationMsg.builder()
                    .originalParam(extractParamFromMessage(message))
                    .failReason(e.getMessage())
                    .failTime(System.currentTimeMillis())
                    .build();
            sendDLMessage(message.getKey(), compensationMsg);
        }
    }

    /**
     * 处理死信队列消息
     */
    private void processDLQMessage(MQMessage dlqMessage) {
        log.error("RedisMQ: 处理死信消息, key: {}, message: {}",
                dlqMessage.getKey(), dlqMessage.getMessage());
        // 这里可以实现死信消息的持久化、告警等逻辑
        // persistenceService.saveDeadLetter(dlqMessage);
        // alertService.sendAlert("死信消息告警", dlqMessage.toString());
    }

    /**
     * 监控队列状态
     */
    private void monitorQueue() {
        try {
            Long dlqSize = redisListQueueService.getQueueSize(DLQ_QUEUE_NAME);
            
            if (dlqSize != null && dlqSize > 1000) {
                log.error("RedisMQ: 死信队列堆积严重, 当前大小: {}", dlqSize);
            }
            
            // 可以添加更多监控逻辑，如监控各个主题队列的大小
        } catch (Exception e) {
            log.error("RedisMQ: 队列监控异常", e);
        }
    }


    /**
     * 设置消息处理器
     */
    public void setMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
    }

    private CacheSyncParam<Object> extractParamFromMessage(MQMessage message) {
        // 从消息中提取参数的逻辑
        if (message.getMessage() instanceof AsyncDataMsg) {
            AsyncDataMsg<?> asyncDataMsg = (AsyncDataMsg<?>) message.getMessage();
            // 这里简化处理，实际应该根据entityType创建正确的CacheSyncParam
            return CacheSyncParam.builder()
                    .key(asyncDataMsg.getKey())
                    .newValue(asyncDataMsg.getData())
                    .build();
        }
        return null;
    }
}