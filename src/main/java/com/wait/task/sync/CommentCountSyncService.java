package com.wait.task.sync;

import java.util.List;

import org.springframework.stereotype.Service;

import com.wait.entity.domain.Comment;
import com.wait.mapper.CommentMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 评论数同步服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentCountSyncService implements DataSyncService<Comment> {

    private final CommentMapper commentMapper;

    private static final String RANKING_COMMENTS = "post:ranking:comments";

    @Override
    public String getRedisKey() {
        return RANKING_COMMENTS;
    }

    @Override
    public List<Comment> queryDataFromDatabase() {
        return commentMapper.selectAllPostCommentCounts();
    }

    @Override
    public Long extractResourceId(Comment data) {
        return data.getPostId();
    }

    @Override
    public Integer extractCount(Comment data) {
        // 注意：selectAllPostCommentCounts返回的Comment对象中，likeCount字段存储评论数（SQL中COUNT(*)映射到likeCount）
        return data.getLikeCount() != null ? data.getLikeCount() : 0;
    }

    @Override
    public String getTaskName() {
        return "Comment Count Sync";
    }
}
