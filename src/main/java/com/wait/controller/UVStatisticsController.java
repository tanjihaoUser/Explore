package com.wait.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wait.entity.type.ResourceType;
import com.wait.service.StatisticsService;
import com.wait.service.UVStatisticsService;
import com.wait.util.ResponseUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * UV 统计控制器
 * 提供 UV 统计相关的 API 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/uv-statistics")
@RequiredArgsConstructor
public class UVStatisticsController {

    private final UVStatisticsService uvStatisticsService;
    private final StatisticsService statisticsService;

    /**
     * 获取资源的 UV 统计
     * GET /api/uv-statistics/uv
     * 
     * @param resourceType 资源类型（post, page, user_profile, category, search）
     * @param resourceId   资源ID
     * @return UV 统计
     */
    @GetMapping("/uv")
    public ResponseEntity<Map<String, Object>> getUV(
            @RequestParam String resourceType,
            @RequestParam Long resourceId) {
        try {
            ResourceType type = ResourceType.fromCode(resourceType);
            if (type == null) {
                return ResponseUtil.error("Invalid resource type: " + resourceType);
            }

            Long uv = uvStatisticsService.getUV(type, resourceId);
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("resourceType", resourceType);
            data.put("resourceId", resourceId);
            data.put("uv", uv);

            return ResponseUtil.success(data);
        } catch (Exception e) {
            log.error("Failed to get UV: resourceType={}, resourceId={}", resourceType, resourceId, e);
            return ResponseUtil.error("获取UV统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取一段时间内每天的 UV 统计
     * GET /api/uv-statistics/daily-uv
     * 
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @param startDate    开始日期（格式：yyyyMMdd）
     * @param endDate      结束日期（格式：yyyyMMdd）
     * @return 每天的 UV 统计
     */
    @GetMapping("/daily-uv")
    public ResponseEntity<Map<String, Object>> getDailyUVInRange(
            @RequestParam String resourceType,
            @RequestParam Long resourceId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            ResourceType type = ResourceType.fromCode(resourceType);
            if (type == null) {
                return ResponseUtil.error("Invalid resource type: " + resourceType);
            }

            Map<String, Long> dailyUV = statisticsService.getDailyUVInRange(type, resourceId, startDate, endDate);
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("resourceType", resourceType);
            data.put("resourceId", resourceId);
            data.put("startDate", startDate);
            data.put("endDate", endDate);
            data.put("dailyUV", dailyUV);

            return ResponseUtil.success(data);
        } catch (Exception e) {
            log.error("Failed to get daily UV in range: resourceType={}, resourceId={}, startDate={}, endDate={}",
                    resourceType, resourceId, startDate, endDate, e);
            return ResponseUtil.error("获取每日UV统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户所有帖子的 UV 统计
     * GET /api/uv-statistics/user-posts
     * 
     * @param userId 用户ID
     * @return 用户所有帖子的 UV 统计列表
     */
    @GetMapping("/user-posts")
    public ResponseEntity<Map<String, Object>> getUserPostsUV(@RequestParam Long userId) {
        try {
            List<Map<String, Object>> postsUV = uvStatisticsService.getUserPostsUV(userId);
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("userId", userId);
            data.put("posts", postsUV);
            data.put("totalPosts", postsUV.size());

            return ResponseUtil.success(data);
        } catch (Exception e) {
            log.error("Failed to get user posts UV: userId={}", userId, e);
            return ResponseUtil.error("获取用户帖子UV统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取帖子在一段时间内的统计数据（UV、点赞、收藏、评论）
     * GET /api/uv-statistics/post-statistics
     * 
     * @param postId    帖子ID
     * @param startDate 开始日期（格式：yyyyMMdd）
     * @param endDate   结束日期（格式：yyyyMMdd）
     * @return 统计数据
     */
    @GetMapping("/post-statistics")
    public ResponseEntity<Map<String, Object>> getPostStatisticsInRange(
            @RequestParam Long postId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            Map<String, Object> statistics = statisticsService.getPostStatisticsInRange(postId, startDate, endDate);
            Map<String, Object> data = new HashMap<>();
            data.put("postId", postId);
            data.put("startDate", startDate);
            data.put("endDate", endDate);
            data.putAll(statistics);

            return ResponseUtil.success(data);
        } catch (Exception e) {
            log.error("Failed to get post statistics in range: postId={}, startDate={}, endDate={}",
                    postId, startDate, endDate, e);
            return ResponseUtil.error("获取帖子统计数据失败: " + e.getMessage());
        }
    }
}
