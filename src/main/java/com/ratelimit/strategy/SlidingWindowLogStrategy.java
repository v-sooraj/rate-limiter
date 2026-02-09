package com.ratelimit.strategy;

import com.ratelimit.api.RateLimitResult;
import com.ratelimit.redis.RedisClient;
import com.ratelimit.rules.RateLimitRule;
import com.ratelimit.util.ScriptLoader;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("unchecked")
public class SlidingWindowLogStrategy implements RateLimitStrategy {

    private static final String LUA_SCRIPT = ScriptLoader.load("/scripts/sliding_window.lua");

    private final RedisClient redisClient;

    public SlidingWindowLogStrategy(RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    @Override
    public RateLimitResult execute(String key, RateLimitRule rule) {

        Object response = redisClient.eval(
                LUA_SCRIPT,
                Collections.singletonList(key),
                List.of(
                        String.valueOf(rule.windowMs()),
                        String.valueOf(rule.limit())
                )
        );

        List<Object> result = (List<Object>) response;

        boolean allowed = (Long) result.get(0) == 1L;
        long remaining = (Long) result.get(1);
        long retryAfterMs = (Long) result.get(2);

        // Optional: calculate resetAtMs = current time + retryAfterMs
        long resetAtMs = System.currentTimeMillis() + retryAfterMs;

        return new RateLimitResult(
                allowed,
                remaining,
                retryAfterMs,
                resetAtMs,
                rule.name()
        );
    }
}