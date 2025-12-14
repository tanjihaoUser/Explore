package com.wait.entity.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 时间窗口统计数据实体
 * 用于持久化 Redis 中超过7天的统计数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeWindowStatistics {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 指标名称（如：visit_count, sales_amount等）
     */
    private String metric;

    /**
     * 数据值（存储为字符串，支持数字和文本）
     */
    private String value;

    /**
     * 时间戳（毫秒）
     */
    private Long timestamp;

    /**
     * 创建时间（数据库自动设置）
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间（数据库自动更新）
     */
    private LocalDateTime updatedAt;
}

