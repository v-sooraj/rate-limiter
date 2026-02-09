package com.ratelimit.key;

import com.ratelimit.rules.RateLimitRule;

/**
 * A single key resolver that can deterministically generate Redis keys
 * based on multiple identity components: userId, IP, API key, or combinations.
 */
public class FlexibleKeyResolver {

    private final String prefix;

    public FlexibleKeyResolver(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Resolve a Redis key for rate limiting.
     * Any null argument is ignored.
     * Examples:
     * - user only: {rl}:FREE_TIER:user=bob
     * - IP only: {rl}:FREE_TIER:ip=1.2.3.4
     * - user+IP: {rl}:FREE_TIER:user=bob:ip=1.2.3.4
     */
    public String resolve(String userId, String ip, String apiKey, RateLimitRule rule) {
        StringBuilder sb = new StringBuilder();
        sb.append("{").append(prefix).append("}:").append(rule.name());

        if (userId != null && !userId.isBlank()) sb.append(":user=").append(userId);
        if (ip != null && !ip.isBlank()) sb.append(":ip=").append(ip);
        if (apiKey != null && !apiKey.isBlank()) sb.append(":key=").append(apiKey);

        return sb.toString();
    }
}
