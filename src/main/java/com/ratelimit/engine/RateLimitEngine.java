package com.ratelimit.engine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ratelimit.api.FailurePolicy;
import com.ratelimit.api.RateLimitResult;
import com.ratelimit.api.RateLimiter;
import com.ratelimit.api.UserContext;
import com.ratelimit.config.RateLimiterConfig;
import com.ratelimit.key.FlexibleKeyResolver;
import com.ratelimit.rules.RateLimitRule;
import com.ratelimit.strategy.RateLimitStrategy;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class RateLimitEngine implements RateLimiter {

    private final FlexibleKeyResolver keyResolver;
    private final RateLimitStrategy strategy;
    private final FailurePolicy failurePolicy;
    private final Cache<String, RateLimitResult> negativeCache;

    private RateLimitEngine(FlexibleKeyResolver keyResolver,
                           RateLimitStrategy strategy,
                           FailurePolicy failurePolicy,
                           Cache<String, RateLimitResult> negativeCache) {
        this.keyResolver = keyResolver;
        this.strategy = strategy;
        this.failurePolicy = failurePolicy;
        this.negativeCache = negativeCache;
    }

    private RateLimitEngine(Builder builder) {
        this.keyResolver = builder.keyResolver;
        this.strategy = builder.strategy;
        this.failurePolicy = builder.config.failurePolicy();
        this.negativeCache = builder.l1Cache;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RateLimitResult evaluate(UserContext context, RateLimitRule rule) {
        String key = keyResolver.resolve(context.userId(), context.clientIp(), context.apiKey(), rule);
        return evaluateInternal(rule, key);
    }

    private RateLimitResult evaluateInternal(RateLimitRule rule, String key) {
        // L1 SHIELD CHECK: Prevents unnecessary network RTT to Redis
        if (negativeCache != null) {
            RateLimitResult limitResult = negativeCache.getIfPresent(key);
            if (Objects.nonNull(limitResult) && limitResult.retryAfterMs() > 0) {
                long waitTime = limitResult.retryAfterMs() - System.currentTimeMillis();
                if (waitTime > 0) {
                    // Short-circuit: Logic ends here if user is already known to be blocked
                    return RateLimitResult.blocked(rule.name(), System.currentTimeMillis() + limitResult.retryAfterMs());
                } else {
                    negativeCache.invalidate(key);
                }
            }
        }
        try {
            RateLimitResult result = strategy.execute(key, rule);
            if (!result.allowed() && negativeCache != null) {
                // We create a "Cache Entry" version of the result where
                // retryAfterMs is the absolute time the block ends.
                RateLimitResult absoluteBlock = RateLimitResult.blocked(
                        rule.name(),
                        System.currentTimeMillis() + result.retryAfterMs()
                );
                negativeCache.put(key, absoluteBlock);
            }
            return result;
        } catch (Exception e) {
            return handleFailure(rule, e);
        }
    }

    private RateLimitResult handleFailure(RateLimitRule rule, Exception e) {
        return failurePolicy == FailurePolicy.FAIL_OPEN ? RateLimitResult.allowAll(rule.name())
                : RateLimitResult.blockAll(rule.name());
    }

    public static class Builder {
        private FlexibleKeyResolver keyResolver;
        private RateLimitStrategy strategy;
        private RateLimiterConfig config = new RateLimiterConfig();
        private Cache<String, RateLimitResult> l1Cache;

        private Builder() {
        }

        public Builder withKeyResolver(FlexibleKeyResolver keyResolver) {
            this.keyResolver = keyResolver;
            return this;
        }

        public Builder withStrategy(RateLimitStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        public Builder withConfig(RateLimiterConfig config) {
            this.config = config;
            return this;
        }

        public Builder withL1Cache(Cache<String, RateLimitResult> cache) {
            this.l1Cache = cache;
            return this;
        }

        public Builder withDefaultL1Cache() {
            this.l1Cache = Caffeine.newBuilder()
                    .maximumSize(100_000)
                    .expireAfterWrite(1, TimeUnit.MINUTES)
                    .build();
            return this;
        }

        public RateLimitEngine build() {
            if (strategy == null) throw new IllegalStateException("Strategy is required");
            if (keyResolver == null) throw new IllegalStateException("KeyResolver is required");
            return new RateLimitEngine(this);
        }
    }
}