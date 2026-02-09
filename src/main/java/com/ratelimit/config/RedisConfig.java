package com.ratelimit.config;

import com.ratelimit.redis.JedisRedisClient;
import com.ratelimit.redis.RedisClient;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.providers.PooledConnectionProvider;

public enum RedisConfig {

    INSTANCE; // Single instance

    private final RedisClient redisClient;

    RedisConfig() {
        String host = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        int port = 6379;

        HostAndPort address = new HostAndPort(host, port);
        PooledConnectionProvider provider = new PooledConnectionProvider(address);
        UnifiedJedis jedis = new UnifiedJedis(provider);

        this.redisClient = new JedisRedisClient(jedis);

        System.out.println("Redis Client Initialized");
    }

    public RedisClient getClient() {
        return redisClient;
    }
}
