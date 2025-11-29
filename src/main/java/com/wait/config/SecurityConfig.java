package com.wait.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 安全配置类 - 适用于 Spring Boot 2.7.x
 */
@Configuration
@EnableWebSecurity
@SuppressWarnings("deprecation") // 抑制弃用警告
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    /**
     * 密码加密器
     * BCrypt 是一种安全的密码哈希函数，具有以下特点：
     * 1. 自带盐值，每次加密结果都不同
     * 2. 计算成本可调，可以抵御暴力破解
     * 3. 单向加密，不可逆
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        // 使用默认强度 10，可以根据需要调整（4-31）
        // 强度越高，计算时间越长，安全性越高
        return new BCryptPasswordEncoder(10);
    }

    /**
     * HTTP 安全配置
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF 保护（对于 REST API 通常不需要）
                .csrf().disable()

                // 配置 CORS
                .cors().configurationSource(corsConfigurationSource())

                // 配置会话管理
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

                // 配置授权规则
                .and()
                .authorizeRequests()
                // 允许所有用户访问的端点
                .antMatchers(
                        "/api/sessions/login", // 登录接口
                        "/api/sessions/**", // 所有会话相关接口
                        "/api/users/**", // 所有用户相关接口
                        "/api/posts/**", // 所有帖子相关接口
                        "/api/comments/**", // 所有评论相关接口
                        "/api/ranking/**", // 所有排行榜相关接口
                        "/api/relation/**", // 所有关系相关接口
                        "/api/test/**", // 测试接口
                        "/api/debug/**", // 调试接口
                        "/api/password-test/**", // 密码测试接口
                        "/error", // 错误页面
                        "/actuator/health" // 健康检查
                ).permitAll()

                // 其他所有请求都需要认证
                .anyRequest().authenticated()

                // 禁用默认的登录页面
                .and()
                .formLogin().disable()

                // 禁用 HTTP Basic 认证
                .httpBasic().disable();
    }

    /**
     * CORS 配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许的源
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));

        // 允许的 HTTP 方法
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 允许的请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 允许携带凭证
        configuration.setAllowCredentials(true);

        // 预检请求的缓存时间
        configuration.setMaxAge(3600L);

        // 暴露的响应头
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "X-Requested-With", "Accept",
                "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
