package com.wait.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.wait.entity.domain.UserBlock;

@Mapper
public interface UserBlockMapper {

    int insert(UserBlock userBlock);

    int delete(@Param("userId") Long userId, @Param("blockedUserId") Long blockedUserId);

    boolean exists(@Param("userId") Long userId, @Param("blockedUserId") Long blockedUserId);

    /**
     * 查询用户的黑名单列表
     */
    List<Long> selectBlockedUserIds(@Param("userId") Long userId);

    /**
     * 批量检查用户是否被拉黑
     */
    List<Long> batchCheckBlocked(@Param("userId") Long userId, @Param("targetUserIds") List<Long> targetUserIds);
}

