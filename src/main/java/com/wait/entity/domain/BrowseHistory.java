package com.wait.entity.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 浏览记录实体
 * 用于持久化 Redis 中超过3天的浏览记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrowseHistory {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 浏览时间戳（毫秒）
     */
    private Long browseTime;

    /**
     * 创建时间（数据库自动设置）
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间（数据库自动更新）
     */
    private LocalDateTime updatedAt;
}

