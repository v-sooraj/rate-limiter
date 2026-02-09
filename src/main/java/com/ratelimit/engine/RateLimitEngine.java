package com.ratelimit.engine;

import com.ratelimit.api.FailurePolicy;
import com.ratelimit.api.RateLimitResult;
import com.ratelimit.api.RateLimiter;
import com.ratelimit.rules.RateLimitRule;
import com.ratelimit.strategy.RateLimitStrategy;

public class RateLimitEngine implements RateLimiter {

    private final RateLimitStrategy strategy;
    private final FailurePolicy failurePolicy;

    public RateLimitEngine(RateLimitStrategy strategy,
                           FailurePolicy failurePolicy) {
        this.strategy = strategy;
        this.failurePolicy = failurePolicy;
    }

    @Override
    public RateLimitResult evaluate(String key, RateLimitRule rule) {
        try {
            return strategy.execute(key, rule);
        } catch (Exception e) {
            return failurePolicy == FailurePolicy.FAIL_OPEN
                    ? RateLimitResult.allowAll(rule.name())
                    : RateLimitResult.blockAll(rule.name());
        }
    }
}
