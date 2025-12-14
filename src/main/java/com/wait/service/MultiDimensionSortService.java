package com.wait.service;

import org.springframework.data.redis.connection.RedisZSetCommands;

import java.util.List;
import java.util.Map;

/**
 * 多维度排序服务
 * 基于多个 Redis SortedSet 实现多维度综合排序
 * 使用场景：
 * - 商品排序（价格+销量+评分）
 * - 内容排序（热度+时间+质量）
 * - 用户排序（活跃度+贡献度+影响力）
 */
public interface MultiDimensionSortService {

    /**
     * 添加单维度数据
     * @param dimension 维度名称（如：price, sales, rating）
     * @param itemId 项目ID
     * @param score 分数
     * @return 是否成功
     */
    Boolean addDimensionData(String dimension, String itemId, double score);

    /**
     * 批量添加单维度数据
     * @param dimension 维度名称
     * @param data Map<itemId, score>
     * @return 添加成功的数量
     */
    Long addDimensionDataBatch(String dimension, Map<String, Double> data);

    /**
     * 更新单维度数据
     * @param dimension 维度名称
     * @param itemId 项目ID
     * @param score 新分数
     * @return 是否成功
     */
    Boolean updateDimensionData(String dimension, String itemId, double score);

    /**
     * 增加单维度分数
     * @param dimension 维度名称
     * @param itemId 项目ID
     * @param delta 增量
     * @return 更新后的分数
     */
    Double incrementDimensionScore(String dimension, String itemId, double delta);

    /**
     * 综合排序（多维度加权合并）
     * @param resultKey 结果存储的key
     * @param dimensions 维度列表
     * @param weights 权重列表（与dimensions对应）
     * @param aggregate 聚合类型（SUM, MAX, MIN）
     * @return 合并后的项目数量
     */
    Long compositeSort(String resultKey, List<String> dimensions, List<Double> weights, RedisZSetCommands.Aggregate aggregate);

    /**
     * 综合排序（使用默认权重，所有维度权重相等）
     * @param resultKey 结果存储的key
     * @param dimensions 维度列表
     * @param aggregate 聚合类型（SUM, MAX, MIN）
     * @return 合并后的项目数量
     */
    Long compositeSort(String resultKey, List<String> dimensions, RedisZSetCommands.Aggregate aggregate);

    /**
     * 获取综合排序结果
     * @param resultKey 结果key
     * @param start 起始排名（0-based）
     * @param end 结束排名（0-based，-1表示最后）
     * @return 项目ID列表（按分数从高到低）
     */
    List<String> getSortedResult(String resultKey, long start, long end);

    /**
     * 获取项目在综合排序中的排名
     * @param resultKey 结果key
     * @param itemId 项目ID
     * @return 排名（0-based），如果不存在返回null
     */
    Long getItemRank(String resultKey, String itemId);

    /**
     * 获取项目的综合分数
     * @param resultKey 结果key
     * @param itemId 项目ID
     * @return 综合分数，如果不存在返回null
     */
    Double getItemScore(String resultKey, String itemId);

    /**
     * 获取单维度排序结果
     * @param dimension 维度名称
     * @param start 起始排名
     * @param end 结束排名
     * @return 项目ID列表（按分数从高到低）
     */
    List<String> getDimensionSortResult(String dimension, long start, long end);

    /**
     * 获取项目在单维度中的分数
     * @param dimension 维度名称
     * @param itemId 项目ID
     * @return 分数，如果不存在返回null
     */
    Double getDimensionScore(String dimension, String itemId);

    /**
     * 删除综合排序结果
     * @param resultKey 结果key
     * @return 是否成功
     */
    Boolean deleteCompositeResult(String resultKey);
}

