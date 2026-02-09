package com.ratelimit.rules;

public record RateLimitRule(String name, int limit, int windowMs) {
}
