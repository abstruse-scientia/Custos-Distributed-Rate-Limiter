local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])
local state = redis.call('HMGET', key, 'tokens', 'last_leak_time_ms')
local tokens = tonumber(state[1])
local last_leak_time_ms = tonumber(state[2])
if not tokens or not last_leak_time_ms then
    tokens = 0
    last_leak_time_ms = now
end
local elapsedMs = math.max(0, now - last_leak_time_ms)
local leakedAmount = (elapsedMs / 1000) * rate
tokens = math.max(0, tokens - leakedAmount)
last_leak_time_ms = now
if (tokens + requested) <= capacity then
    tokens = tokens + requested
    redis.call('HMSET', key, 'tokens', tokens, 'last_leak_time_ms', last_leak_time_ms)
    local ttl = math.ceil(capacity / rate) + 2 -- + 2 to avoid redis latency causing unfair rate limit
    redis.call('EXPIRE', key, ttl)
    return {1, 0}
else
    local overflow = (tokens + requested) - capacity
    local leak_time_per_request = (1.0 / rate) * 1000
    local total_leak_time_ms = overflow * leak_time_per_request
    local retry_after_seconds = math.max(1, math.ceil(total_leak_time_ms/ 1000))
    return {0, retry_after_seconds}
end
