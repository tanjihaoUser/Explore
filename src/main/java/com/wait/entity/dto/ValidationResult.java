package com.wait.entity.dto;

import lombok.Getter;

/**
 * 数据校验结果
 * 用于记录Redis和数据库数据一致性校验的结果
 */
@Getter
public class ValidationResult {
    /** 校验的数据类型 */
    private final String dataType;
    /** 校验的数据ID */
    private final Long dataId;
    /** Redis中的数量 */
    private final long redisCount;
    /** 数据库中的数量 */
    private final long dbCount;
    /** 不一致的数量 */
    private final long diffCount;
    /** 是否修复成功 */
    private final boolean fixed;

    public ValidationResult(String dataType, Long dataId, long redisCount, long dbCount,
            long diffCount, boolean fixed) {
        this.dataType = dataType;
        this.dataId = dataId;
        this.redisCount = redisCount;
        this.dbCount = dbCount;
        this.diffCount = diffCount;
        this.fixed = fixed;
    }

    @Override
    public String toString() {
        return String.format("ValidationResult{type=%s, id=%d, redis=%d, db=%d, diff=%d, fixed=%s}",
                dataType, dataId, redisCount, dbCount, diffCount, fixed);
    }
}
