package com.wait.service;

import java.util.List;

import com.wait.entity.domain.Post;

/**
 * 帖子服务接口
 * 提供帖子相关的业务操作
 */
public interface PostService {

    /**
     * 创建新帖子
     * @param post 帖子对象
     * @return 帖子ID
     */
    Long insert(Post post);

    /**
     * 分页查询用户帖子
     * @param userId 用户ID
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @return 帖子列表
     */
    List<Post> getUserPagedPosts(Long userId, int page, int pageSize);

    /**
     * 根据ID获取单个帖子详情
     * @param postId 帖子ID
     * @return 帖子对象，如果不存在则返回null
     */
    Post getPostById(Long postId);

    /**
     * 批量获取帖子详情
     * @param postIds 帖子ID列表
     * @return 帖子列表
     */
    List<Post> getPostsByIds(List<Long> postIds);

    /**
     * 批量获取帖子详情，并填充当前用户的点赞、收藏状态和统计数据
     * @param postIds 帖子ID列表
     * @param currentUserId 当前用户ID（可为null，如果为null则不填充关系信息）
     * @return 帖子列表（包含点赞、收藏状态和统计数据）
     */
    List<Post> getPostsByIdsWithRelation(List<Long> postIds, Long currentUserId);

    /**
     * 更新帖子
     * @param post 帖子对象
     * @return 更新的行数
     */
    int update(Post post);

    /**
     * 评论数变化时调用，更新排行榜和热度分数
     * @param postId 帖子ID
     */
    void onCommentCountChanged(Long postId);

    /**
     * 增加评论数
     * @param postId 帖子ID
     */
    void incrementCommentCount(Long postId);

    /**
     * 减少评论数
     * @param postId 帖子ID
     */
    void decrementCommentCount(Long postId);

    /**
     * 删除帖子
     * @param userId 用户ID
     * @param postId 帖子ID
     * @return 删除的行数
     */
    int delete(Long userId, Long postId);
}

