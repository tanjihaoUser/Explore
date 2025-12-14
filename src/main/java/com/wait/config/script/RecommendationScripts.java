package com.wait.config.script;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RecommendationScripts extends LuaScriptConfig {

    public static final String FILE_PATH = "lua/recommendation/%s.lua";
    public static final String RECOMMEND_AND_MARK = "recommend_and_mark";

    public RecommendationScripts(StringRedisTemplate stringRedisTemplate) {
        super(stringRedisTemplate);
    }

    @Override
    protected Map<String, Class<?>> buildReturnTypeMap() {
        Map<String, Class<?>> returnTypeMap = new HashMap<>();
        returnTypeMap.put(RECOMMEND_AND_MARK, String.class); // 返回 JSON 字符串
        return Collections.unmodifiableMap(returnTypeMap);
    }

    @Override
    protected String getScriptDirectory() {
        return "classpath:lua/recommendation/*.lua";
    }
}
