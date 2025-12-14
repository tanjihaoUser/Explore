package com.wait.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.wait.entity.domain.PostLike;

@Mapper
public interface PostLikeMapper {

    int insert(PostLike postLike);

    /**
     * 批量插入（用于批量持久化）
     */
    int batchInsert(List<PostLike> likes);

    int delete(@Param("postId") Long postId, @Param("userId") Long userId);

    /**
     * 批量删除（用于数据修复）
     * 
     * @param deletes 待删除的点赞关系列表
     * @return 删除的记录数
     */
    int batchDelete(@Param("deletes") List<PostLike> deletes);

    boolean exists(@Param("postId") Long postId, @Param("userId") Long userId);

    /**
     * 批量查询存在性：返回已存在的 (postId, userId) 对
     * 用于批量写入前检查，避免重复插入
     * 
     * @param likes 待检查的点赞关系列表
     * @return 已存在的点赞关系列表（只包含 postId 和 userId）
     */
    List<PostLike> batchExists(@Param("likes") List<PostLike> likes);

    int countByPostId(Long postId);

    /**
     * 查询所有点赞关系（用于数据加载）
     */
    List<PostLike> selectAll();

    /**
     * 查询所有有点赞的帖子ID和点赞数（用于定时同步）
     * 
     * @return 帖子ID和点赞数的映射（PostLike对象中postId存储帖子ID，id字段存储点赞数）
     */
    List<PostLike> selectAllPostLikeCounts();

    /**
     * 根据帖子ID查询所有点赞的用户ID列表
     * 用于数据校验
     * 
     * @param postId 帖子ID
     * @return 用户ID列表
     */
    List<Long> selectUserIdsByPostId(@Param("postId") Long postId);

    /**
     * 查询所有有点赞的帖子ID列表（去重）
     * 用于定时校验
     * 
     * @return 帖子ID列表
     */
    List<Long> selectDistinctPostIds();

    /**
     * 分页查询有点赞的帖子ID列表（用于分批校验）
     * 
     * @param offset 偏移量
     * @param limit  限制数量
     * @return 帖子ID列表
     */
    List<Long> selectDistinctPostIdsWithPaging(@Param("offset") int offset, @Param("limit") int limit);
}
