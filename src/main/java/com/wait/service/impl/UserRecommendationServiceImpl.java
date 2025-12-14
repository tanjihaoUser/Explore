package com.wait.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.wait.service.UserRecommendationService;
import com.wait.util.BoundUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户推荐服务实现
 * 基于 Redis Set 实现随机用户推荐功能
 * 
 * Redis 命令使用：
 * - SADD: 添加候选用户
 * - SRANDMEMBER: 随机获取用户（不删除）
 * - SPOP: 随机获取并删除用户
 * - SREM: 移除用户（标记为已推荐）
 * - SCARD: 获取候选用户数量
 * - SDIFF: 过滤已推荐用户
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRecommendationServiceImpl implements UserRecommendationService {

    private final BoundUtil boundUtil;

    private static final String CANDIDATE_PREFIX = "recommend:candidate:";
    private static final String RECOMMENDED_PREFIX = "recommend:shown:";

    @Override
    public Long addCandidates(Long userId, List<Long> candidateUserIds) {
        if (userId == null || candidateUserIds == null || candidateUserIds.isEmpty()) {
            return 0L;
        }

        String key = CANDIDATE_PREFIX + userId;
        Long[] ids = candidateUserIds.toArray(new Long[0]);
        Long added = boundUtil.sAdd(key, ids);
        
        if (added != null && added > 0) {
            log.debug("{} candidates added for user {}", added, userId);
        }
        
        return added != null ? added : 0L;
    }

    @Override
    public List<Long> recommendUsers(Long userId, int count) {
        if (userId == null || count <= 0) {
            return new ArrayList<>();
        }

        String candidateKey = CANDIDATE_PREFIX + userId;
        String recommendedKey = RECOMMENDED_PREFIX + userId;

        // 获取候选用户集合
        Set<Long> candidates = boundUtil.sMembers(candidateKey, Long.class);
        if (candidates == null || candidates.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取已推荐用户集合
        Set<Long> recommended = boundUtil.sMembers(recommendedKey, Long.class);
        if (recommended != null && !recommended.isEmpty()) {
            // 过滤已推荐用户
            candidates.removeAll(recommended);
        }

        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }

        // 随机选择用户
        List<Long> candidateList = new ArrayList<>(candidates);
        Collections.shuffle(candidateList);
        
        int actualCount = Math.min(count, candidateList.size());
        List<Long> result = candidateList.subList(0, actualCount);
        
        log.debug("Recommended {} users for user {}", result.size(), userId);
        return result;
    }

    @Override
    public List<Long> recommendAndMark(Long userId, int count) {
        if (userId == null || count <= 0) {
            return new ArrayList<>();
        }

        String candidateKey = CANDIDATE_PREFIX + userId;
        String recommendedKey = RECOMMENDED_PREFIX + userId;

        List<Long> recommended = new ArrayList<>();
        
        // 使用 SPOP 随机获取并删除（避免重复推荐）
        for (int i = 0; i < count; i++) {
            Long userIdToRecommend = boundUtil.sPop(candidateKey, Long.class);
            if (userIdToRecommend == null) {
                break; // 候选池已空
            }
            recommended.add(userIdToRecommend);
            
            // 标记为已推荐
            boundUtil.sAdd(recommendedKey, userIdToRecommend);
        }

        if (!recommended.isEmpty()) {
            log.debug("Recommended and marked {} users for user {}", recommended.size(), userId);
        }
        
        return recommended;
    }

    @Override
    public Boolean markAsRecommended(Long userId, Long recommendedUserId) {
        if (userId == null || recommendedUserId == null) {
            return false;
        }

        String key = RECOMMENDED_PREFIX + userId;
        Long added = boundUtil.sAdd(key, recommendedUserId);
        boolean success = added != null && added > 0;
        
        if (success) {
            log.debug("User {} marked as recommended for user {}", recommendedUserId, userId);
        }
        
        return success;
    }

    @Override
    public Long markAsRecommendedBatch(Long userId, List<Long> recommendedUserIds) {
        if (userId == null || recommendedUserIds == null || recommendedUserIds.isEmpty()) {
            return 0L;
        }

        String key = RECOMMENDED_PREFIX + userId;
        Long[] ids = recommendedUserIds.toArray(new Long[0]);
        Long added = boundUtil.sAdd(key, ids);
        
        if (added != null && added > 0) {
            log.debug("{} users marked as recommended for user {}", added, userId);
        }
        
        return added != null ? added : 0L;
    }

    @Override
    public Long clearRecommendedHistory(Long userId) {
        if (userId == null) {
            return 0L;
        }

        String key = RECOMMENDED_PREFIX + userId;
        Long count = boundUtil.sCard(key);
        
        // 删除已推荐记录
        Boolean deleted = boundUtil.del(key);
        
        if (Boolean.TRUE.equals(deleted) && count != null) {
            log.info("Cleared recommended history for user {}, count: {}", userId, count);
            return count;
        }
        
        return 0L;
    }

    @Override
    public Long getCandidateCount(Long userId) {
        if (userId == null) {
            return 0L;
        }

        String key = CANDIDATE_PREFIX + userId;
        Long count = boundUtil.sCard(key);
        return count != null ? count : 0L;
    }
}

