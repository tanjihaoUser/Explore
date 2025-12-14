package com.wait.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.wait.entity.domain.UserBlock;

@Mapper
public interface UserBlockMapper {

    int insert(UserBlock userBlock);

    /**
     * 批量插入（用于数据修复）
     * 
     * @param blocks 待插入的黑名单关系列表
     * @return 插入的记录数
     */
    int batchInsert(List<UserBlock> blocks);

    int delete(@Param("userId") Long userId, @Param("blockedUserId") Long blockedUserId);

    /**
     * 批量删除（用于数据修复）
     * 
     * @param deletes 待删除的黑名单关系列表
     * @return 删除的记录数
     */
    int batchDelete(@Param("deletes") List<UserBlock> deletes);

    boolean exists(@Param("userId") Long userId, @Param("blockedUserId") Long blockedUserId);

    /**
     * 查询用户的黑名单列表
     */
    List<Long> selectBlockedUserIds(@Param("userId") Long userId);

    /**
     * 批量检查用户是否被拉黑
     */
    List<Long> batchCheckBlocked(@Param("userId") Long userId, @Param("targetUserIds") List<Long> targetUserIds);

    /**
     * 查询所有有黑名单关系的用户ID列表（去重）
     * 用于定时校验
     * 
     * @return 用户ID列表
     */
    List<Long> selectDistinctUserIds();

    /**
     * 分页查询有黑名单关系的用户ID列表（用于分批校验）
     * 
     * @param offset 偏移量
     * @param limit  限制数量
     * @return 用户ID列表
     */
    List<Long> selectDistinctUserIdsWithPaging(@Param("offset") int offset, @Param("limit") int limit);
}
