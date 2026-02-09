package com.ratelimit.service;

public interface RateLimit {
    boolean isAllowed(String clientId, int limit, int windowMs);
}
