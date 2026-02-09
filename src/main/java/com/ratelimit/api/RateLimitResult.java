package com.ratelimit.api;

public record RateLimitResult(
        boolean allowed,
        long remaining,
        long retryAfterMs,
        long resetAtMs,
        String ruleName
) {

    public static RateLimitResult allowAll(String ruleName) {
        return new RateLimitResult(
                true,
                Long.MAX_VALUE,
                0,
                0,
                ruleName
        );
    }

    public static RateLimitResult blockAll(String ruleName) {
        return new RateLimitResult(
                false,
                0,
                -1,
                -1,
                ruleName
        );
    }
}