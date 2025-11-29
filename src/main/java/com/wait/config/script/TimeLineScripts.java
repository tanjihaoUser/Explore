package com.wait.config.script;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 时间线操作脚本管理类
 * 统一管理 timeline 目录下的所有 Lua 脚本，包括：
 * - 帖子发布/删除相关脚本（publish_post, delete_post）
 * - 时间线 Sorted Set 操作脚本（publish_to_timeline, remove_from_timeline）
 */
@Component
@Slf4j
public class TimeLineScripts extends LuaScriptConfig {

    // 帖子发布/删除相关脚本
    public static final String PUBLISH_POST = "publish_post";
    public static final String DELETE_POST = "delete_post";

    // 时间线 Sorted Set 操作脚本
    public static final String PUBLISH_TO_TIMELINE = "publish_to_timeline";
    public static final String REMOVE_FROM_TIMELINE = "remove_from_timeline";

    public TimeLineScripts(StringRedisTemplate stringRedisTemplate) {
        super(stringRedisTemplate);
    }

    @Override
    protected Map<String, Class<?>> buildReturnTypeMap() {
        Map<String, Class<?>> returnTypeMap = new HashMap<>();
        // 帖子发布/删除相关脚本
        returnTypeMap.put(PUBLISH_POST, Long.class);
        returnTypeMap.put(DELETE_POST, Long.class);
        // 时间线 Sorted Set 操作脚本
        returnTypeMap.put(PUBLISH_TO_TIMELINE, Long.class);
        returnTypeMap.put(REMOVE_FROM_TIMELINE, Long.class);
        return Collections.unmodifiableMap(returnTypeMap);
    }

    @Override
    protected String getScriptDirectory() {
        return "classpath:lua/timeline/*.lua";
    }
}
