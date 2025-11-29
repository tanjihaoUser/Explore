package com.wait.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wait.entity.domain.Post;
import com.wait.service.PostService;
import com.wait.util.ResponseUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 使用list存储用户帖子之间的关系
 */
@Slf4j
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createPost(@RequestBody Post post) {
        log.info("Creating post for user: {}", post.getUserId());
        Long postId = postService.insert(post);
        Map<String, Object> extraFields = new HashMap<>();
        extraFields.put("postId", postId);
        return ResponseUtil.success(extraFields, post);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<Map<String, Object>> getPostById(@PathVariable Long postId) {
        log.info("Getting post by id: {}", postId);
        Post post = postService.getPostById(postId);
        if (post == null) {
            return ResponseUtil.notFound("帖子不存在或已被删除");
        }
        return ResponseUtil.success(post);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getPostsByUserId(
            @PathVariable Long userId,
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) Long currentUserId) {
        log.info("Getting posts for user: {}, page: {}, size: {}, currentUserId: {}", userId, page, size,
                currentUserId);
        List<Post> posts = postService.getUserPagedPosts(userId, page, size);

        // 如果提供了当前用户ID，填充关系数据
        if (currentUserId != null && !posts.isEmpty()) {
            List<Long> postIds = posts.stream()
                    .map(Post::getId)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toList());
            List<Post> postsWithRelation = postService.getPostsByIdsWithRelation(postIds, currentUserId);

            // 保持原有顺序，更新关系数据
            java.util.Map<Long, Post> relationMap = postsWithRelation.stream()
                    .collect(java.util.stream.Collectors.toMap(Post::getId, p -> p));
            for (Post post : posts) {
                Post postWithRelation = relationMap.get(post.getId());
                if (postWithRelation != null) {
                    post.setIsLiked(postWithRelation.getIsLiked());
                    post.setIsFavorited(postWithRelation.getIsFavorited());
                    post.setLikeCount(postWithRelation.getLikeCount());
                    post.setFavoriteCount(postWithRelation.getFavoriteCount());
                    post.setCommentCount(postWithRelation.getCommentCount());
                    post.setUsername(postWithRelation.getUsername());
                }
            }
        }

        return ResponseUtil.success(posts);
    }

    @PutMapping("")
    public ResponseEntity<Map<String, Object>> updatePost(
            @RequestBody Post post) {
        log.info("Updating post: {}", post.getId());
        int rowsAffected = postService.update(post);
        Map<String, Object> data = new HashMap<>();
        data.put("rowsAffected", rowsAffected);
        return ResponseUtil.success(data);
    }

    @DeleteMapping()
    public ResponseEntity<Map<String, Object>> deletePost(@RequestParam("userId") Long userId,
            @RequestParam("postId") Long postId) {
        log.info("Deleting post: {}, user: {}", postId, userId);
        int rowsAffected = postService.delete(userId, postId);
        Map<String, Object> data = new HashMap<>();
        data.put("rowsAffected", rowsAffected);
        return ResponseUtil.success("帖子删除成功", data);
    }
}
