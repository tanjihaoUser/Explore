package com.wait.service.impl;

import static java.time.temporal.ChronoUnit.DAYS;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.wait.entity.domain.Post;
import com.wait.entity.domain.UVStatistics;
import com.wait.entity.type.ResourceType;
import com.wait.mapper.UVStatisticsMapper;
import com.wait.service.PostService;
import com.wait.service.RankingService;
import com.wait.service.UVStatisticsService;
import com.wait.util.BoundUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 独立访客统计服务实现
 * 使用 Redis Set 统计独立访客数量
 * 
 * 数据存储策略：
 * - Redis：存储最近7天的 UV 数据（热数据，快速访问）
 * - 数据库：存储7天前的 UV 数据（冷数据，持久化存储）
 * 
 * Redis 命令使用：
 * - SADD: 记录访问（自动去重）
 * - SCARD: 获取独立访客数
 * - SMEMBERS: 获取所有访客
 * - SISMEMBER: 检查是否已访问
 * - DEL: 删除过期的 key
 */
@Slf4j
@Service
public class UVStatisticsServiceImpl implements UVStatisticsService {

    private final BoundUtil boundUtil;
    private final UVStatisticsMapper uvStatisticsMapper;
    private final PostService postService;
    private final RankingService rankingService;

    public UVStatisticsServiceImpl(BoundUtil boundUtil, UVStatisticsMapper uvStatisticsMapper,
            @Lazy PostService postService, RankingService rankingService) {
        this.boundUtil = boundUtil;
        this.uvStatisticsMapper = uvStatisticsMapper;
        this.postService = postService;
        this.rankingService = rankingService;
    }

    private static final String UV_DAILY_PREFIX = "uv:daily:";
    private static final int KEEP_DAYS = 7; // Redis中保留最近7天的数据
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public Boolean recordVisit(ResourceType resourceType, Long resourceId, String visitorId) {
        if (resourceType == null || resourceId == null || visitorId == null) {
            throw new IllegalArgumentException("Resource type, resource ID and visitor ID cannot be null");
        }

        // 获取当前日期（格式：yyyyMMdd）
        String date = LocalDate.now().format(DATE_FORMATTER);

        // 调用 recordDailyVisit 记录当日访问
        return recordDailyVisit(resourceType.getCode(), resourceId, date, visitorId);
    }

    @Override
    public Long getUV(ResourceType resourceType, Long resourceId) {
        if (resourceType == null || resourceId == null) {
            return 0L;
        }

        // 1. 从 Redis 获取最近7天的所有访客（合并所有日期，自动去重）
        Set<String> redisVisitors = new HashSet<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < KEEP_DAYS; i++) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.format(DATE_FORMATTER);
            String key = UV_DAILY_PREFIX + resourceType.getCode() + ":" + resourceId + ":" + dateStr;
            Set<String> dailyVisitors = boundUtil.sMembers(key, String.class);
            if (dailyVisitors != null) {
                redisVisitors.addAll(dailyVisitors);
            }
        }

        // 2. 从数据库获取所有历史访客（从最早开始，确保获取创建账户以来的总流量）
        // 注意：数据库存储的是7天前的数据，所以这里查询所有历史数据
        String startDate = "00000000"; // 最早日期，确保获取所有历史数据
        String endDate = "99999999"; // 未来日期，确保获取所有历史数据

        Long dbUV = uvStatisticsMapper.countUVByDateRange(
                resourceType.getCode(), resourceId, startDate, endDate);
        if (dbUV == null) {
            dbUV = 0L;
        }

        // 3. 合并结果并去重
        // 由于定时任务会在数据超过7天后将其持久化到数据库并从 Redis 删除，
        // 理论上 Redis 和数据库不应该有重叠。但为了确保准确性，我们需要合并去重。
        // 如果数据库返回的是去重后的数量，而 Redis 中的访客可能也在数据库中，
        // 我们需要获取数据库中的访客ID列表来进行精确去重。
        //
        // 当前实现：简单相加（适用于数据已经正确持久化且无重叠的情况）
        // 如果需要精确去重，可以查询数据库中的访客ID列表，然后与 Redis 合并去重

        // 优化：如果数据库中有数据，查询具体的访客ID列表进行精确去重
        if (dbUV > 0) {
            // 查询数据库中的所有访客ID（从最早开始，确保获取所有历史数据）
            List<UVStatistics> dbStatistics = uvStatisticsMapper.selectByResourceAndDateRange(
                    resourceType.getCode(), resourceId, startDate, endDate);

            Set<String> dbVisitors = new HashSet<>();
            if (dbStatistics != null) {
                for (UVStatistics stat : dbStatistics) {
                    if (stat.getVisitorId() != null) {
                        dbVisitors.add(stat.getVisitorId());
                    }
                }
            }

            // 合并 Redis 和数据库的访客，自动去重
            Set<String> allVisitors = new HashSet<>(redisVisitors);
            allVisitors.addAll(dbVisitors);

            return (long) allVisitors.size();
        } else {
            // 数据库中没有数据，只返回 Redis 中的访客数
            return (long) redisVisitors.size();
        }
    }

    @Override
    public Long getDailyUV(String resourceType, Long resourceId, String date) {
        if (resourceType == null || resourceId == null || date == null) {
            return 0L;
        }

        // 判断日期是否在最近7天内
        LocalDate targetDate = LocalDate.parse(date, DATE_FORMATTER);
        LocalDate today = LocalDate.now();
        long daysDiff = DAYS.between(targetDate, today);

        if (daysDiff < KEEP_DAYS) {
            // 在 Redis 中查询
            String key = UV_DAILY_PREFIX + resourceType + ":" + resourceId + ":" + date;
            Long count = boundUtil.sCard(key);
            return count != null ? count : 0L;
        } else {
            // 从数据库查询
            Long count = uvStatisticsMapper.countDailyUV(resourceType, resourceId, date);
            return count != null ? count : 0L;
        }
    }

    @Override
    public Boolean recordDailyVisit(String resourceType, Long resourceId, String date, String visitorId) {
        if (resourceType == null || resourceId == null || date == null || visitorId == null) {
            throw new IllegalArgumentException("All parameters cannot be null");
        }

        String key = UV_DAILY_PREFIX + resourceType + ":" + resourceId + ":" + date;
        Long added = boundUtil.sAdd(key, visitorId);

        // added > 0 表示是新访客（首次访问）
        boolean isNewVisitor = added != null && added > 0;

        if (isNewVisitor) {
            log.debug("New daily visitor recorded: type={}, resourceId={}, date={}, visitorId={}",
                    resourceType, resourceId, date, visitorId);
        }

        return isNewVisitor;
    }

    @Override
    public Long mergeUV(String resourceType, Long resourceId, Set<String> dates) {
        if (resourceType == null || resourceId == null || dates == null || dates.isEmpty()) {
            return 0L;
        }

        // 合并多天的 UV 数据（去重）
        // 使用场景：统计一周、一个月等时间范围内的独立访客数
        Set<String> mergedVisitors = new HashSet<>();
        LocalDate today = LocalDate.now();

        for (String date : dates) {
            try {
                LocalDate targetDate = LocalDate.parse(date, DATE_FORMATTER);
                long daysDiff = DAYS.between(targetDate, today);

                if (daysDiff < KEEP_DAYS) {
                    // 从 Redis 查询
                    String key = UV_DAILY_PREFIX + resourceType + ":" + resourceId + ":" + date;
                    Set<String> visitors = boundUtil.sMembers(key, String.class);
                    if (visitors != null) {
                        mergedVisitors.addAll(visitors);
                    }
                } else {
                    // 从数据库查询
                    List<UVStatistics> statistics = uvStatisticsMapper.selectByResourceAndDate(
                            resourceType, resourceId, date);
                    if (statistics != null) {
                        for (UVStatistics stat : statistics) {
                            if (stat.getVisitorId() != null) {
                                mergedVisitors.add(stat.getVisitorId());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to merge UV for date: {}, type={}, resourceId={}",
                        date, resourceType, resourceId, e);
            }
        }

        Long count = (long) mergedVisitors.size();
        log.debug("Merged UV for {} dates: type={}, resourceId={}, count={}",
                dates.size(), resourceType, resourceId, count);

        return count;
    }

    @Override
    public Boolean hasVisited(String resourceType, Long resourceId, String visitorId) {
        if (resourceType == null || resourceId == null || visitorId == null) {
            return false;
        }

        // 先检查最近7天的 Redis 数据
        LocalDate today = LocalDate.now();
        for (int i = 0; i < KEEP_DAYS; i++) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.format(DATE_FORMATTER);
            String key = UV_DAILY_PREFIX + resourceType + ":" + resourceId + ":" + dateStr;
            Boolean isMember = boundUtil.sIsMember(key, visitorId);
            if (Boolean.TRUE.equals(isMember)) {
                return true;
            }
        }

        // 检查数据库（7天前的数据）
        LocalDate sevenDaysAgo = today.minusDays(KEEP_DAYS);
        String startDate = sevenDaysAgo.format(DATE_FORMATTER);
        List<UVStatistics> statistics = uvStatisticsMapper.selectByResourceAndDateRange(
                resourceType, resourceId, "00000000", startDate);
        if (statistics != null) {
            for (UVStatistics stat : statistics) {
                if (visitorId.equals(stat.getVisitorId())) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public List<Map<String, Object>> getUserPostsUV(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }

        try {
            // 获取用户的所有帖子（获取前100个，避免数据过多）
            List<Post> posts = postService.getUserPagedPosts(userId, 1, 100);

            List<Map<String, Object>> result = new ArrayList<>();
            for (Post post : posts) {
                Long postId = post.getId();
                Map<String, Object> postUV = new HashMap<>();
                postUV.put("postId", postId);
                postUV.put("content", post.getContent() != null && post.getContent().length() > 50
                        ? post.getContent().substring(0, 50) + "..."
                        : post.getContent());

                // UV统计
                postUV.put("totalUV", getUV(ResourceType.POST, postId));

                // 点赞、收藏、评论数量（总量）
                Long likeCount = rankingService.getLikeCount(postId);
                Long favoriteCount = rankingService.getFavoriteCount(postId);
                Long commentCount = rankingService.getCommentCount(postId);

                postUV.put("likeCount", likeCount != null ? likeCount : 0L);
                postUV.put("favoriteCount", favoriteCount != null ? favoriteCount : 0L);
                postUV.put("commentCount", commentCount != null ? commentCount : 0L);

                postUV.put("createdAt", post.getCreatedAt());
                result.add(postUV);
            }

            return result;
        } catch (Exception e) {
            log.error("Failed to get user posts UV: userId={}", userId, e);
            return new ArrayList<>();
        }
    }

}
