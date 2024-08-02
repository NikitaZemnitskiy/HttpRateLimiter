package com.zemnitskiy.httpratelimiter.strategy;

public sealed interface RateLimiterStrategy permits FixedWindowRateLimiter, SlidingWindowRateLimiter, SlidingRedisLimiterStrategy   {

  boolean allowRequest();

}
