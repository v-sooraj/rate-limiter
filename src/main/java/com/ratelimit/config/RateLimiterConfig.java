package com.ratelimit.config;

import com.ratelimit.api.FailurePolicy;

public class RateLimiterConfig {

    private final String keyPrefix;
    private final String scope;
    private final FailurePolicy failurePolicy;

    public RateLimiterConfig() {
        this.keyPrefix = System.getenv().getOrDefault("KEY_PREFIX", "rl");
        this.scope = System.getenv().getOrDefault("RATE_LIMIT_SCOPE", "user");
        this.failurePolicy = FailurePolicy.valueOf(
                System.getenv().getOrDefault("FAILURE_POLICY", "FAIL_OPEN")
        );
    }

    public String keyPrefix() { return keyPrefix; }
    public FailurePolicy failurePolicy() { return failurePolicy; }
}