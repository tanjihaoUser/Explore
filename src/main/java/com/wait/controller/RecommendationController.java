package com.wait.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wait.service.UserRecommendationService;
import com.wait.util.ResponseUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户推荐控制器
 * 提供用户推荐相关的REST API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final UserRecommendationService recommendationService;

    /**
     * 获取推荐用户列表
     * GET /api/recommendations/user/{userId}?count=10
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getRecommendedUsers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int count) {
        log.info("获取推荐用户列表: userId={}, count={}", userId, count);

        List<Long> recommendedUserIds = recommendationService.recommendAndMark(userId, count);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("recommendedUserIds", recommendedUserIds);
        data.put("count", recommendedUserIds.size());

        return ResponseUtil.success("获取推荐用户成功", data);
    }

    /**
     * 获取推荐用户列表（不标记为已推荐）
     * GET /api/recommendations/user/{userId}/preview?count=10
     */
    @GetMapping("/user/{userId}/preview")
    public ResponseEntity<Map<String, Object>> previewRecommendedUsers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int count) {
        log.info("预览推荐用户列表: userId={}, count={}", userId, count);

        List<Long> recommendedUserIds = recommendationService.recommendUsers(userId, count);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("recommendedUserIds", recommendedUserIds);
        data.put("count", recommendedUserIds.size());

        return ResponseUtil.success("预览推荐用户成功", data);
    }

    /**
     * 清除推荐历史（重新推荐）
     * PUT /api/recommendations/user/{userId}/clear
     */
    @PutMapping("/user/{userId}/clear")
    public ResponseEntity<Map<String, Object>> clearRecommendedHistory(@PathVariable Long userId) {
        log.info("清除推荐历史: userId={}", userId);

        Long clearedCount = recommendationService.clearRecommendedHistory(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("clearedCount", clearedCount);

        return ResponseUtil.success("清除推荐历史成功", data);
    }

    /**
     * 获取候选用户数量
     * GET /api/recommendations/user/{userId}/candidate-count
     */
    @GetMapping("/user/{userId}/candidate-count")
    public ResponseEntity<Map<String, Object>> getCandidateCount(@PathVariable Long userId) {
        log.info("获取候选用户数量: userId={}", userId);

        Long count = recommendationService.getCandidateCount(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("candidateCount", count != null ? count : 0L);

        return ResponseUtil.success(data);
    }
}

