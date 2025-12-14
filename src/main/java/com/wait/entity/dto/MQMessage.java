package com.wait.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MQ消息实体
 * 用于Redis队列消息的序列化和反序列化
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MQMessage {
    /** 消息主题 */
    private String topic;
    /** 消息键 */
    private String key;
    /** 消息内容 */
    private Object message;
    /** 消息时间戳 */
    private long timestamp;
}
