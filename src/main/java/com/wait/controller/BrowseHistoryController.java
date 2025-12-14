package com.wait.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wait.service.BrowseHistoryService;
import com.wait.util.ResponseUtil;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 浏览记录控制器
 * 提供浏览记录的增删查接口
 */
@Slf4j
@RestController
@RequestMapping("/api/browse-history")
@RequiredArgsConstructor
public class BrowseHistoryController {

    private final BrowseHistoryService browseHistoryService;

    /**
     * 记录浏览历史
     * POST /api/browse-history
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> recordBrowse(@RequestBody RecordBrowseRequest request) {
        log.info("Recording browse: userId={}, postId={}", request.getUserId(), request.getPostId());
        browseHistoryService.recordBrowse(request.getUserId(), request.getPostId());
        return ResponseUtil.success("浏览记录已保存");
    }

    /**
     * 获取浏览历史（最近N条）
     * GET /api/browse-history/user/{userId}?limit=20
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getBrowseHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "20") int limit) {
        log.info("Getting browse history for user: {}, limit: {}", userId, limit);
        List<Long> postIds = browseHistoryService.getBrowseHistory(userId, limit);
        
        Map<String, Object> data = new HashMap<>();
        data.put("postIds", postIds);
        data.put("count", postIds.size());
        return ResponseUtil.success(data);
    }

    /**
     * 分页获取浏览历史
     * GET /api/browse-history/user/{userId}/page?page=1&pageSize=20
     */
    @GetMapping("/user/{userId}/page")
    public ResponseEntity<Map<String, Object>> getBrowseHistoryPage(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        log.info("Getting browse history page for user: {}, page: {}, pageSize: {}", userId, page, pageSize);
        List<Long> postIds = browseHistoryService.getBrowseHistory(userId, page, pageSize);
        Long totalCount = browseHistoryService.getBrowseHistoryCount(userId);
        
        Map<String, Object> data = new HashMap<>();
        data.put("postIds", postIds);
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("total", totalCount);
        data.put("totalPages", (totalCount + pageSize - 1) / pageSize);
        return ResponseUtil.success(data);
    }

    /**
     * 按时间范围查询浏览记录
     * GET /api/browse-history/user/{userId}/range?startTime=xxx&endTime=xxx
     */
    @GetMapping("/user/{userId}/range")
    public ResponseEntity<Map<String, Object>> getBrowseHistoryByTimeRange(
            @PathVariable Long userId,
            @RequestParam long startTime,
            @RequestParam long endTime) {
        log.info("Getting browse history by time range for user: {}, startTime: {}, endTime: {}", 
                userId, startTime, endTime);
        List<Long> postIds = browseHistoryService.getBrowseHistoryByTimeRange(userId, startTime, endTime);
        
        Map<String, Object> data = new HashMap<>();
        data.put("postIds", postIds);
        data.put("count", postIds.size());
        return ResponseUtil.success(data);
    }

    /**
     * 检查是否浏览过指定帖子
     * GET /api/browse-history/user/{userId}/post/{postId}/has-browsed
     */
    @GetMapping("/user/{userId}/post/{postId}/has-browsed")
    public ResponseEntity<Map<String, Object>> hasBrowsed(
            @PathVariable Long userId,
            @PathVariable Long postId) {
        log.info("Checking if user {} has browsed post {}", userId, postId);
        boolean hasBrowsed = browseHistoryService.hasBrowsed(userId, postId);
        
        Map<String, Object> data = new HashMap<>();
        data.put("hasBrowsed", hasBrowsed);
        return ResponseUtil.success(data);
    }

    /**
     * 获取浏览时间
     * GET /api/browse-history/user/{userId}/post/{postId}/time
     */
    @GetMapping("/user/{userId}/post/{postId}/time")
    public ResponseEntity<Map<String, Object>> getBrowseTime(
            @PathVariable Long userId,
            @PathVariable Long postId) {
        log.info("Getting browse time for user {} and post {}", userId, postId);
        Long browseTime = browseHistoryService.getBrowseTime(userId, postId);
        
        Map<String, Object> data = new HashMap<>();
        data.put("browseTime", browseTime);
        return ResponseUtil.success(data);
    }

    /**
     * 获取浏览历史总数
     * GET /api/browse-history/user/{userId}/count
     */
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Map<String, Object>> getBrowseHistoryCount(@PathVariable Long userId) {
        log.info("Getting browse history count for user: {}", userId);
        Long count = browseHistoryService.getBrowseHistoryCount(userId);
        
        Map<String, Object> data = new HashMap<>();
        data.put("count", count);
        return ResponseUtil.success(data);
    }

    /**
     * 清除浏览历史
     * DELETE /api/browse-history/user/{userId}
     */
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> clearBrowseHistory(@PathVariable Long userId) {
        log.info("Clearing browse history for user: {}", userId);
        browseHistoryService.clearBrowseHistory(userId);
        return ResponseUtil.success("浏览历史已清除");
    }

    /**
     * 清除指定时间之前的浏览记录
     * DELETE /api/browse-history/user/{userId}/old?expireTime=xxx
     */
    @DeleteMapping("/user/{userId}/old")
    public ResponseEntity<Map<String, Object>> clearOldBrowseHistory(
            @PathVariable Long userId,
            @RequestParam long expireTime) {
        log.info("Clearing old browse history for user: {}, expireTime: {}", userId, expireTime);
        browseHistoryService.clearOldBrowseHistory(userId, expireTime);
        return ResponseUtil.success("旧浏览记录已清除");
    }

    /**
     * 记录浏览请求体
     */
    @Data
    public static class RecordBrowseRequest {
        private Long userId;
        private Long postId;
    }
}

