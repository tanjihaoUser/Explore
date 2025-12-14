package com.wait.service;

import com.wait.entity.domain.UserBase;
import com.wait.entity.dto.UserStatisticsDTO;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 根据用户ID查找用户
     */
    UserBase findById(Long userId);
    
    /**
     * 根据用户名查找用户
     */
    UserBase findByUsername(String username);
    
    /**
     * 验证用户密码
     */
    UserBase validateUser(String username, String password);
    
    /**
     * 创建新用户
     */
    UserBase createUser(String username, String email, String password);
    
    /**
     * 更新用户密码
     */
    boolean updatePassword(Long userId, String oldPassword, String newPassword);
    
    /**
     * 更新最后登录时间
     */
    void updateLastLoginTime(Long userId);
    
    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 获取用户排行榜（按帖子数、点赞、收藏、评论加权排序）
     * @param page 页码
     * @param pageSize 每页大小
     * @param currentUserId 当前用户ID（用于判断是否关注，可为null）
     * @return 用户统计数据列表
     */
    List<UserStatisticsDTO> getUserRanking(int page, int pageSize, Long currentUserId);
}
