package com.wait.service;

/**
 * 消息处理器接口
 * 用于处理MQ消息
 */
@FunctionalInterface
public interface MessageHandler {
    /**
     * 处理消息
     * 
     * @param topic   消息主题
     * @param key     消息键
     * @param message 消息内容
     */
    void handleMessage(String topic, String key, Object message);
}
