package com.wait.config.script;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 通知操作脚本管理类
 * 管理通知相关操作的 Lua 脚本，确保原子性
 */
@Component
@Slf4j
public class NotificationScripts extends LuaScriptConfig {

    public static final String SEND_NOTIFICATION = "send_notification";
    public static final String DELETE_NOTIFICATION = "delete_notification";
    public static final String MARK_ALL_AS_READ = "mark_all_as_read";

    public NotificationScripts(StringRedisTemplate stringRedisTemplate) {
        super(stringRedisTemplate);
    }

    @Override
    protected Map<String, Class<?>> buildReturnTypeMap() {
        Map<String, Class<?>> returnTypeMap = new HashMap<>();
        returnTypeMap.put(SEND_NOTIFICATION, String.class); // 返回通知ID
        returnTypeMap.put(DELETE_NOTIFICATION, Long.class); // 返回删除结果（0或1）
        returnTypeMap.put(MARK_ALL_AS_READ, Long.class); // 返回标记为已读的数量
        return Collections.unmodifiableMap(returnTypeMap);
    }

    @Override
    protected String getScriptDirectory() {
        return "classpath:lua/notification/*.lua";
    }

}

