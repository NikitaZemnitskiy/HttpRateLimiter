local key = KEYS[1]
local maxRequests = tonumber(ARGV[1])
local windowSize = tonumber(ARGV[2])

local time = redis.call("TIME")  -- Current time in seconds
local currentTime = tonumber(time[1]) * 1000 + tonumber(time[2]) / 1000 -- Current time in microseconds

local windowStart = currentTime - windowSize

local removedCount = redis.call("ZREMRANGEBYSCORE", key, 0, windowStart)
redis.log(redis.LOG_NOTICE, "Removed elements: " .. removedCount)


local currentCount = redis.call("ZCARD", key)
redis.log(redis.LOG_NOTICE, "Current time (ms): " .. currentTime)
redis.log(redis.LOG_NOTICE, "Window start (ms): " .. windowStart)
redis.log(redis.LOG_NOTICE, "Current count: " .. currentCount)
redis.log(redis.LOG_NOTICE, "Window size (ms): " .. windowSize)

if currentCount < maxRequests then
    redis.call("ZADD", key, currentTime, currentTime)
    redis.call('EXPIRE', key, windowSize / 1000)
    return 0
else
    local oldestRequestTime = tonumber(redis.call("ZRANGE", key, 0, 0, "WITHSCORES")[2])
    redis.log(redis.LOG_NOTICE, "Oldest request time (ms): " .. oldestRequestTime)
    local retryAfter = math.ceil((oldestRequestTime + windowSize - currentTime))
    redis.log(redis.LOG_NOTICE, "Retry after(ms): " .. retryAfter)
    return retryAfter
end