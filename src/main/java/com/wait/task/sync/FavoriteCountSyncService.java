package com.wait.task.sync;

import java.util.List;

import org.springframework.stereotype.Service;

import com.wait.entity.domain.PostFavorite;
import com.wait.mapper.PostFavoriteMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 收藏数同步服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteCountSyncService implements DataSyncService<PostFavorite> {

    private final PostFavoriteMapper postFavoriteMapper;

    private static final String RANKING_FAVORITES = "post:ranking:favorites";

    @Override
    public String getRedisKey() {
        return RANKING_FAVORITES;
    }

    @Override
    public List<PostFavorite> queryDataFromDatabase() {
        return postFavoriteMapper.selectAllPostFavoriteCounts();
    }

    @Override
    public Long extractResourceId(PostFavorite data) {
        return data.getPostId();
    }

    @Override
    public Integer extractCount(PostFavorite data) {
        // 注意：selectAllPostFavoriteCounts返回的PostFavorite对象中，id字段存储收藏数（SQL中COUNT(*)映射到id）
        return data.getId() != null ? data.getId().intValue() : 0;
    }

    @Override
    public String getTaskName() {
        return "Favorite Count Sync";
    }
}

