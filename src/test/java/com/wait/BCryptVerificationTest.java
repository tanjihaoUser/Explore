package com.wait;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.annotation.Resource;

/**
 * BCrypt验证机制测试 - 验证即使每次加密生成不同的哈希值，也能正确验证密码
 */
@SpringBootTest
public class BCryptVerificationTest {

    @Resource
    private BCryptPasswordEncoder passwordEncoder;

    @Test
    void testBCryptVerificationMechanism() {
        System.out.println("==========================================");
        System.out.println("BCrypt 验证机制测试");
        System.out.println("==========================================");
        
        String password = "123456";
        
        // 测试1: 多次加密同一密码，生成不同的哈希值
        System.out.println("\n【测试1】多次加密同一密码，生成不同的哈希值：");
        String hash1 = passwordEncoder.encode(password);
        String hash2 = passwordEncoder.encode(password);
        String hash3 = passwordEncoder.encode(password);
        
        System.out.println("密码: " + password);
        System.out.println("哈希1: " + hash1);
        System.out.println("哈希2: " + hash2);
        System.out.println("哈希3: " + hash3);
        System.out.println("哈希1 == 哈希2? " + hash1.equals(hash2));
        System.out.println("哈希1 == 哈希3? " + hash1.equals(hash3));
        
        // 测试2: 验证所有哈希值都能匹配原始密码
        System.out.println("\n【测试2】验证所有哈希值都能匹配原始密码：");
        boolean match1 = passwordEncoder.matches(password, hash1);
        boolean match2 = passwordEncoder.matches(password, hash2);
        boolean match3 = passwordEncoder.matches(password, hash3);
        
        System.out.println("密码 '" + password + "' 匹配 哈希1? " + match1);
        System.out.println("密码 '" + password + "' 匹配 哈希2? " + match2);
        System.out.println("密码 '" + password + "' 匹配 哈希3? " + match3);
        
        // 测试3: 验证错误密码不能匹配
        System.out.println("\n【测试3】验证错误密码不能匹配：");
        String wrongPassword = "wrong123";
        boolean wrongMatch1 = passwordEncoder.matches(wrongPassword, hash1);
        boolean wrongMatch2 = passwordEncoder.matches(wrongPassword, hash2);
        
        System.out.println("错误密码 '" + wrongPassword + "' 匹配 哈希1? " + wrongMatch1);
        System.out.println("错误密码 '" + wrongPassword + "' 匹配 哈希2? " + wrongMatch2);
        
        // 测试4: 验证之前失败的哈希值
        System.out.println("\n【测试4】验证之前失败的哈希值：");
        String oldHash = "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";
        String[] testPasswords = {
            "123456",
            "password",
            "admin",
            "test",
            "lizhujin"
        };
        
        System.out.println("旧哈希值: " + oldHash);
        for (String testPwd : testPasswords) {
            boolean matches = passwordEncoder.matches(testPwd, oldHash);
            System.out.println("密码 '" + testPwd + "' 匹配? " + matches);
        }
        
        // 测试5: 验证BCrypt哈希值的结构
        System.out.println("\n【测试5】BCrypt哈希值结构分析：");
        System.out.println("BCrypt哈希格式: $2a$10$[22字符盐值][31字符哈希值]");
        System.out.println("哈希1长度: " + hash1.length() + " (应该是60)");
        System.out.println("哈希1格式: " + (hash1.startsWith("$2a$") || hash1.startsWith("$2b$") || hash1.startsWith("$2y$")));
        
        System.out.println("\n==========================================");
        System.out.println("结论：");
        System.out.println("1. BCrypt每次加密生成不同的哈希值（因为盐值不同）");
        System.out.println("2. 但验证时能正确识别原始密码（通过盐值和算法）");
        System.out.println("3. 如果验证失败，说明数据库中的哈希值不是用该密码生成的");
        System.out.println("==========================================");
    }
    
    @Test
    void testWhyVerificationFailed() {
        System.out.println("==========================================");
        System.out.println("分析为什么之前验证失败");
        System.out.println("==========================================");
        
        String oldHash = "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";
        String testPassword = "123456";
        
        System.out.println("旧哈希值: " + oldHash);
        System.out.println("测试密码: " + testPassword);
        
        boolean matches = passwordEncoder.matches(testPassword, oldHash);
        System.out.println("匹配结果: " + matches);
        
        if (!matches) {
            System.out.println("\n原因分析：");
            System.out.println("1. 这个哈希值不是用 '123456' 生成的");
            System.out.println("2. 可能的情况：");
            System.out.println("   - 数据库中的哈希值是用其他密码生成的");
            System.out.println("   - 之前更新密码时使用了错误的密码");
            System.out.println("   - 数据库被手动修改过");
            System.out.println("   - 使用了不同的BCrypt实现或配置");
            
            // 生成一个用123456正确生成的哈希值
            String correctHash = passwordEncoder.encode(testPassword);
            System.out.println("\n3. 用 '123456' 正确生成的哈希值示例: " + correctHash);
            System.out.println("   验证这个新哈希值: " + passwordEncoder.matches(testPassword, correctHash));
        }
    }
}

