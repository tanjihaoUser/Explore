package com.wait.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wait.service.StatisticsService;
import com.wait.util.ResponseUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 统计控制器
 * 提供各种统计数据查询接口，用于前端绘制图表
 */
@Slf4j
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * 获取帖子浏览量统计
     * GET /api/statistics/post/{postId}/view
     * 
     * @param postId 帖子ID
     * @param hours 最近N小时（默认24小时）
     * @return 时间序列数据 [{time: timestamp, value: count}, ...]
     */
    @GetMapping("/post/{postId}/view")
    public ResponseEntity<Map<String, Object>> getPostViewStatistics(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "24") int hours) {
        
        try {
            List<Map<String, Object>> data = statisticsService.getPostViewStatistics(postId, hours);
            
            Map<String, Object> result = new HashMap<>();
            result.put("postId", postId);
            result.put("hours", hours);
            result.put("data", data);
            result.put("total", data.stream()
                    .mapToInt(point -> (Integer) point.get("value"))
                    .sum());
            
            return ResponseUtil.success("获取帖子浏览量统计成功", result);
        } catch (Exception e) {
            log.error("Failed to get post view statistics: postId={}, hours={}", postId, hours, e);
            return ResponseUtil.error("获取统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取主页访问量统计
     * GET /api/statistics/homepage/view
     * 
     * @param hours 最近N小时（默认24小时）
     * @return 时间序列数据
     */
    @GetMapping("/homepage/view")
    public ResponseEntity<Map<String, Object>> getHomePageViewStatistics(
            @RequestParam(defaultValue = "24") int hours) {
        
        try {
            List<Map<String, Object>> data = statisticsService.getHomePageViewStatistics(hours);
            
            Map<String, Object> result = new HashMap<>();
            result.put("hours", hours);
            result.put("data", data);
            result.put("total", data.stream()
                    .mapToInt(point -> (Integer) point.get("value"))
                    .sum());
            
            return ResponseUtil.success("获取主页访问量统计成功", result);
        } catch (Exception e) {
            log.error("Failed to get homepage view statistics: hours={}", hours, e);
            return ResponseUtil.error("获取统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取点赞变化曲线
     * GET /api/statistics/post/{postId}/like
     * 
     * @param postId 帖子ID（可选，不提供则统计所有帖子）
     * @param hours 最近N小时（默认24小时）
     * @return 时间序列数据（累计值）
     */
    @GetMapping("/post/{postId}/like")
    public ResponseEntity<Map<String, Object>> getLikeStatistics(
            @PathVariable(required = false) Long postId,
            @RequestParam(defaultValue = "24") int hours) {
        
        try {
            List<Map<String, Object>> data = statisticsService.getLikeStatistics(postId, hours);
            
            Map<String, Object> result = new HashMap<>();
            result.put("postId", postId != null ? postId : "all");
            result.put("hours", hours);
            result.put("data", data);
            result.put("current", data.isEmpty() ? 0 : (Integer) data.get(data.size() - 1).get("value"));
            
            return ResponseUtil.success("获取点赞统计成功", result);
        } catch (Exception e) {
            log.error("Failed to get like statistics: postId={}, hours={}", postId, hours, e);
            return ResponseUtil.error("获取统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有帖子的点赞统计
     * GET /api/statistics/post/like
     */
    @GetMapping("/post/like")
    public ResponseEntity<Map<String, Object>> getAllPostLikeStatistics(
            @RequestParam(defaultValue = "24") int hours) {
        return getLikeStatistics(null, hours);
    }

    /**
     * 获取收藏变化曲线
     * GET /api/statistics/post/{postId}/favorite
     * 
     * @param postId 帖子ID（可选，不提供则统计所有帖子）
     * @param hours 最近N小时（默认24小时）
     * @return 时间序列数据（累计值）
     */
    @GetMapping("/post/{postId}/favorite")
    public ResponseEntity<Map<String, Object>> getFavoriteStatistics(
            @PathVariable(required = false) Long postId,
            @RequestParam(defaultValue = "24") int hours) {
        
        try {
            List<Map<String, Object>> data = statisticsService.getFavoriteStatistics(postId, hours);
            
            Map<String, Object> result = new HashMap<>();
            result.put("postId", postId != null ? postId : "all");
            result.put("hours", hours);
            result.put("data", data);
            result.put("current", data.isEmpty() ? 0 : (Integer) data.get(data.size() - 1).get("value"));
            
            return ResponseUtil.success("获取收藏统计成功", result);
        } catch (Exception e) {
            log.error("Failed to get favorite statistics: postId={}, hours={}", postId, hours, e);
            return ResponseUtil.error("获取统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有帖子的收藏统计
     * GET /api/statistics/post/favorite
     */
    @GetMapping("/post/favorite")
    public ResponseEntity<Map<String, Object>> getAllPostFavoriteStatistics(
            @RequestParam(defaultValue = "24") int hours) {
        return getFavoriteStatistics(null, hours);
    }

    /**
     * 获取综合统计（包含浏览量、点赞、收藏）
     * GET /api/statistics/post/{postId}/comprehensive
     * 
     * @param postId 帖子ID（可选，不提供则统计所有帖子）
     * @param hours 最近N小时（默认24小时）
     * @return 综合统计数据
     */
    @GetMapping("/post/{postId}/comprehensive")
    public ResponseEntity<Map<String, Object>> getComprehensiveStatistics(
            @PathVariable(required = false) Long postId,
            @RequestParam(defaultValue = "24") int hours) {
        
        try {
            Map<String, Object> data = statisticsService.getComprehensiveStatistics(postId, hours);
            
            Map<String, Object> result = new HashMap<>();
            result.put("postId", postId != null ? postId : "all");
            result.put("hours", hours);
            result.put("statistics", data);
            
            return ResponseUtil.success("获取综合统计成功", result);
        } catch (Exception e) {
            log.error("Failed to get comprehensive statistics: postId={}, hours={}", postId, hours, e);
            return ResponseUtil.error("获取统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有帖子的综合统计
     * GET /api/statistics/post/comprehensive
     */
    @GetMapping("/post/comprehensive")
    public ResponseEntity<Map<String, Object>> getAllPostComprehensiveStatistics(
            @RequestParam(defaultValue = "24") int hours) {
        return getComprehensiveStatistics(null, hours);
    }
}

