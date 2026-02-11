package com.ratelimit.api;

public record UserContext(String userId, String clientIp, String apiKey) {}
