-- ===========================================================
-- KEYS[1] = unique key (e.g., "RATE:USER:1234")
-- ARGV[1] = window in milliseconds
-- ARGV[2] = limit (max requests allowed)
-- ARGV[3] = cleanup probability 0-100 (default 10)
-- Returns: {allowed (1|0), remaining, retry_after_ms}
-- ===========================================================

local key = KEYS[1]
local window = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])
local cleanup_prob = tonumber(ARGV[3]) or 10

-- =========================
-- Input validation
-- =========================
if not window or window <= 0 then
    return redis.error_reply("ERR invalid window")
end
if not limit or limit <= 0 then
    return redis.error_reply("ERR invalid limit")
end
if cleanup_prob < 0 or cleanup_prob > 100 then
    cleanup_prob = 10
end

-- =========================
-- Current timestamp (ms)
-- =========================
local now = redis.call("TIME")
local current_ts = tonumber(now[1]) * 1000 + math.floor(tonumber(now[2]) / 1000)
local window_start = current_ts - window

-- =========================
-- Probabilistic cleanup (~10% by default)
-- =========================
if cleanup_prob > 0 then
    -- Deterministic hash to prevent cleanup storm
    local key_hash = tonumber(redis.sha1hex(key):sub(1,8), 16) % 100
    if (key_hash + current_ts) % 100 < cleanup_prob then
        redis.call("ZREMRANGEBYSCORE", key, "-inf", window_start)
    end
end

-- =========================
-- Count valid entries only
-- =========================
local current_count = redis.call("ZCOUNT", key, window_start, "+inf")

-- =========================
-- Deny if limit reached
-- =========================
if current_count >= limit then
    local remaining = 0
    local retry_after = 0
    local oldest = redis.call("ZRANGEBYSCORE", key, window_start, "+inf",
                              "WITHSCORES", "LIMIT", 0, 1)
    if #oldest >= 2 then
        local oldest_ts = tonumber(oldest[2])
        retry_after = math.max(0, math.ceil(oldest_ts + window - current_ts))
    else
        retry_after = window -- fail-safe
    end
    return {0, remaining, retry_after}
end

-- =========================
-- Allow request: add unique member
-- =========================
local member = tostring(current_ts) .. ":" .. tostring(now[2])
redis.call("ZADD", key, current_ts, member)

-- =========================
-- Smart TTL management
-- =========================
local ttl = redis.call("PTTL", key)
if ttl == -1 or ttl < window then
    redis.call("PEXPIRE", key, window * 2)
end

-- =========================
-- Calculate remaining
-- =========================
local new_count = current_count + 1
local remaining = limit - new_count

-- =========================
-- Return success
-- =========================
return {1, remaining, 0}
