package com.wait.config.script;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class BrowseHistoryScripts extends LuaScriptConfig {

    public static final String FILE_PATH = "lua/browse_history/%s.lua";
    public static final String RECORD_BROWSE = "record_browse";

    public BrowseHistoryScripts(StringRedisTemplate stringRedisTemplate) {
        super(stringRedisTemplate);
    }

    @Override
    protected Map<String, Class<?>> buildReturnTypeMap() {
        Map<String, Class<?>> returnTypeMap = new HashMap<>();
        returnTypeMap.put(RECORD_BROWSE, Long.class);
        return Collections.unmodifiableMap(returnTypeMap);
    }

    @Override
    protected String getScriptDirectory() {
        return "classpath:lua/browse_history/*.lua";
    }
}
