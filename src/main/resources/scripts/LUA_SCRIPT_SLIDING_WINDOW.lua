local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local windowDurationMs = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local uniqueMember = ARGV[4]

local windowStart = now - windowDurationMs

-- remove old requests before the start of the window
redis.call('ZREMRANGEBYSCORE', key, 0 , windowStart) -- redis.call('ZREMRANGEBYSCORE', key, min, max)

local size = redis.call('ZCARD', key)

if (size < capacity) then
    redis.call('ZADD', key, now, uniqueMember)

    -- configure ttl based on windowDuration since , the only way ttl expires if there comes no new request during window
    -- so all the request is no longer useful
    redis.call('PEXPIRE', key, windowDurationMs)
    return {1, 0}
else
    local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
    local oldestRequestTime = oldest[2]
    local timeUntilOldestExpires = (oldestRequestTime + windowDurationMs) - now
    local retrySeconds = math.max(1, math.floor((timeUntilOldestExpires + 999) / 1000))
    return {0, retrySeconds}
end