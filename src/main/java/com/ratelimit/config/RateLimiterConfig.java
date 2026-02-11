package com.ratelimit.config;

import com.ratelimit.api.FailurePolicy;

import java.time.Duration;

public class RateLimiterConfig {

    private final String keyPrefix;
    private final FailurePolicy failurePolicy;

    // L1 Cache Settings
    private final boolean l1Enabled;
    private final long l1MaxEntries;
    private final Duration l1Ttl;

    public RateLimiterConfig() {
        this.keyPrefix = System.getenv().getOrDefault("KEY_PREFIX", "rl");
        this.failurePolicy = FailurePolicy.valueOf(
                System.getenv().getOrDefault("FAILURE_POLICY", "FAIL_OPEN")
        );

        // L1 Shield Config (Memory Bounded)
        this.l1Enabled = Boolean.parseBoolean(
                System.getenv().getOrDefault("RL_L1_ENABLED", "true")
        );
        this.l1MaxEntries = Long.parseLong(
                System.getenv().getOrDefault("RL_L1_MAX_ENTRIES", "50000")
        );
        this.l1Ttl = Duration.ofSeconds(Long.parseLong(
                System.getenv().getOrDefault("RL_L1_TTL_SECONDS", "5")
        ));

    }

    public String keyPrefix() { return keyPrefix; }
    public FailurePolicy failurePolicy() { return failurePolicy; }
    public boolean isL1Enabled() { return l1Enabled; }
    public long l1MaxEntries() { return l1MaxEntries; }
    public Duration l1Ttl() { return l1Ttl; }
}