local key = KEYS[1]
local maxRequests = tonumber(ARGV[1])
local windowSize = tonumber(ARGV[2])

local currentTime = tonumber(redis.call("TIME")[1]) * 1000
local windowStart = currentTime - windowSize

redis.call("ZREMRANGEBYSCORE", key, 0, windowStart)

local currentCount = redis.call("ZCARD", key)

if currentCount < maxRequests then
    redis.call("ZADD", key, currentTime, currentTime)
    redis.call('EXPIRE', key, window)
    return 1
else
    return 0
end