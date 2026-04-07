local key = KEYS[1]

local capacity = tonumber(ARGV[1])
local refillRate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

local data = redis.call("HMGET", key, "tokens", "lastRefillTime")

local token = tonumber(data[1])
local lastRefillTime = tonumber(data[2])

if token == nil then
    token = capacity
    lastRefillTime = now
else
    local elapsedTime = math.max(0, now - lastRefillTime)
    local refillTokens = (elapsedTime * refillRate) / 1000
    token = math.min(capacity, token + refillTokens)

end
local allowed = 0
if token >= 1 then
    token = token - 1
    allowed = 1
end

redis.call("HMSET", key, "tokens", token  , "lastRefillTime", now)

redis.call("PEXPIRE", key, 60000)

return allowed