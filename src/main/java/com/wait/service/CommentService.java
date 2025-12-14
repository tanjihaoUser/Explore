package com.wait.service;

import java.util.List;
import java.util.Map;

import com.wait.entity.domain.Comment;

/**
 * 评论服务接口
 */
public interface CommentService {

    /**
     * 发表评论
     */
    Comment createComment(Long userId, Long postId, String content, Long parentId);

    /**
     * 删除评论（逻辑删除）
     */
    boolean deleteComment(Long commentId, Long userId);

    /**
     * 获取帖子的评论列表
     */
    List<Comment> getPostComments(Long postId, int page, int pageSize);

    /**
     * 获取帖子的顶级评论列表
     */
    List<Comment> getTopLevelComments(Long postId, int page, int pageSize);

    /**
     * 获取评论的回复列表
     */
    List<Comment> getCommentReplies(Long parentCommentId);

    /**
     * 获取用户的评论列表
     */
    List<Comment> getUserComments(Long userId, int page, int pageSize);

    /**
     * 获取评论详情
     */
    Comment getCommentById(Long commentId);

    /**
     * 获取帖子的评论数
     */
    int getCommentCount(Long postId);

    /**
     * 批量获取帖子的评论数
     */
    Map<Long, Integer> batchGetCommentCounts(List<Long> postIds);
}

