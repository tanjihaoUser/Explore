package com.wait.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.wait.entity.domain.UserFollow;

@Mapper
public interface FollowMapper {

    int insert(UserFollow userFollow);

    /**
     * 批量插入（用于数据修复）
     * 
     * @param follows 待插入的关注关系列表
     * @return 插入的记录数
     */
    int batchInsert(List<UserFollow> follows);

    int delete(@Param("followerId") Long followerId, @Param("followedId") Long followedId);

    /**
     * 批量删除（用于数据修复）
     * 
     * @param deletes 待删除的关注关系列表
     * @return 删除的记录数
     */
    int batchDelete(@Param("deletes") List<UserFollow> deletes);

    List<Long> selectFollowedIds(Long followerId);

    boolean exists(@Param("followerId") Long followerId, @Param("followedId") Long followedId);

    int countFollowers(Long followedId);

    int countFollowing(Long followerId);

    /**
     * 查询所有有关注关系的用户ID列表（去重，包括关注者和被关注者）
     * 用于定时校验
     * 
     * @return 用户ID列表
     */
    List<Long> selectDistinctUserIds();

    /**
     * 分页查询有关注关系的用户ID列表（用于分批校验）
     * 
     * @param offset 偏移量
     * @param limit  限制数量
     * @return 用户ID列表
     */
    List<Long> selectDistinctUserIdsWithPaging(@Param("offset") int offset, @Param("limit") int limit);
}
