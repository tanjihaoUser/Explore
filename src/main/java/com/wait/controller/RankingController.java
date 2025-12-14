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

import com.wait.entity.domain.Post;
import com.wait.service.HotRankingService;
import com.wait.service.PostService;
import com.wait.service.RankingService;
import com.wait.service.TimelineSortedSetService;
import com.wait.util.ResponseUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 排行榜和时间线控制器
 * 使用 Sorted Set 排行榜、时间线等功能
 */
@Slf4j
@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final HotRankingService hotRankingService;
    private final RankingService rankingService;
    private final TimelineSortedSetService timelineSortedSetService;
    private final PostService postService;

    // ==================== 时间线相关 ====================

    /**
     * 获取用户时间线
     * GET /ranking/timeline/user/{userId}
     */
    @GetMapping("/timeline/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserTimeline(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long currentUserId) {
        log.info("Getting timeline for user: {}, page: {}, size: {}, currentUserId: {}", userId, page, pageSize,
                currentUserId);

        // 1. 获取帖子 ID 列表
        List<Long> postIds = timelineSortedSetService.getUserTimeline(userId, page, pageSize);

        // 2. 根据 ID 列表获取完整帖子信息，并填充关系数据
        List<Post> posts = postService.getPostsByIdsWithRelation(postIds, currentUserId);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("posts", posts); // 返回完整帖子列表，包含点赞收藏状态
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("count", posts.size()); // 使用实际获取到的帖子数量

        return ResponseUtil.success(data);
    }

    /**
     * 获取全局时间线
     * GET /ranking/timeline/global
     */
    @GetMapping("/timeline/global")
    public ResponseEntity<Map<String, Object>> getGlobalTimeline(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long currentUserId) {
        log.info("Getting global timeline, page: {}, size: {}, currentUserId: {}", page, pageSize, currentUserId);

        // 1. 获取帖子 ID 列表
        List<Long> postIds = timelineSortedSetService.getGlobalTimeline(page, pageSize);
        log.debug("global timelines: {}", postIds);

        // 2. 根据 ID 列表获取完整帖子信息，并填充关系数据
        List<Post> posts = postService.getPostsByIdsWithRelation(postIds, currentUserId);

        Map<String, Object> data = new HashMap<>();
        data.put("posts", posts); // 返回完整帖子列表，包含点赞收藏状态
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("count", posts.size()); // 使用实际获取到的帖子数量

        return ResponseUtil.success(data);
    }

    /**
     * 获取指定时间范围内的帖子
     * GET /ranking/timeline/range
     */
    @GetMapping("/timeline/range")
    public ResponseEntity<Map<String, Object>> getPostsByTimeRange(
            @RequestParam(required = false) Long userId,
            @RequestParam long startTime,
            @RequestParam long endTime,
            @RequestParam(required = false) Long currentUserId) {
        log.info("Getting posts in time range: {} - {}, userId: {}, currentUserId: {}", startTime, endTime, userId,
                currentUserId);

        // 1. 获取帖子 ID 列表
        List<Long> postIds = timelineSortedSetService.getPostsByTimeRange(userId, startTime, endTime);

        // 2. 根据 ID 列表获取完整帖子信息，并填充关系数据
        List<Post> posts = postService.getPostsByIdsWithRelation(postIds, currentUserId);

        Map<String, Object> data = new HashMap<>();
        if (userId != null) {
            data.put("userId", userId);
        }
        data.put("posts", posts); // 返回完整帖子列表，包含点赞收藏状态
        data.put("startTime", startTime);
        data.put("endTime", endTime);
        data.put("count", posts.size()); // 使用实际获取到的帖子数量

        return ResponseUtil.success(data);
    }

    /**
     * 获取我的时间线（聚合关注用户的帖子，自动过滤黑名单用户）
     * GET /ranking/timeline/my/{userId}
     */
    @GetMapping("/timeline/my/{userId}")
    public ResponseEntity<Map<String, Object>> getMyTimeline(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long currentUserId) {
        log.info("Getting my timeline for user: {}, page: {}, size: {}, currentUserId: {}", userId, page, pageSize,
                currentUserId);

        // 1. 获取帖子 ID 列表（已过滤黑名单用户）
        List<Long> postIds = timelineSortedSetService.getMyTimeline(userId, page, pageSize);

        // 2. 根据 ID 列表获取完整帖子信息，并填充关系数据
        // 如果没有提供 currentUserId，使用 userId 作为当前用户
        Long actualCurrentUserId = currentUserId != null ? currentUserId : userId;
        List<Post> posts = postService.getPostsByIdsWithRelation(postIds, actualCurrentUserId);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("posts", posts); // 返回完整帖子列表，包含点赞收藏状态
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("count", posts.size()); // 使用实际获取到的帖子数量

        return ResponseUtil.success(data);
    }

    // ==================== 热度排行榜相关 ====================

    /**
     * 获取热门帖子排行榜
     * GET /ranking/hot
     */
    @GetMapping("/hot")
    public ResponseEntity<Map<String, Object>> getHotPosts(
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long currentUserId) {
        log.info("Getting hot posts, period: {}, page: {}, size: {}, currentUserId: {}", period, page, pageSize,
                currentUserId);

        // 1. 获取热门帖子 ID 列表
        List<Long> postIds = hotRankingService.getHotPosts(period, page, pageSize);

        // 2. 根据 ID 列表获取完整帖子信息，并填充关系数据
        List<Post> posts = postService.getPostsByIdsWithRelation(postIds, currentUserId);

        Map<String, Object> data = new HashMap<>();
        data.put("period", period);
        data.put("posts", posts); // 返回完整帖子列表，包含点赞收藏状态
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("count", posts.size()); // 使用实际获取到的帖子数量

        return ResponseUtil.success(data);
    }

    /**
     * 获取帖子在排行榜中的排名
     * GET /ranking/hot/{postId}/rank
     */
    @GetMapping("/hot/{postId}/rank")
    public ResponseEntity<Map<String, Object>> getPostRank(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "daily") String period) {
        log.info("Getting rank for post: {}, period: {}", postId, period);
        Long rank = hotRankingService.getPostRank(postId, period);
        Double hotScore = hotRankingService.getHotScore(postId, period);

        Map<String, Object> data = new HashMap<>();
        data.put("postId", postId);
        data.put("period", period);
        data.put("rank", rank);
        data.put("hotScore", hotScore);

        return ResponseUtil.success(data);
    }

    // ==================== 单项排行榜相关 ====================

    /**
     * 获取点赞数排行榜
     * GET /ranking/likes
     */
    @GetMapping("/likes")
    public ResponseEntity<Map<String, Object>> getLikesRanking(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long currentUserId) {
        log.info("Getting likes ranking, page: {}, size: {}, currentUserId: {}", page, pageSize, currentUserId);

        // 1. 获取点赞排行榜 ID 列表
        List<Long> postIds = rankingService.getLikesRanking(page, pageSize);

        // 2. 根据 ID 列表获取完整帖子信息，并填充关系数据
        List<Post> posts = postService.getPostsByIdsWithRelation(postIds, currentUserId);

        Map<String, Object> data = new HashMap<>();
        data.put("posts", posts); // 返回完整帖子列表，包含点赞收藏状态
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("count", posts.size()); // 使用实际获取到的帖子数量

        return ResponseUtil.success(data);
    }

    /**
     * 获取收藏数排行榜
     * GET /ranking/favorites
     */
    @GetMapping("/favorites")
    public ResponseEntity<Map<String, Object>> getFavoritesRanking(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long currentUserId) {
        log.info("Getting favorites ranking, page: {}, size: {}, currentUserId: {}", page, pageSize, currentUserId);

        // 1. 获取收藏排行榜 ID 列表
        List<Long> postIds = rankingService.getFavoritesRanking(page, pageSize);

        // 2. 根据 ID 列表获取完整帖子信息，并填充关系数据
        List<Post> posts = postService.getPostsByIdsWithRelation(postIds, currentUserId);

        Map<String, Object> data = new HashMap<>();
        data.put("posts", posts); // 返回完整帖子列表，包含点赞收藏状态
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("count", posts.size()); // 使用实际获取到的帖子数量

        return ResponseUtil.success(data);
    }

    /**
     * 获取评论数排行榜
     * GET /ranking/comments
     */
    @GetMapping("/comments")
    public ResponseEntity<Map<String, Object>> getCommentsRanking(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long currentUserId) {
        log.info("Getting comments ranking, page: {}, size: {}, currentUserId: {}", page, pageSize, currentUserId);

        // 1. 获取评论排行榜 ID 列表
        List<Long> postIds = rankingService.getCommentsRanking(page, pageSize);

        // 2. 根据 ID 列表获取完整帖子信息，并填充关系数据
        List<Post> posts = postService.getPostsByIdsWithRelation(postIds, currentUserId);

        Map<String, Object> data = new HashMap<>();
        data.put("posts", posts); // 返回完整帖子列表，包含点赞收藏状态
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("count", posts.size()); // 使用实际获取到的帖子数量

        return ResponseUtil.success(data);
    }
}
