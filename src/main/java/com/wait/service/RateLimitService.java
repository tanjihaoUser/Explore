package com.wait.service;

import java.util.concurrent.TimeUnit;

/**
 * 限流服务接口
 * 提供基于Redis的限流功能
 */
public interface RateLimitService {

    /**
     * 根据key获取值（无限流）
     * @param key Redis key
     * @param clazz 返回类型
     * @return 值
     */
    <T> T getByKey(String key, Class<T> clazz);

    /**
     * 根据key获取值（带限流）
     * @param key Redis key
     * @param limit 限制次数
     * @param interval 时间间隔
     * @param unit 时间单位
     * @return 值（如果被限流，返回默认值100）
     */
    int getWithLimit(String key, int limit, int interval, TimeUnit unit);
}

