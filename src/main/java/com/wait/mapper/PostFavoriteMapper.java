package com.wait.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.wait.entity.domain.PostFavorite;

@Mapper
public interface PostFavoriteMapper {

    /**
     * 插入收藏记录
     */
    int insert(PostFavorite postFavorite);

    /**
     * 删除收藏记录
     */
    int delete(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * 批量删除（用于数据修复）
     * 
     * @param deletes 待删除的收藏关系列表
     * @return 删除的记录数
     */
    int batchDelete(@Param("deletes") List<PostFavorite> deletes);

    /**
     * 检查是否存在
     */
    boolean exists(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * 批量查询存在性：返回已存在的 (userId, postId) 对
     * 用于批量写入前检查，避免重复插入
     * 
     * @param favorites 待检查的收藏关系列表
     * @return 已存在的收藏关系列表（只包含 userId 和 postId）
     */
    List<PostFavorite> batchExists(@Param("favorites") List<PostFavorite> favorites);

    /**
     * 根据帖子ID统计收藏数
     */
    int countByPostId(Long postId);

    /**
     * 根据用户ID查询收藏的帖子ID列表
     */
    List<Long> selectPostIdsByUserId(Long userId);

    /**
     * 批量插入（用于数据恢复/同步）
     */
    int batchInsert(List<PostFavorite> favorites);

    /**
     * 查询所有收藏关系（用于数据加载）
     */
    List<PostFavorite> selectAll();

    /**
     * 查询所有有收藏的帖子ID和收藏数（用于定时同步）
     * 
     * @return 帖子ID和收藏数的映射（PostFavorite对象中postId存储帖子ID，id字段存储收藏数）
     */
    List<PostFavorite> selectAllPostFavoriteCounts();

    /**
     * 查询所有有收藏的用户ID列表（去重）
     * 用于定时校验
     * 
     * @return 用户ID列表
     */
    List<Long> selectDistinctUserIds();

    /**
     * 分页查询有收藏的用户ID列表（用于分批校验）
     * 
     * @param offset 偏移量
     * @param limit  限制数量
     * @return 用户ID列表
     */
    List<Long> selectDistinctUserIdsWithPaging(@Param("offset") int offset, @Param("limit") int limit);
}
