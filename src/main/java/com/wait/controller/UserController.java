package com.wait.controller;

import com.wait.entity.domain.UserBase;
import com.wait.entity.dto.UserStatisticsDTO;
import com.wait.service.UserService;
import com.wait.util.ResponseUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    /**
     * 用户注册
     * POST /api/users/register
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        try {
            UserBase user = userService.createUser(request.getUsername(), request.getEmail(), request.getPassword());
            
            Map<String, Object> extraFields = new HashMap<>();
            extraFields.put("userId", user.getId());
            extraFields.put("message", "注册成功");
            
            // 返回用户信息（不包含密码）
            UserBase safeUser = new UserBase();
            safeUser.setId(user.getId());
            safeUser.setUsername(user.getUsername());
            safeUser.setEmail(user.getEmail());
            safeUser.setPhone(user.getPhone());
            safeUser.setStatus(user.getStatus());
            safeUser.setUserType(user.getUserType());
            safeUser.setCreateTime(user.getCreateTime());
            
            return ResponseUtil.success(extraFields, safeUser);
        } catch (IllegalArgumentException e) {
            log.warn("用户注册失败: {}", e.getMessage());
            return ResponseUtil.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("用户注册异常", e);
            return ResponseUtil.error(500, "注册失败，请稍后重试");
        }
    }
    
    /**
     * 检查用户名是否可用
     * GET /api/users/check-username?username=xxx
     */
    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Object>> checkUsername(@RequestParam String username) {
        try {
            boolean exists = userService.existsByUsername(username);
            Map<String, Object> data = new HashMap<>();
            data.put("available", !exists);
            data.put("message", exists ? "用户名已存在" : "用户名可用");
            
            return ResponseUtil.success(data);
        } catch (Exception e) {
            log.error("检查用户名异常", e);
            return ResponseUtil.error(500, "检查失败");
        }
    }
    
    /**
     * 检查邮箱是否可用
     * GET /api/users/check-email?email=xxx
     */
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam String email) {
        try {
            boolean exists = userService.existsByEmail(email);
            Map<String, Object> data = new HashMap<>();
            data.put("available", !exists);
            data.put("message", exists ? "邮箱已存在" : "邮箱可用");
            
            return ResponseUtil.success(data);
        } catch (Exception e) {
            log.error("检查邮箱异常", e);
            return ResponseUtil.error(500, "检查失败");
        }
    }
    
    /**
     * 获取用户信息
     * GET /api/users/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUserInfo(@PathVariable Long userId) {
        try {
            UserBase user = userService.findById(userId);
            if (user == null) {
                return ResponseUtil.error(404, "用户不存在");
            }
            
            // 返回安全的用户信息（不包含密码）
            UserBase safeUser = new UserBase();
            safeUser.setId(user.getId());
            safeUser.setUsername(user.getUsername());
            safeUser.setEmail(user.getEmail());
            safeUser.setPhone(user.getPhone());
            safeUser.setStatus(user.getStatus());
            safeUser.setUserType(user.getUserType());
            safeUser.setCreateTime(user.getCreateTime());
            safeUser.setLastLoginTime(user.getLastLoginTime());
            
            return ResponseUtil.success(safeUser);
        } catch (Exception e) {
            log.error("获取用户信息异常: userId={}", userId, e);
            return ResponseUtil.error(500, "获取用户信息失败");
        }
    }
    
    /**
     * 修改密码
     * PUT /api/users/{userId}/password
     */
    @PutMapping("/{userId}/password")
    public ResponseEntity<Map<String, Object>> updatePassword(
            @PathVariable Long userId,
            @RequestBody UpdatePasswordRequest request) {
        try {
            boolean success = userService.updatePassword(userId, request.getOldPassword(), request.getNewPassword());
            
            if (success) {
                return ResponseUtil.success("密码修改成功");
            } else {
                return ResponseUtil.error(400, "旧密码验证失败");
            }
        } catch (Exception e) {
            log.error("修改密码异常: userId={}", userId, e);
            return ResponseUtil.error(500, "修改密码失败");
        }
    }

    /**
     * 获取用户排行榜（按帖子数、点赞、收藏、评论加权排序）
     * GET /api/users/ranking?page=1&pageSize=20&currentUserId=xxx
     */
    @GetMapping("/ranking")
    public ResponseEntity<Map<String, Object>> getUserRanking(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long currentUserId) {
        try {
            log.info("获取用户排行榜: page={}, pageSize={}, currentUserId={}", page, pageSize, currentUserId);
            
            List<UserStatisticsDTO> ranking = userService.getUserRanking(page, pageSize, currentUserId);
            
            Map<String, Object> data = new HashMap<>();
            data.put("users", ranking);
            data.put("page", page);
            data.put("pageSize", pageSize);
            data.put("count", ranking.size());
            
            return ResponseUtil.success(data);
        } catch (Exception e) {
            log.error("获取用户排行榜失败", e);
            return ResponseUtil.error(500, "获取用户排行榜失败");
        }
    }
    
    // DTO 类
    @Data
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        private String phone;
    }
    
    @Data
    public static class UpdatePasswordRequest {
        private String oldPassword;
        private String newPassword;
    }
}
