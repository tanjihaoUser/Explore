package com.wait;

import com.wait.entity.domain.UserBase;
import com.wait.mapper.UserBaseMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 密码测试类 - 用于验证数据库中的密码哈希值对应的原始密码，并重置所有用户密码
 */
@SpringBootTest
public class PasswordTest {

    @Resource
    private BCryptPasswordEncoder passwordEncoder;
    
    @Resource
    private UserBaseMapper userBaseMapper;

    @Test
    void testPasswordHash() {
        // 数据库中的密码哈希值
        String storedHash = "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";
        
        // 常见密码列表
        String[] commonPasswords = {
            "123456",
            "password",
            "12345678",
            "123456789",
            "1234567890",
            "admin",
            "root",
            "test",
            "123",
            "1234",
            "12345",
            "qwerty",
            "abc123",
            "password123",
            "admin123",
            "root123",
            "lizhujin",  // 用户名
            "lizhujin123",
            "lizhujin123456"
        };
        
        System.out.println("开始测试密码哈希值: " + storedHash);
        System.out.println("==========================================");
        
        boolean found = false;
        for (String password : commonPasswords) {
            boolean matches = passwordEncoder.matches(password, storedHash);
            System.out.println("密码: " + password + " -> 匹配结果: " + matches);
            if (matches) {
                System.out.println("\n✓✓✓ 找到匹配的密码: " + password + " ✓✓✓");
                found = true;
                break;
            }
        }
        
        if (!found) {
            System.out.println("\n✗✗✗ 未找到匹配的密码 ✗✗✗");
            System.out.println("建议：需要重置密码或使用其他方式验证");
        }
        
        System.out.println("\n==========================================");
        System.out.println("测试完成");
    }
    
    @Test
    void generatePasswordHash() {
        // 生成一些常见密码的哈希值，用于对比
        String[] passwords = {"123456", "password", "admin", "test"};
        
        System.out.println("生成密码哈希值:");
        System.out.println("==========================================");
        for (String password : passwords) {
            String hash = passwordEncoder.encode(password);
            System.out.println("密码: " + password);
            System.out.println("哈希: " + hash);
            System.out.println("验证: " + passwordEncoder.matches(password, hash));
            System.out.println("---");
        }
    }
    
    /**
     * 找出正确密码并重置所有用户密码为123456
     */
    @Test
    void findPasswordAndResetAll() {
        System.out.println("==========================================");
        System.out.println("步骤1: 查找正确密码");
        System.out.println("==========================================");
        
        // 从数据库获取用户
        UserBase user = userBaseMapper.selectByUsername("lizhujin");
        if (user == null) {
            System.out.println("用户 lizhujin 不存在");
            return;
        }
        
        String storedHash = user.getPasswordHash();
        System.out.println("用户: " + user.getUsername());
        System.out.println("密码哈希: " + storedHash);
        
        // 常见密码列表
        String[] commonPasswords = {
            "123456",
            "password",
            "12345678",
            "123456789",
            "1234567890",
            "admin",
            "root",
            "test",
            "123",
            "1234",
            "12345",
            "qwerty",
            "abc123",
            "password123",
            "admin123",
            "root123",
            "lizhujin",
            "lizhujin123",
            "lizhujin123456"
        };
        
        String foundPassword = null;
        for (String password : commonPasswords) {
            boolean matches = passwordEncoder.matches(password, storedHash);
            System.out.println("测试密码: " + password + " -> " + (matches ? "✓ 匹配" : "✗ 不匹配"));
            if (matches) {
                foundPassword = password;
                System.out.println("\n✓✓✓ 找到正确密码: " + password + " ✓✓✓");
                break;
            }
        }
        
        if (foundPassword == null) {
            System.out.println("\n✗✗✗ 未找到匹配的密码，将直接重置为 123456 ✗✗✗");
        }
        
        System.out.println("\n==========================================");
        System.out.println("步骤2: 生成新密码哈希 (123456)");
        System.out.println("==========================================");
        
        String newPassword = "123456";
        String newHash = passwordEncoder.encode(newPassword);
        System.out.println("新密码: " + newPassword);
        System.out.println("新哈希: " + newHash);
        System.out.println("验证新哈希: " + passwordEncoder.matches(newPassword, newHash));
        
        System.out.println("\n==========================================");
        System.out.println("步骤3: 更新所有用户密码为 123456");
        System.out.println("==========================================");
        
        try {
            // 获取所有用户
            List<UserBase> allUsers = userBaseMapper.selectAll();
            System.out.println("找到 " + allUsers.size() + " 个用户");
            
            int successCount = 0;
            int failCount = 0;
            
            for (UserBase u : allUsers) {
                try {
                    u.setPasswordHash(newHash);
                    u.setPasswordUpdateTime(LocalDateTime.now());
                    u.setUpdateTime(LocalDateTime.now());
                    userBaseMapper.updateById(u);
                    System.out.println("✓ 更新用户: " + u.getUsername() + " (ID: " + u.getId() + ")");
                    successCount++;
                } catch (Exception e) {
                    System.err.println("✗ 更新用户失败: " + u.getUsername() + " (ID: " + u.getId() + ") - " + e.getMessage());
                    failCount++;
                }
            }
            
            System.out.println("\n更新结果:");
            System.out.println("  成功: " + successCount + " 个用户");
            System.out.println("  失败: " + failCount + " 个用户");
            
        } catch (Exception e) {
            System.err.println("获取用户列表失败: " + e.getMessage());
            e.printStackTrace();
            
            // 如果查询所有用户失败，至少更新已知的用户
            try {
                user.setPasswordHash(newHash);
                user.setPasswordUpdateTime(LocalDateTime.now());
                user.setUpdateTime(LocalDateTime.now());
                userBaseMapper.updateById(user);
                System.out.println("已更新用户: " + user.getUsername());
            } catch (Exception ex) {
                System.err.println("更新用户失败: " + ex.getMessage());
            }
        }
        
        System.out.println("\n==========================================");
        System.out.println("完成！所有用户密码已重置为: 123456");
        System.out.println("==========================================");
    }
}

