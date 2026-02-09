package com.ratelimit.config;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.providers.PooledConnectionProvider;

public enum RedisConfig {

    INSTANCE; // Single instance
    private final UnifiedJedis jedis;

    // The constructor is private and called once by the JVM
    RedisConfig() {
        String host = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        int port = 6379;

        HostAndPort address = new HostAndPort(host, port);
        PooledConnectionProvider provider = new PooledConnectionProvider(address);
        this.jedis = new UnifiedJedis(provider);

        System.out.println("Redis Client Initialized via Enum Singleton");
    }

    public UnifiedJedis getClient() {
        return jedis;
    }
}
