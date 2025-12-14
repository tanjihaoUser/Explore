package com.wait.entity.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UV 统计数据实体
 * 用于持久化 Redis 中超过7天的 UV 统计数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UVStatistics {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 资源类型（post, article, page等）
     */
    private String resourceType;

    /**
     * 资源ID
     */
    private Long resourceId;

    /**
     * 日期（格式：yyyyMMdd）
     */
    private String date;

    /**
     * 访客ID（用户ID或IP地址）
     */
    private String visitorId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
