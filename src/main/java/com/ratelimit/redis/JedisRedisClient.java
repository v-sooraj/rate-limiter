package com.ratelimit.redis;

import redis.clients.jedis.UnifiedJedis;

import java.util.List;

public class JedisRedisClient implements RedisClient {

    private final UnifiedJedis jedis;

    public JedisRedisClient(UnifiedJedis jedis) {
        this.jedis = jedis;
    }

    @Override
    public Object eval(String script, List<String> keys, List<String> args) {
        return jedis.eval(script, keys, args);
    }
}
