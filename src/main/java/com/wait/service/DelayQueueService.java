package com.wait.service;

import java.util.List;

/**
 * 延迟队列服务
 * 基于 Redis SortedSet 实现精确的延迟任务队列
 * 使用场景：
 * - 订单15分钟未支付自动取消
 * - 优惠券过期提醒
 * - 定时推送消息
 * - 定时数据同步
 */
public interface DelayQueueService {

    /**
     * 添加延迟任务
     * @param queueName 队列名称
     * @param taskId 任务ID
     * @param executeTime 执行时间戳（毫秒）
     * @return 是否成功
     */
    Boolean addTask(String queueName, String taskId, long executeTime);

    /**
     * 添加延迟任务（延迟N秒后执行）
     * @param queueName 队列名称
     * @param taskId 任务ID
     * @param delaySeconds 延迟秒数
     * @return 是否成功
     */
    Boolean addTaskWithDelay(String queueName, String taskId, long delaySeconds);

    /**
     * 获取到期任务（不删除）
     * @param queueName 队列名称
     * @param limit 最多获取数量
     * @return 任务ID列表
     */
    List<String> getReadyTasks(String queueName, int limit);

    /**
     * 获取并删除到期任务
     * @param queueName 队列名称
     * @param limit 最多获取数量
     * @return 任务ID列表
     */
    List<String> pollReadyTasks(String queueName, int limit);

    /**
     * 删除任务
     * @param queueName 队列名称
     * @param taskId 任务ID
     * @return 是否成功
     */
    Boolean removeTask(String queueName, String taskId);

    /**
     * 获取任务执行时间
     * @param queueName 队列名称
     * @param taskId 任务ID
     * @return 执行时间戳（毫秒），如果任务不存在返回null
     */
    Long getTaskExecuteTime(String queueName, String taskId);

    /**
     * 获取队列长度
     * @param queueName 队列名称
     * @return 队列长度
     */
    Long getQueueSize(String queueName);

    /**
     * 清空队列
     * @param queueName 队列名称
     * @return 是否成功
     */
    Boolean clearQueue(String queueName);

    /**
     * 注册任务处理器（用于消费延迟任务）
     * @param queueName 队列名称
     * @param handler 任务处理器
     */
    void registerHandler(String queueName, DelayTaskHandler handler);

    /**
     * 启动消费（开始定时轮询）
     * @param queueName 队列名称
     */
    void startConsuming(String queueName);

    /**
     * 停止消费
     * @param queueName 队列名称
     */
    void stopConsuming(String queueName);

    /**
     * 检查是否正在消费
     * @param queueName 队列名称
     * @return 是否正在消费
     */
    Boolean isConsuming(String queueName);
}

