package com.ratelimit.strategy;

import com.ratelimit.api.RateLimitResult;
import com.ratelimit.rules.RateLimitRule;

public interface RateLimitStrategy {
    RateLimitResult execute(String key, RateLimitRule rule);
}

