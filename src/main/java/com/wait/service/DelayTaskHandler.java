package com.wait.service;

/**
 * 延迟任务处理器接口
 * 用于处理延迟队列中的任务
 */
@FunctionalInterface
public interface DelayTaskHandler {
    /**
     * 处理延迟任务
     * 
     * @param taskId 任务ID
     * @return 是否处理成功，返回false会触发重试
     */
    boolean handle(String taskId);
}
