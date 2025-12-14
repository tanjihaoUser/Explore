package com.wait.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.wait.entity.domain.Post;
import com.wait.entity.domain.UserBase;
import com.wait.entity.dto.UserStatisticsDTO;
import com.wait.mapper.PostMapper;
import com.wait.mapper.UserBaseMapper;
import com.wait.service.RelationService;
import com.wait.service.UserService;

import lombok.extern.slf4j.Slf4j;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserBaseMapper userBaseMapper;
    private final PostMapper postMapper;
    private final RelationService relationService;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserServiceImpl(UserBaseMapper userBaseMapper, PostMapper postMapper,
            @Lazy RelationService relationService, BCryptPasswordEncoder passwordEncoder) {
        this.userBaseMapper = userBaseMapper;
        this.postMapper = postMapper;
        this.relationService = relationService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserBase findById(Long userId) {
        if (userId == null) {
            return null;
        }

        try {
            return userBaseMapper.selectById(userId);
        } catch (Exception e) {
            log.error("查询用户失败: userId={}", userId, e);
            return null;
        }
    }

    @Override
    public UserBase findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }

        try {
            return userBaseMapper.selectByUsername(username);
        } catch (Exception e) {
            log.error("查询用户失败: username={}", username, e);
            return null;
        }
    }

    @Override
    public UserBase validateUser(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            log.warn("用户名或密码为空");
            return null;
        }

        try {
            UserBase user = findByUsername(username);
            if (user == null) {
                log.warn("用户不存在: {}", username);
                return null;
            }

            // 检查用户状态
            if (user.getStatus() == null || user.getStatus() != UserBase.Status.NORMAL.getCode()) {
                log.warn("用户状态异常: username={}, status={}", username, user.getStatus());
                return null;
            }

            // 验证密码
            if (!StringUtils.hasText(user.getPasswordHash())) {
                log.warn("用户未设置密码: {}", username);
                return null;
            }

            // 详细的密码验证日志
            log.info("开始密码验证 - 用户: {}", username);
            log.info("数据库密码哈希: {}", user.getPasswordHash());
            log.info("输入的明文密码: {}", password);
            log.info("密码哈希长度: {}", user.getPasswordHash().length());
            log.info("是否为BCrypt格式: {}", user.getPasswordHash().startsWith("$2"));

            boolean matches = passwordEncoder.matches(password, user.getPasswordHash());
            log.info("密码匹配结果: {}", matches);

            if (matches) {
                log.info("用户验证成功: {}", username);
                // 更新最后登录时间
                updateLastLoginTime(user.getId());
                return user;
            } else {
                log.warn("密码验证失败 - 用户: {}, 哈希: {}, 输入: {}", username, user.getPasswordHash(), password);
                return null;
            }

        } catch (Exception e) {
            log.error("用户验证异常: username={}", username, e);
            return null;
        }
    }

    @Override
    public UserBase createUser(String username, String email, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("用户名、邮箱和密码不能为空");
        }

        // 检查用户名是否已存在
        if (existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在: " + username);
        }

        // 检查邮箱是否已存在
        if (existsByEmail(email)) {
            throw new IllegalArgumentException("邮箱已存在: " + email);
        }

        try {
            UserBase user = new UserBase();
            user.setUsername(username);
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setStatus(UserBase.Status.NORMAL.getCode());
            user.setUserType(UserBase.UserType.NORMAL.getCode());
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            user.setPasswordUpdateTime(LocalDateTime.now());

            userBaseMapper.insert(user);
            log.info("创建用户成功: username={}, email={}", username, email);

            return user;
        } catch (Exception e) {
            log.error("创建用户失败: username={}, email={}", username, email, e);
            throw new RuntimeException("创建用户失败", e);
        }
    }

    @Override
    public boolean updatePassword(Long userId, String oldPassword, String newPassword) {
        if (userId == null || !StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            return false;
        }

        try {
            UserBase user = userBaseMapper.selectById(userId);
            if (user == null) {
                log.warn("用户不存在: userId={}", userId);
                return false;
            }

            // 验证旧密码
            if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
                log.warn("旧密码验证失败: userId={}", userId);
                return false;
            }

            // 更新密码
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            user.setPasswordUpdateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());

            userBaseMapper.updateById(user);
            log.info("密码更新成功: userId={}", userId);

            return true;
        } catch (Exception e) {
            log.error("更新密码失败: userId={}", userId, e);
            return false;
        }
    }

    @Override
    public void updateLastLoginTime(Long userId) {
        if (userId == null) {
            return;
        }

        try {
            UserBase user = new UserBase();
            user.setId(userId);
            user.setLastLoginTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());

            userBaseMapper.updateLastLoginTime(userId, LocalDateTime.now());
            log.debug("更新最后登录时间: userId={}", userId);
        } catch (Exception e) {
            log.error("更新最后登录时间失败: userId={}", userId, e);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }

        try {
            return userBaseMapper.countByUsername(username) > 0;
        } catch (Exception e) {
            log.error("检查用户名是否存在失败: username={}", username, e);
            return false;
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }

        try {
            return userBaseMapper.countByEmail(email) > 0;
        } catch (Exception e) {
            log.error("检查邮箱是否存在失败: email={}", email, e);
            return false;
        }
    }

    @Override
    public List<UserStatisticsDTO> getUserRanking(int page, int pageSize, Long currentUserId) {
        try {
            // 1. 获取所有用户
            List<UserBase> allUsers = userBaseMapper.selectAll();
            if (allUsers == null || allUsers.isEmpty()) {
                return new ArrayList<>();
            }

            // 2. 为每个用户计算统计数据
            List<UserStatisticsDTO> userStats = new ArrayList<>();
            for (UserBase user : allUsers) {
                if (user.getId() == null) {
                    continue;
                }

                Long userId = user.getId();

                // 从数据库获取帖子统计数据
                Map<String, Object> postStats = postMapper.selectUserStatistics(userId);
                Long postCount = postStats != null && postStats.get("postCount") != null
                        ? ((Number) postStats.get("postCount")).longValue()
                        : 0L;
                Long totalLikeCount = postStats != null && postStats.get("totalLikeCount") != null
                        ? ((Number) postStats.get("totalLikeCount")).longValue()
                        : 0L;
                Long totalCommentCount = postStats != null && postStats.get("totalCommentCount") != null
                        ? ((Number) postStats.get("totalCommentCount")).longValue()
                        : 0L;

                // 从Redis获取收藏总数（需要获取用户的所有帖子ID，然后统计收藏数）
                Long totalFavoriteCount = 0L;
                try {
                    List<Post> userPosts = postMapper.selectByUserId(userId);
                    if (userPosts != null && !userPosts.isEmpty()) {
                        for (Post post : userPosts) {
                            if (post.getId() != null) {
                                Long favoriteCount = relationService.getFavoriteCount(post.getId());
                                if (favoriteCount != null) {
                                    totalFavoriteCount += favoriteCount;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("获取用户{}的收藏总数失败", userId, e);
                }

                // 计算加权分数
                // 权重：帖子数*1 + 点赞数*2 + 收藏数*3 + 评论数*1
                double score = postCount * 1.0 + totalLikeCount * 2.0 + totalFavoriteCount * 3.0
                        + totalCommentCount * 1.0;

                // 判断是否关注
                Boolean isFollowing = null;
                if (currentUserId != null && !currentUserId.equals(userId)) {
                    isFollowing = relationService.isFollowing(currentUserId, userId);
                }

                UserStatisticsDTO dto = UserStatisticsDTO.builder()
                        .userId(userId)
                        .username(user.getUsername())
                        .postCount(postCount)
                        .totalLikeCount(totalLikeCount)
                        .totalFavoriteCount(totalFavoriteCount)
                        .totalCommentCount(totalCommentCount)
                        .score(score)
                        .isFollowing(isFollowing)
                        .build();

                userStats.add(dto);
            }

            // 3. 按分数排序（降序）
            userStats.sort(Comparator.comparing(UserStatisticsDTO::getScore).reversed());

            // 4. 分页
            int start = (page - 1) * pageSize;
            int end = Math.min(start + pageSize, userStats.size());

            if (start >= userStats.size()) {
                return new ArrayList<>();
            }

            return userStats.subList(start, end);

        } catch (Exception e) {
            log.error("获取用户排行榜失败", e);
            return new ArrayList<>();
        }
    }
}
