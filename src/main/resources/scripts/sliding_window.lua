-- sliding_window.lua
local redis_time = redis.call('TIME')
local now = (tonumber(redis_time[1]) * 1000) + math.floor(tonumber(redis_time[2]) / 1000)

local key = KEYS[1]
local window = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])

redis.call('ZREMRANGEBYSCORE', key, 0, now - window)

local current_count = redis.call('ZCARD', key)

if current_count < limit then
    redis.call('ZADD', key, now, now)
    redis.call('EXPIRE', key, math.ceil(window / 1000) + 1)
    return 1
else
    return 0
end