package com.wait.task.sync;

import java.util.List;

import org.springframework.stereotype.Service;

import com.wait.entity.domain.PostLike;
import com.wait.mapper.PostLikeMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 点赞数同步服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LikeCountSyncService implements DataSyncService<PostLike> {

    private final PostLikeMapper postLikeMapper;

    private static final String RANKING_LIKES = "post:ranking:likes";

    @Override
    public String getRedisKey() {
        return RANKING_LIKES;
    }

    @Override
    public List<PostLike> queryDataFromDatabase() {
        return postLikeMapper.selectAllPostLikeCounts();
    }

    @Override
    public Long extractResourceId(PostLike data) {
        return data.getPostId();
    }

    @Override
    public Integer extractCount(PostLike data) {
        // 注意：selectAllPostLikeCounts返回的PostLike对象中，id字段存储点赞数（SQL中COUNT(*)映射到id）
        return data.getId() != null ? data.getId().intValue() : 0;
    }

    @Override
    public String getTaskName() {
        return "Like Count Sync";
    }
}

