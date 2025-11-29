package com.wait.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.wait.entity.domain.Post;
import com.wait.mapper.PostMapper;
import com.wait.service.HotRankingService;
import com.wait.util.BoundUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 热度排行榜服务实现
 * 使用 Sorted Set 实现综合热度排行榜
 * 
 * 热度算法：热度 = 点赞数 × 0.4 + 收藏数 × 0.3 + 评论数 × 0.2 + 分享数 × 0.1
 * 支持多个时间段：daily（日榜）、weekly（周榜）、monthly（月榜）、alltime（总榜）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotRankingServiceImpl implements HotRankingService {

    private final BoundUtil boundUtil;
    private final PostMapper postMapper;

    // 热度权重配置
    private static final double LIKE_WEIGHT = 0.4;        // 点赞权重
    private static final double FAVORITE_WEIGHT = 0.3;    // 收藏权重
    private static final double COMMENT_WEIGHT = 0.2;     // 评论权重
    private static final double SHARE_WEIGHT = 0.1;       // 分享权重（暂未实现）

    // Redis Key前缀
    private static final String HOT_RANKING_PREFIX = "post:ranking:hot:";
    
    // 时间加成配置（可选，用于提升新帖子的热度）
    @SuppressWarnings("unused")
    private static final int TIME_BOOST_HOURS = 24;       // 24小时内发布的内容有加成
    @SuppressWarnings("unused")
    private static final double TIME_BOOST_RATIO = 1.2;   // 加成比例

    @Override
    public void updateHotScore(Long postId) {
        if (postId == null) {
            log.warn("Post ID is null, cannot update hot score");
            return;
        }

        try {
            // 从数据库获取帖子统计信息
            Post post = postMapper.selectById(postId);
            if (post == null) {
                log.warn("Post {} not found, cannot update hot score", postId);
                return;
            }

            // 获取各项统计数据
            int likeCount = post.getLikeCount() != null ? post.getLikeCount() : 0;
            int commentCount = post.getCommentCount() != null ? post.getCommentCount() : 0;
            
            // 从RankingService的Sorted Set获取收藏数（如果Redis中有）
            Double favoriteScore = boundUtil.zScore("post:ranking:favorites", postId);
            int favoriteCount = favoriteScore != null ? favoriteScore.intValue() : 0;
            
            // 如果Redis中没有收藏数，尝试从数据库获取（如果Post实体有收藏数字段）
            // 注意：当前Post实体没有收藏数字段，所以暂时只从Redis获取

            // 计算综合热度分数
            double hotScore = calculateHotScore(likeCount, favoriteCount, commentCount, 0);

            // 更新所有时间段的排行榜
            updateRankingForAllPeriods(postId, hotScore);

            log.debug("Updated hot score for post {}: likeCount={}, favoriteCount={}, commentCount={}, hotScore={}",
                    postId, likeCount, favoriteCount, commentCount, hotScore);

        } catch (Exception e) {
            log.error("Failed to update hot score for post {}", postId, e);
        }
    }

    @Override
    public void onLike(Long postId) {
        if (postId == null) {
            return;
        }
        updateHotScore(postId);
    }

    @Override
    public void onUnlike(Long postId) {
        if (postId == null) {
            return;
        }
        updateHotScore(postId);
    }

    @Override
    public void onFavorite(Long postId) {
        if (postId == null) {
            return;
        }
        updateHotScore(postId);
    }

    @Override
    public void onUnfavorite(Long postId) {
        if (postId == null) {
            return;
        }
        updateHotScore(postId);
    }

    @Override
    public void onComment(Long postId) {
        if (postId == null) {
            return;
        }
        updateHotScore(postId);
    }

    @Override
    public List<Long> getHotPosts(String period, int page, int pageSize) {
        String key = HOT_RANKING_PREFIX + period.toLowerCase();
        long start = (page - 1) * pageSize;
        long end = start + pageSize - 1;

        // 使用 zReverseRange 获取分数最高的帖子（热度从高到低）
        Set<Long> postIds = boundUtil.zReverseRange(key, start, end, Long.class);
        return new ArrayList<>(postIds != null ? postIds : new ArrayList<>());
    }

    @Override
    public Long getPostRank(Long postId, String period) {
        if (postId == null) {
            return null;
        }
        String key = HOT_RANKING_PREFIX + period.toLowerCase();
        
        // 使用 zRevRank 获取排名（0-based），转换为1-based
        Long rank = boundUtil.zRevRank(key, postId);
        return rank != null ? rank + 1 : null;
    }

    @Override
    public Double getHotScore(Long postId, String period) {
        if (postId == null) {
            return null;
        }
        String key = HOT_RANKING_PREFIX + period.toLowerCase();
        return boundUtil.zScore(key, postId);
    }

    /**
     * 计算综合热度分数
     * 热度 = 点赞数 × 0.4 + 收藏数 × 0.3 + 评论数 × 0.2 + 分享数 × 0.1
     */
    private double calculateHotScore(int likeCount, int favoriteCount, int commentCount, int shareCount) {
        return likeCount * LIKE_WEIGHT
                + favoriteCount * FAVORITE_WEIGHT
                + commentCount * COMMENT_WEIGHT
                + shareCount * SHARE_WEIGHT;
    }

    /**
     * 更新所有时间段的排行榜
     */
    private void updateRankingForAllPeriods(Long postId, double hotScore) {
        String[] periods = {"daily", "weekly", "monthly", "alltime"};
        for (String period : periods) {
            String key = HOT_RANKING_PREFIX + period;
            // 使用 ZADD 更新分数（如果不存在则添加，存在则更新）
            boundUtil.zAdd(key, postId, hotScore);
        }
    }

    /**
     * 定时清理过期数据（每天凌晨2点执行）
     * 可选功能：清理超过一定时间未更新的数据，避免排行榜过大
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredRankingData() {
        log.info("Starting to clean expired ranking data...");
        try {
            // 这里可以添加清理逻辑，比如删除30天前的数据
            // 当前实现暂不清理，保留所有历史数据
            log.info("Ranking data cleanup completed");
        } catch (Exception e) {
            log.error("Failed to clean expired ranking data", e);
        }
    }
}
