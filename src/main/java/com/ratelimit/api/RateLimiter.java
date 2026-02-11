package com.ratelimit.api;

import com.ratelimit.rules.RateLimitRule;

public interface RateLimiter {
    RateLimitResult evaluate(UserContext userContext, RateLimitRule rule);
}