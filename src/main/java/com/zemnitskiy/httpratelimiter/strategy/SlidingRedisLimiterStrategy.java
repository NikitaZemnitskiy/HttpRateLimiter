package com.zemnitskiy.httpratelimiter.strategy;

import com.zemnitskiy.httpratelimiter.service.CacheService;

public final class SlidingRedisLimiterStrategy implements RateLimiterStrategy{

  private final long period;
  private final int maxRequests;
  private final String redisKey;
  private final CacheService cacheService;

  public SlidingRedisLimiterStrategy(long period, int maxRequests,
      String redisKey, CacheService cacheService) {
    this.period = period;
    this.maxRequests = maxRequests;
    this.redisKey = redisKey;
    this.cacheService = cacheService;
  }

  @Override
  public boolean allowRequest() {
    long requestCount = cacheService.get(redisKey) == null ? 0 : Long.parseLong(cacheService.get(redisKey));
    if (requestCount < maxRequests) {
      cacheService.set(redisKey, String.valueOf(requestCount + 1), (int) (period / 1_000_000_000));
      return true;
    }
    return false;
  }
}
