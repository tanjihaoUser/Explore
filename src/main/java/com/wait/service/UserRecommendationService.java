package com.wait.service;

import java.util.List;
import java.util.Set;

/**
 * 用户推荐服务
 * 基于 Redis Set 实现随机用户推荐功能
 */
public interface UserRecommendationService {

    /**
     * 添加候选用户到推荐池
     * 
     * @param userId 用户ID
     * @param candidateUserIds 候选用户ID列表
     * @return 添加成功的数量
     */
    Long addCandidates(Long userId, List<Long> candidateUserIds);

    /**
     * 随机推荐用户（不删除）
     * 
     * @param userId 用户ID
     * @param count 推荐数量
     * @return 推荐的用户ID列表
     */
    List<Long> recommendUsers(Long userId, int count);

    /**
     * 随机推荐用户并标记为已推荐（删除）
     * 
     * @param userId 用户ID
     * @param count 推荐数量
     * @return 推荐的用户ID列表
     */
    List<Long> recommendAndMark(Long userId, int count);

    /**
     * 标记用户为已推荐
     * 
     * @param userId 用户ID
     * @param recommendedUserId 已推荐的用户ID
     * @return 是否成功
     */
    Boolean markAsRecommended(Long userId, Long recommendedUserId);

    /**
     * 批量标记用户为已推荐
     * 
     * @param userId 用户ID
     * @param recommendedUserIds 已推荐的用户ID列表
     * @return 标记成功的数量
     */
    Long markAsRecommendedBatch(Long userId, List<Long> recommendedUserIds);

    /**
     * 清除已推荐记录（重新推荐）
     * 
     * @param userId 用户ID
     * @return 清除的数量
     */
    Long clearRecommendedHistory(Long userId);

    /**
     * 获取候选用户数量
     * 
     * @param userId 用户ID
     * @return 候选用户数量
     */
    Long getCandidateCount(Long userId);
}

