
local current_key = KEYS[1]
local previous_key = KEYS[2]
local capacity = tonumber(ARGV[1])
local previous_weight = tonumber(ARGV[2])
local current_second = tonumber(ARGV[3])


local previous_window_count = tonumber(redis.call('GET', previous_key) or '0')
local current_window_count = tonumber(redis.call('GET', current_key) or '0')

local estimated_total  = (previous_weight * previous_window_count) + current_window_count

if estimated_total >= capacity then
    local retry_after_seconds = 0
    if current_window_count >= capacity then
        retry_after_seconds = 60 - current_second
    else
        local weight_required = (capacity - current_second) / previous_window_count
        local target_second = 60.0 - (weight_required * 60.0)
        retry_after_seconds = math.ceil(target_second - current_second) + 1
    end

    return {0, retry_after_seconds}
end

redis.call('INCR', current_key)

if (current_window_count == 0) then
    redis.call('EXPIRE', current_key, 120)
end

return {1, 0}