package com.wait.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.wait.annotation.RedisCache;
import com.wait.entity.domain.Post;
import com.wait.entity.type.CacheType;
import com.wait.entity.type.ReadStrategyType;

@Mapper
public interface PostMapper {

    int insert(Post post);

    @RedisCache(prefix = "post", key = "#id", expire = 3000, cacheType = CacheType.STRING, returnType = Post.class, readStrategy = ReadStrategyType.LAZY_LOAD)
    Post selectById(Long id);

    List<Post> selectByUserId(@Param("userId") Long userId);

    /**
     * 分页查询用户帖子（数据库层面分页）
     * 
     * @param userId 用户ID
     * @param offset 偏移量（从第几条开始，0-based）
     * @param limit  每页数量
     * @return 帖子列表（按id倒序，最新的在前）
     */
    List<Post> selectByUserIdWithPagination(@Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("limit") int limit);

    int update(Post post);

    int delete(Long id);

    int countByUserId(Long userId);

    List<Post> selectByIds(@Param("ids") List<Long> ids);

    /**
     * 批量查询帖子的用户ID映射（仅返回postId和userId，用于优化性能）
     * @param ids 帖子ID列表
     * @return 帖子列表（只包含id和userId字段）
     */
    List<Post> selectUserIdsByIds(@Param("ids") List<Long> ids);

    /**
     * 统计用户的所有帖子数据（帖子数、点赞总数、评论总数）
     * @param userId 用户ID
     * @return 统计数据Map，包含postCount, totalLikeCount, totalCommentCount
     */
    java.util.Map<String, Object> selectUserStatistics(@Param("userId") Long userId);
}
