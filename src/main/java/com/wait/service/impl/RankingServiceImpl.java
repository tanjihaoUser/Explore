package com.wait.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.wait.service.RankingService;
import com.wait.util.BoundUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 单项排行榜服务实现
 * 实现按点赞数、收藏数、评论数排序的排行榜
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final BoundUtil boundUtil;

    private static final String RANKING_LIKES = "post:ranking:likes";
    private static final String RANKING_FAVORITES = "post:ranking:favorites";
    private static final String RANKING_COMMENTS = "post:ranking:comments";

    @Override
    public void onLike(Long postId) {
        boundUtil.zIncrBy(RANKING_LIKES, postId, 1.0);
        log.debug("Updated likes ranking for post {}", postId);
    }

    @Override
    public void onUnlike(Long postId) {
        boundUtil.zIncrBy(RANKING_LIKES, postId, -1.0);
        log.debug("Updated likes ranking for post {} (unlike)", postId);
    }

    @Override
    public void onFavorite(Long postId) {
        boundUtil.zIncrBy(RANKING_FAVORITES, postId, 1.0);
        log.debug("Updated favorites ranking for post {}", postId);
    }

    @Override
    public void onUnfavorite(Long postId) {
        boundUtil.zIncrBy(RANKING_FAVORITES, postId, -1.0);
        log.debug("Updated favorites ranking for post {} (unfavorite)", postId);
    }

    @Override
    public void onComment(Long postId) {
        boundUtil.zIncrBy(RANKING_COMMENTS, postId, 1.0);
        log.debug("Updated comments ranking for post {}", postId);
    }

    @Override
    public void onUncomment(Long postId) {
        boundUtil.zIncrBy(RANKING_COMMENTS, postId, -1.0);
        log.debug("Updated comments ranking for post {} (uncomment)", postId);
    }

    @Override
    public List<Long> getLikesRanking(int page, int pageSize) {
        long start = (page - 1) * pageSize;
        long end = start + pageSize - 1;
        List<Long> postIds = boundUtil.zReverseRange(RANKING_LIKES, start, end, Long.class);
        return postIds != null ? postIds : new ArrayList<>();
    }

    @Override
    public List<Long> getFavoritesRanking(int page, int pageSize) {
        long start = (page - 1) * pageSize;
        long end = start + pageSize - 1;
        List<Long> postIds = boundUtil.zReverseRange(RANKING_FAVORITES, start, end, Long.class);
        return postIds != null ? postIds : new ArrayList<>();
    }

    @Override
    public List<Long> getCommentsRanking(int page, int pageSize) {
        long start = (page - 1) * pageSize;
        long end = start + pageSize - 1;
        List<Long> postIds = boundUtil.zReverseRange(RANKING_COMMENTS, start, end, Long.class);
        return postIds != null ? postIds : new ArrayList<>();
    }

    @Override
    public Long getLikeCount(Long postId) {
        if (postId == null) {
            return 0L;
        }
        Double score = boundUtil.zScore(RANKING_LIKES, postId);
        return score != null ? score.longValue() : 0L;
    }

    @Override
    public Long getFavoriteCount(Long postId) {
        if (postId == null) {
            return 0L;
        }
        Double score = boundUtil.zScore(RANKING_FAVORITES, postId);
        return score != null ? score.longValue() : 0L;
    }

    @Override
    public Long getCommentCount(Long postId) {
        if (postId == null) {
            return 0L;
        }
        Double score = boundUtil.zScore(RANKING_COMMENTS, postId);
        return score != null ? score.longValue() : 0L;
    }
}
