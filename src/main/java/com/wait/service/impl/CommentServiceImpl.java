package com.wait.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wait.entity.domain.Comment;
import com.wait.entity.domain.Post;
import com.wait.mapper.CommentMapper;
import com.wait.mapper.PostMapper;
import com.wait.service.CommentService;
import com.wait.service.HotRankingService;
import com.wait.service.NotificationService;
import com.wait.service.RankingService;
import com.wait.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 评论服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final RankingService rankingService;
    private final HotRankingService hotRankingService;
    private final NotificationService notificationService;
    private final UserService userService;

    @Override
    @Transactional
    public Comment createComment(Long userId, Long postId, String content, Long parentId) {
        // 检查帖子是否存在
        Post post = postMapper.selectById(postId);
        if (post == null || post.getIsDeleted() != null && post.getIsDeleted() == 1) {
            throw new IllegalArgumentException("帖子不存在或已删除");
        }

        // 如果是回复，检查父评论是否存在
        if (parentId != null) {
            Comment parentComment = commentMapper.selectById(parentId);
            if (parentComment == null || parentComment.getIsDeleted() != null && parentComment.getIsDeleted() == 1) {
                throw new IllegalArgumentException("父评论不存在或已删除");
            }
            // 确保父评论属于同一个帖子
            if (!parentComment.getPostId().equals(postId)) {
                throw new IllegalArgumentException("父评论不属于该帖子");
            }
        }

        long now = System.currentTimeMillis();
        Comment comment = Comment.builder()
                .postId(postId)
                .userId(userId)
                .parentId(parentId)
                .content(content)
                .likeCount(0)
                .isDeleted(0)
                .createdAt(now)
                .updatedAt(now)
                .build();

        commentMapper.insert(comment);
        log.info("User {} created comment {} on post {}", userId, comment.getId(), postId);

        // 更新帖子的评论数
        Post updatePost = new Post();
        updatePost.setId(postId);
        updatePost.setCommentCount((post.getCommentCount() != null ? post.getCommentCount() : 0) + 1);
        postMapper.update(updatePost);

        // 更新排行榜和热度分数
        try {
            rankingService.onComment(postId);
            hotRankingService.onComment(postId);
            log.debug("Updated ranking and hot score for post {} due to comment", postId);
        } catch (Exception e) {
            log.error("Failed to update ranking/hot score for post {}", postId, e);
            // 不影响主流程
        }

        // 异步发送通知
        try {
            com.wait.entity.domain.UserBase commenter = userService.findById(userId);
            String commenterName = commenter != null && commenter.getUsername() != null
                    ? commenter.getUsername()
                    : "用户" + userId;

            if (parentId != null) {
                // 回复评论：通知被回复的用户
                Comment parentComment = commentMapper.selectById(parentId);
                if (parentComment != null) {
                    Long parentCommentAuthorId = parentComment.getUserId();
                    // 如果不是自己回复自己，发送通知
                    if (!parentCommentAuthorId.equals(userId)) {
                        notificationService.sendNotificationAsync(
                                parentCommentAuthorId,
                                "reply",
                                String.format("%s回复了你的评论", commenterName),
                                parentId);
                    }
                }
            } else {
                // 评论帖子：通知帖子作者
                Long postAuthorId = post.getUserId();
                // 如果不是自己评论自己的帖子，发送通知
                if (!postAuthorId.equals(userId)) {
                    notificationService.sendNotificationAsync(
                            postAuthorId,
                            "comment",
                            String.format("%s评论了你的帖子", commenterName),
                            postId);
                }
            }
        } catch (Exception e) {
            // 通知发送失败不影响主流程
            log.error("Failed to send comment notification: user={}, post={}, parent={}",
                    userId, postId, parentId, e);
        }

        return comment;
    }

    @Override
    @Transactional
    public boolean deleteComment(Long commentId, Long userId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            log.warn("Comment {} not found", commentId);
            return false;
        }

        // 检查权限：只能删除自己的评论
        if (!comment.getUserId().equals(userId)) {
            log.warn("User {} is not allowed to delete comment {}", userId, commentId);
            return false;
        }

        // 逻辑删除
        int deleted = commentMapper.delete(commentId, System.currentTimeMillis());
        if (deleted > 0) {
            // 更新帖子的评论数
            Post post = postMapper.selectById(comment.getPostId());
            if (post != null) {
                Post updatePost = new Post();
                updatePost.setId(comment.getPostId());
                updatePost.setCommentCount(
                        Math.max(0, (post.getCommentCount() != null ? post.getCommentCount() : 0) - 1));
                postMapper.update(updatePost);
            }

            log.info("Comment {} deleted by user {}", commentId, userId);
            return true;
        }

        return false;
    }

    @Override
    public List<Comment> getPostComments(Long postId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return commentMapper.selectByPostId(postId, offset, pageSize);
    }

    @Override
    public List<Comment> getTopLevelComments(Long postId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return commentMapper.selectTopLevelByPostId(postId, offset, pageSize);
    }

    @Override
    public List<Comment> getCommentReplies(Long parentCommentId) {
        return commentMapper.selectRepliesByParentId(parentCommentId);
    }

    @Override
    public List<Comment> getUserComments(Long userId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return commentMapper.selectByUserId(userId, offset, pageSize);
    }

    @Override
    public Comment getCommentById(Long commentId) {
        return commentMapper.selectById(commentId);
    }

    @Override
    public int getCommentCount(Long postId) {
        return commentMapper.countByPostId(postId);
    }

    @Override
    public Map<Long, Integer> batchGetCommentCounts(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return new HashMap<>();
        }

        // 这里简化处理，实际应该使用批量查询优化
        // 如果CommentMapper有批量查询方法，应该使用它
        Map<Long, Integer> result = new HashMap<>();
        for (Long postId : postIds) {
            result.put(postId, commentMapper.countByPostId(postId));
        }
        return result;
    }
}
