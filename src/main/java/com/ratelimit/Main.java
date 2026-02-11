package com.ratelimit;

import com.ratelimit.api.RateLimitResult;
import com.ratelimit.api.UserContext;
import com.ratelimit.clients.RedisClient;
import com.ratelimit.config.RateLimiterConfig;
import com.ratelimit.config.RedisConfig;
import com.ratelimit.engine.RateLimitEngine;
import com.ratelimit.key.FlexibleKeyResolver;
import com.ratelimit.rules.RateLimitRule;
import com.ratelimit.strategy.SlidingWindowLogStrategy;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {

        // 1. Initialize Redis strategy & engine
        RedisClient redisClient = RedisConfig.INSTANCE.getClient();

        RateLimitRule freeTier = new RateLimitRule("FREE_TIER", 5, 10_000);
        RateLimitRule proTier = new RateLimitRule("PRO_TIER", 50, 10_000);

        // Ideally fetched from requests(headers)
        String userTier = System.getenv().getOrDefault("USER_TIER", "FREE");
        String userId = System.getenv().getOrDefault("USER_ID", "anon-1");
        String clientIp = System.getenv().getOrDefault("CLIENT_IP", null);
        String apiKey = System.getenv().getOrDefault("API_KEY", null);

        RateLimitRule activeRule = userTier.equalsIgnoreCase("PRO") ? proTier : freeTier;
        UserContext userContext = new UserContext(userId, clientIp, apiKey);

        RateLimiterConfig config = new RateLimiterConfig();
        RateLimitEngine engine = RateLimitEngine.builder()
                .withConfig(config)
                .withKeyResolver(new FlexibleKeyResolver(config.keyPrefix()))
                .withStrategy(new SlidingWindowLogStrategy(redisClient))
                .withDefaultL1Cache()
                .build();

        // 5. Simulate requests
        try (ExecutorService executor = Executors.newFixedThreadPool(3)) {
            for (int i = 1; i <= 10; i++) {
                final int requestId = i;
                executor.submit(() -> {
                    RateLimitResult result = engine.evaluate(userContext, activeRule);
                    String status = result.allowed() ? "ALLOWED" : "BLOCKED";
                    System.out.printf("[Req #%d] %s | Remaining: %d | RetryAfterMs: %d%n",
                            requestId, status, result.remaining(), result.retryAfterMs());
                });
                Thread.sleep(200);
            }
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.print(Arrays.toString(e.getStackTrace()));
        }
        System.out.println("Simulation complete.");
    }
}

