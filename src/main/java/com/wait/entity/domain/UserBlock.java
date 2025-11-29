package com.wait.entity.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户黑名单实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBlock {
    private Long id;
    private Long userId; // 拉黑者ID
    private Long blockedUserId; // 被拉黑的用户ID
    private Long createdAt; // 拉黑时间戳（毫秒）
}

