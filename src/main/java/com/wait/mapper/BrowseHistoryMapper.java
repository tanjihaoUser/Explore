package com.wait.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.wait.entity.domain.BrowseHistory;

/**
 * 浏览记录 Mapper
 */
@Mapper
public interface BrowseHistoryMapper {

    /**
     * 批量插入浏览记录
     * 
     * @param histories 浏览记录列表
     * @return 插入的记录数
     */
    int batchInsert(@Param("histories") List<BrowseHistory> histories);

    /**
     * 查询用户的浏览记录（按时间倒序）
     * 
     * @param userId 用户ID
     * @param offset 偏移量
     * @param limit 数量限制
     * @return 浏览记录列表
     */
    List<BrowseHistory> selectByUserId(@Param("userId") Long userId,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    /**
     * 查询指定用户和帖子ID的浏览记录
     * 
     * @param userId 用户ID
     * @param postId 帖子ID
     * @return 浏览记录
     */
    BrowseHistory selectByUserIdAndPostId(@Param("userId") Long userId,
                                           @Param("postId") Long postId);

    /**
     * 查询指定用户和时间范围内的浏览记录
     * 
     * @param userId 用户ID
     * @param startTime 开始时间戳（毫秒）
     * @param endTime 结束时间戳（毫秒）
     * @return 浏览记录列表
     */
    List<BrowseHistory> selectByUserIdAndTimeRange(@Param("userId") Long userId,
                                                    @Param("startTime") Long startTime,
                                                    @Param("endTime") Long endTime);

    /**
     * 检查用户是否浏览过指定帖子
     * 
     * @param userId 用户ID
     * @param postId 帖子ID
     * @return 是否存在
     */
    boolean existsByUserIdAndPostId(@Param("userId") Long userId,
                                     @Param("postId") Long postId);

    /**
     * 统计用户的浏览记录数量
     * 
     * @param userId 用户ID
     * @return 浏览记录数量
     */
    Long countByUserId(@Param("userId") Long userId);

    /**
     * 删除用户的所有浏览记录
     * 
     * @param userId 用户ID
     * @return 删除的记录数
     */
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 删除用户指定时间之前的浏览记录
     * 
     * @param userId 用户ID
     * @param expireTime 过期时间戳（毫秒）
     * @return 删除的记录数
     */
    int deleteByUserIdAndExpireTime(@Param("userId") Long userId,
                                     @Param("expireTime") Long expireTime);
}

