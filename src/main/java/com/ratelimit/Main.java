package com.ratelimit;

import com.ratelimit.api.RateLimitResult;
import com.ratelimit.config.RateLimiterConfig;
import com.ratelimit.config.RedisConfig;
import com.ratelimit.engine.RateLimitEngine;
import com.ratelimit.key.FlexibleKeyResolver;
import com.ratelimit.rules.RateLimitRule;
import com.ratelimit.strategy.RateLimitStrategy;
import com.ratelimit.strategy.SlidingWindowLogStrategy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        RateLimiterConfig config = new RateLimiterConfig();

        // 1. Initialize Redis strategy & engine
        RateLimitStrategy strategy = new SlidingWindowLogStrategy(RedisConfig.INSTANCE.getClient());
        RateLimitEngine engine = new RateLimitEngine(strategy, config.failurePolicy());

        // 2. Define rules
        RateLimitRule freeTier = new RateLimitRule("FREE_TIER", 5, 10_000);
        RateLimitRule proTier = new RateLimitRule("PRO_TIER", 50, 10_000);

        // 3. Resolve user context & scope
        String userTier = System.getenv().getOrDefault("USER_TIER", "FREE");
        String userId = System.getenv().getOrDefault("USER_ID", "anon-1");
        String clientIp = System.getenv().getOrDefault("CLIENT_IP", null);
        String apiKey = System.getenv().getOrDefault("API_KEY", null);

        RateLimitRule activeRule = userTier.equalsIgnoreCase("PRO") ? proTier : freeTier;

        // 4. Use flexible key resolver
        FlexibleKeyResolver resolver = new FlexibleKeyResolver(config.keyPrefix());
        String key = resolver.resolve(userId, clientIp, apiKey, activeRule);

        System.out.printf("System active. Tier: %s | Rule: %s | Key: %s%n",
                userTier, activeRule.name(), key);

        // 5. Simulate requests
        ExecutorService executor = Executors.newFixedThreadPool(3);
        for (int i = 1; i <= 10; i++) {
            final int requestId = i;
            executor.submit(() -> {
                RateLimitResult result = engine.evaluate(key, activeRule);
                String status = result.allowed() ? "ALLOWED" : "BLOCKED";
                System.out.printf("[Req #%d] %s | Remaining: %d | RetryAfterMs: %d%n",
                        requestId, status, result.remaining(), result.retryAfterMs());
            });
            Thread.sleep(200);
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("Simulation complete.");
    }
}
