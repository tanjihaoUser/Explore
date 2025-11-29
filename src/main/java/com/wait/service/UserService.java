package com.wait.service;

import com.wait.entity.domain.UserBase;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 根据用户ID查找用户
     * @param userId 用户ID
     * @return 用户信息，如果不存在返回null
     */
    UserBase findById(Long userId);
    
    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 用户信息，如果不存在返回null
     */
    UserBase findByUsername(String username);
    
    /**
     * 验证用户密码
     * @param username 用户名
     * @param password 明文密码
     * @return 验证成功返回用户信息，失败返回null
     */
    UserBase validateUser(String username, String password);
    
    /**
     * 创建新用户
     * @param username 用户名
     * @param email 邮箱
     * @param password 明文密码
     * @return 创建的用户信息
     */
    UserBase createUser(String username, String email, String password);
    
    /**
     * 更新用户密码
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 是否更新成功
     */
    boolean updatePassword(Long userId, String oldPassword, String newPassword);
    
    /**
     * 更新最后登录时间
     * @param userId 用户ID
     */
    void updateLastLoginTime(Long userId);
    
    /**
     * 检查用户名是否存在
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 检查邮箱是否存在
     * @param email 邮箱
     * @return 是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 获取用户排行榜（按帖子数、点赞、收藏、评论加权排序）
     * @param page 页码
     * @param pageSize 每页大小
     * @param currentUserId 当前用户ID（用于判断是否关注，可为null）
     * @return 用户统计数据列表
     */
    java.util.List<com.wait.entity.dto.UserStatisticsDTO> getUserRanking(int page, int pageSize, Long currentUserId);
}
