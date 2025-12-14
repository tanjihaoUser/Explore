package com.wait.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis List 消息队列服务
 * 基于 Redis List 实现简单的生产者-消费者消息队列
 */
public interface RedisListQueueService {

    /**
     * 发送消息到队列（生产者）
     * 
     * @param queueName 队列名称
     * @param message 消息内容
     * @return 队列长度
     */
    Long sendMessage(String queueName, String message);

    /**
     * 批量发送消息到队列
     * 
     * @param queueName 队列名称
     * @param messages 消息列表
     * @return 队列长度
     */
    Long sendMessages(String queueName, List<String> messages);

    /**
     * 从队列接收消息（消费者，非阻塞）
     * 
     * @param queueName 队列名称
     * @return 消息内容，如果队列为空返回null
     */
    String receiveMessage(String queueName);

    /**
     * 从队列接收消息（消费者，阻塞）
     * 
     * @param queueName 队列名称
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 消息内容，如果超时返回null
     */
    String receiveMessage(String queueName, long timeout, TimeUnit unit);

    /**
     * 批量接收消息
     * 
     * @param queueName 队列名称
     * @param count 接收数量
     * @return 消息列表
     */
    List<String> receiveMessages(String queueName, int count);

    /**
     * 获取队列长度
     * 
     * @param queueName 队列名称
     * @return 队列长度
     */
    Long getQueueSize(String queueName);

    /**
     * 清空队列
     * 
     * @param queueName 队列名称
     * @return 是否成功
     */
    Boolean clearQueue(String queueName);
}

