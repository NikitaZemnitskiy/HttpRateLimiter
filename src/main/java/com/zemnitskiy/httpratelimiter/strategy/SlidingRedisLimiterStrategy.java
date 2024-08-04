package com.zemnitskiy.httpratelimiter.strategy;

import com.zemnitskiy.httpratelimiter.service.CacheService;
import java.util.concurrent.atomic.AtomicInteger;

public final class SlidingRedisLimiterStrategy implements RateLimiterStrategy {

  private final long periodInSeconds;
  private final int maxRequests;
  private final String redisKey;
  private final CacheService cacheService;
  private long oldestElement = 0;
  AtomicInteger id = new AtomicInteger(0);

  public SlidingRedisLimiterStrategy(int maxRequests, long period,
      String redisKey, CacheService cacheService) {
    this.periodInSeconds = period / 1000000000;
    this.maxRequests = maxRequests;
    this.redisKey = redisKey;
    this.cacheService = cacheService;
  }

  @Override
  public boolean allowRequest() {
    if (oldestElement > System.currentTimeMillis()) {
      return false;
    }
    cacheService.removeExpiredItems(redisKey);
    if (cacheService.getQueueSize(redisKey) > maxRequests) {
      oldestElement = (long) cacheService.getOldestElementTTL(redisKey);
      return false;
    }
    if (cacheService.addItemToQueue(redisKey, String.valueOf(id), periodInSeconds, maxRequests)) {
      id.incrementAndGet();
      return true;
    }
    return false;
  }
}
