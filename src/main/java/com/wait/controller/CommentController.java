package com.wait.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wait.entity.domain.Comment;
import com.wait.entity.param.CommentRequest;
import com.wait.service.CommentService;
import com.wait.util.ResponseUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 评论控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 发表评论
     * POST /comments
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createComment(@RequestBody CommentRequest request) {
        log.info("User {} creating comment on post {}", request.getUserId(), request.getPostId());
        Comment comment = commentService.createComment(
                request.getUserId(),
                request.getPostId(),
                request.getContent(),
                request.getParentId());

        Map<String, Object> data = new HashMap<>();
        data.put("comment", comment);

        return ResponseUtil.success("评论成功", data);
    }

    /**
     * 删除评论
     * DELETE /comments/{commentId}
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Map<String, Object>> deleteComment(
            @PathVariable Long commentId,
            @RequestParam Long userId) {
        log.info("User {} deleting comment {}", userId, commentId);
        boolean success = commentService.deleteComment(commentId, userId);

        Map<String, Object> data = new HashMap<>();
        data.put("success", success);
        data.put("commentId", commentId);

        return ResponseUtil.success(success ? "删除成功" : "删除失败", data);
    }

    /**
     * 获取帖子的评论列表
     * GET /comments/post/{postId}
     */
    @GetMapping("/post/{postId}")
    public ResponseEntity<Map<String, Object>> getPostComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        log.info("Getting comments for post: {}, page: {}, size: {}", postId, page, pageSize);
        List<Comment> comments = commentService.getPostComments(postId, page, pageSize);

        Map<String, Object> data = new HashMap<>();
        data.put("postId", postId);
        data.put("comments", comments);
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("count", comments.size());

        return ResponseUtil.success(data);
    }

    /**
     * 获取帖子的顶级评论列表
     * GET /comments/post/{postId}/top-level
     */
    @GetMapping("/post/{postId}/top-level")
    public ResponseEntity<Map<String, Object>> getTopLevelComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        log.info("Getting top-level comments for post: {}, page: {}, size: {}", postId, page, pageSize);
        List<Comment> comments = commentService.getTopLevelComments(postId, page, pageSize);

        Map<String, Object> data = new HashMap<>();
        data.put("postId", postId);
        data.put("comments", comments);
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("count", comments.size());

        return ResponseUtil.success(data);
    }

    /**
     * 获取评论的回复列表
     * GET /comments/{commentId}/replies
     */
    @GetMapping("/{commentId}/replies")
    public ResponseEntity<Map<String, Object>> getCommentReplies(@PathVariable Long commentId) {
        log.info("Getting replies for comment: {}", commentId);
        List<Comment> replies = commentService.getCommentReplies(commentId);

        Map<String, Object> data = new HashMap<>();
        data.put("commentId", commentId);
        data.put("replies", replies);
        data.put("count", replies.size());

        return ResponseUtil.success(data);
    }

    /**
     * 获取用户的评论列表
     * GET /comments/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserComments(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        log.info("Getting comments for user: {}, page: {}, size: {}", userId, page, pageSize);
        List<Comment> comments = commentService.getUserComments(userId, page, pageSize);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("comments", comments);
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("count", comments.size());

        return ResponseUtil.success(data);
    }

    /**
     * 获取评论详情
     * GET /comments/{commentId}
     */
    @GetMapping("/{commentId}")
    public ResponseEntity<Map<String, Object>> getCommentById(@PathVariable Long commentId) {
        Comment comment = commentService.getCommentById(commentId);

        Map<String, Object> data = new HashMap<>();
        data.put("comment", comment);

        return ResponseUtil.success(data);
    }

    /**
     * 获取帖子的评论数
     * GET /comments/post/{postId}/count
     */
    @GetMapping("/post/{postId}/count")
    public ResponseEntity<Map<String, Object>> getCommentCount(@PathVariable Long postId) {
        int count = commentService.getCommentCount(postId);

        Map<String, Object> data = new HashMap<>();
        data.put("postId", postId);
        data.put("count", count);

        return ResponseUtil.success(data);
    }
}
