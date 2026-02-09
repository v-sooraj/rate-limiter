package com.ratelimit.service.impl;

import com.ratelimit.config.RedisConfig;
import com.ratelimit.service.RateLimit;
import com.ratelimit.util.ScriptLoader;
import redis.clients.jedis.UnifiedJedis;

import java.util.Collections;
import java.util.List;

public class SlidingWindowLog implements RateLimit {

    private final UnifiedJedis jedis = RedisConfig.INSTANCE.getClient();

    // This runs inside Redis
    private static final String LUA_SCRIPT = ScriptLoader.load("/scripts/sliding_window.lua");

    @Override
    public boolean isAllowed(String clientId, int limit, int windowMs) {
        return jedis.eval(LUA_SCRIPT,
                        Collections.singletonList("ratelimit:" + clientId),
                        List.of(String.valueOf(windowMs), String.valueOf(limit)))
                .equals(1L);
    }
}