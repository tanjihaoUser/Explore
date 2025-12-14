package com.wait.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.wait.entity.domain.Comment;

@Mapper
public interface CommentMapper {

    int insert(Comment comment);

    Comment selectById(@Param("id") Long id);

    /**
     * 查询帖子的评论列表（按创建时间倒序）
     */
    List<Comment> selectByPostId(@Param("postId") Long postId, 
                                  @Param("offset") int offset, 
                                  @Param("limit") int limit);

    /**
     * 查询帖子的顶级评论（parent_id为NULL）
     */
    List<Comment> selectTopLevelByPostId(@Param("postId") Long postId,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    /**
     * 查询子评论（回复）
     */
    List<Comment> selectRepliesByParentId(@Param("parentId") Long parentId);

    /**
     * 查询用户的评论列表
     */
    List<Comment> selectByUserId(@Param("userId") Long userId,
                                 @Param("offset") int offset,
                                 @Param("limit") int limit);

    /**
     * 更新评论内容
     */
    int update(Comment comment);

    /**
     * 删除评论（逻辑删除）
     */
    int delete(@Param("id") Long id, @Param("updatedAt") Long updatedAt);

    /**
     * 增加点赞数
     */
    int incrementLikeCount(@Param("id") Long id);

    /**
     * 减少点赞数
     */
    int decrementLikeCount(@Param("id") Long id);

    /**
     * 查询帖子的评论数
     */
    int countByPostId(@Param("postId") Long postId);

    /**
     * 批量查询帖子的评论数
     */
    List<Comment> countByPostIds(@Param("postIds") List<Long> postIds);

    /**
     * 查询所有有评论的帖子ID和评论数（用于定时同步）
     * @return 帖子ID和评论数的映射（Comment对象中postId存储帖子ID，likeCount存储评论数）
     */
    List<Comment> selectAllPostCommentCounts();
}

