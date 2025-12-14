package com.wait.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.connection.RedisZSetCommands.Aggregate;
import org.springframework.stereotype.Service;

import com.wait.service.MultiDimensionSortService;
import com.wait.util.BoundUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 多维度排序服务实现
 * 
 * 基于多个 Redis SortedSet 实现多维度综合排序，通过加权合并多个维度的分数得到综合排序结果。
 * 
 * 核心功能：
 * 1. 管理多个维度的排序数据（价格、销量、评分等）
 * 2. 将多个维度合并成综合排序（使用 ZUNIONSTORE）
 * 3. 获取综合排序结果（按综合分数从高到低）
 * 
 * 典型应用场景：
 * - 商品排序：价格 + 销量 + 评分
 * - 内容排序：热度 + 时间 + 质量
 * - 用户排序：活跃度 + 贡献度 + 影响力
 * 
 * 使用流程：
 * 1. 添加各维度数据（addDimensionData）
 * 2. 执行综合排序（compositeSort）- 必须步骤
 * 3. 获取排序结果（getSortedResult）
 * 
 * ⚠️ 重要提示：
 * - 必须先调用 compositeSort() 生成结果，才能使用 getSortedResult() 获取排序结果
 * - 如果先调用 getSortedResult() 而没有执行 compositeSort()，会返回空列表
 * - 权重功能尚未完全实现，当前使用默认权重（所有维度权重相等）
 * 
 * Redis 命令使用：
 * - ZADD: 添加单维度数据
 * - ZINCRBY: 增加单维度分数
 * - ZUNIONSTORE: 多维度合并（加权）- 返回合并后的成员数量（Long值）
 * - ZREVRANGE: 获取排序结果
 * - ZREVRANK: 获取排名
 * - ZSCORE: 获取分数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiDimensionSortServiceImpl implements MultiDimensionSortService {

    private final BoundUtil boundUtil;

    private static final String DIMENSION_PREFIX = "sort:dimension:";
    private static final String RESULT_PREFIX = "sort:result:";

    @Override
    public Boolean addDimensionData(String dimension, String itemId, double score) {
        if (dimension == null || itemId == null) {
            throw new IllegalArgumentException("Dimension and item ID cannot be null");
        }

        String key = DIMENSION_PREFIX + dimension;
        Boolean added = boundUtil.zAdd(key, itemId, score);

        if (Boolean.TRUE.equals(added)) {
            log.debug("Dimension data added: dimension={}, itemId={}, score={}", dimension, itemId, score);
        }

        return added;
    }

    @Override
    public Long addDimensionDataBatch(String dimension, Map<String, Double> data) {
        if (dimension == null || data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Dimension and data cannot be null or empty");
        }

        String key = DIMENSION_PREFIX + dimension;
        long count = 0;

        for (Map.Entry<String, Double> entry : data.entrySet()) {
            Boolean added = boundUtil.zAdd(key, entry.getKey(), entry.getValue());
            if (Boolean.TRUE.equals(added)) {
                count++;
            }
        }

        log.debug("Batch added {} items to dimension {}", count, dimension);
        return count;
    }

    @Override
    public Boolean updateDimensionData(String dimension, String itemId, double score) {
        // ZADD 如果成员已存在会更新分数
        return addDimensionData(dimension, itemId, score);
    }

    @Override
    public Double incrementDimensionScore(String dimension, String itemId, double delta) {
        if (dimension == null || itemId == null) {
            throw new IllegalArgumentException("Dimension and item ID cannot be null");
        }

        String key = DIMENSION_PREFIX + dimension;
        return boundUtil.zIncrBy(key, itemId, delta);
    }

    /**
     * 综合排序（多维度加权合并）
     * 
     * 将多个维度的 SortedSet 合并成一个综合排序结果，存储在 resultKey 中。
     * 
     * 执行流程：
     * 1. 构建源key列表（各维度的SortedSet）
     * 2. 执行 ZUNIONSTORE 合并所有维度
     * 3. 返回合并后的成员数量
     * 
     * ⚠️ 注意：
     * - 必须先调用此方法生成结果，才能使用 getSortedResult() 获取排序结果
     * - 权重功能尚未完全实现，当前使用默认权重（所有维度权重相等）
     * - 返回的 Long 值是合并后 SortedSet 中成员的数量（ZCARD的结果）
     * 
     * @param resultKey  结果存储的key（后续通过 getSortedResult(resultKey, ...) 获取结果）
     * @param dimensions 维度列表（如：["price", "sales", "rating"]）
     * @param weights    权重列表（与dimensions对应，如：[0.3, 0.5, 0.2]）
     * @param aggregate  聚合类型（SUM:求和, MAX:取最大值, MIN:取最小值）
     * @return 合并后的项目数量（Long值，即 ZUNIONSTORE 返回的成员数量）
     */
    @Override
    public Long compositeSort(String resultKey, List<String> dimensions, List<Double> weights, Aggregate aggregate) {
        if (resultKey == null || dimensions == null || dimensions.isEmpty()) {
            throw new IllegalArgumentException("Result key and dimensions cannot be null or empty");
        }

        if (weights != null && weights.size() != dimensions.size()) {
            throw new IllegalArgumentException("Weights size must match dimensions size");
        }

        // 构建源key列表
        List<String> sourceKeys = new ArrayList<>();
        for (String dimension : dimensions) {
            sourceKeys.add(DIMENSION_PREFIX + dimension);
        }

        // 构建结果key
        String fullResultKey = RESULT_PREFIX + resultKey;

        // 执行 ZUNIONSTORE（支持权重）
        // ZUNIONSTORE 返回合并后 SortedSet 中成员的数量（ZCARD的结果）
        Long result;
        if (weights != null && !weights.isEmpty()) {
            // 使用带权重的ZUNIONSTORE
            result = boundUtil.zUnionAndStore(fullResultKey, sourceKeys, weights, aggregate);
            log.debug("Composite sort with weights: resultKey={}, dimensions={}, weights={}, memberCount={}",
                    resultKey, dimensions, weights, result);
        } else {
            // 无权重，使用默认方法
            result = boundUtil.zUnionAndStore(fullResultKey, sourceKeys, aggregate);
            log.debug("Composite sort without weights: resultKey={}, dimensions={}, memberCount={}",
                    resultKey, dimensions, result);
        }

        log.info("Composite sort created: resultKey={}, dimensions={}, memberCount={}, aggregate={}",
                resultKey, dimensions, result, aggregate);

        return result;
    }

    @Override
    public Long compositeSort(String resultKey, List<String> dimensions, Aggregate aggregate) {
        // 使用默认权重（所有维度权重相等）
        return compositeSort(resultKey, dimensions, null, aggregate);
    }

    /**
     * 获取综合排序结果
     * 
     * ⚠️ 重要提示：必须先调用 compositeSort() 生成结果，否则返回空列表
     * 
     * @param resultKey 结果key（与compositeSort中的resultKey对应）
     * @param start     起始排名（0-based）
     * @param end       结束排名（0-based，-1表示最后）
     * @return 项目ID列表（按分数从高到低），如果结果key不存在则返回空列表
     */
    @Override
    public List<String> getSortedResult(String resultKey, long start, long end) {
        if (resultKey == null) {
            throw new IllegalArgumentException("Result key cannot be null");
        }

        String fullResultKey = RESULT_PREFIX + resultKey;
        List<String> result = boundUtil.zReverseRange(fullResultKey, start, end, String.class);

        if (result == null || result.isEmpty()) {
            log.warn("Sorted result is empty for key: {}. Did you call compositeSort() first?", resultKey);
        }

        return result != null ? new ArrayList<>(result) : new ArrayList<>();
    }

    @Override
    public Long getItemRank(String resultKey, String itemId) {
        if (resultKey == null || itemId == null) {
            throw new IllegalArgumentException("Result key and item ID cannot be null");
        }

        String fullResultKey = RESULT_PREFIX + resultKey;
        Long rank = boundUtil.zRevRank(fullResultKey, itemId);

        return rank;
    }

    @Override
    public Double getItemScore(String resultKey, String itemId) {
        if (resultKey == null || itemId == null) {
            throw new IllegalArgumentException("Result key and item ID cannot be null");
        }

        String fullResultKey = RESULT_PREFIX + resultKey;
        return boundUtil.zScore(fullResultKey, itemId);
    }

    @Override
    public List<String> getDimensionSortResult(String dimension, long start, long end) {
        if (dimension == null) {
            throw new IllegalArgumentException("Dimension cannot be null");
        }

        String key = DIMENSION_PREFIX + dimension;
        List<String> result = boundUtil.zReverseRange(key, start, end, String.class);

        return result != null ? result : new ArrayList<>();
    }

    @Override
    public Double getDimensionScore(String dimension, String itemId) {
        if (dimension == null || itemId == null) {
            throw new IllegalArgumentException("Dimension and item ID cannot be null");
        }

        String key = DIMENSION_PREFIX + dimension;
        return boundUtil.zScore(key, itemId);
    }

    @Override
    public Boolean deleteCompositeResult(String resultKey) {
        if (resultKey == null) {
            throw new IllegalArgumentException("Result key cannot be null");
        }

        String fullResultKey = RESULT_PREFIX + resultKey;
        return boundUtil.del(fullResultKey);
    }
}
