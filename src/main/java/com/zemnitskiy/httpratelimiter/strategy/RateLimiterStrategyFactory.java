package com.zemnitskiy.httpratelimiter.strategy;

import com.zemnitskiy.httpratelimiter.service.CacheService;
import com.zemnitskiy.httpratelimiter.utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RateLimiterStrategyFactory {

  public RateLimiterStrategyFactory(CacheService cacheService) {
    this.cacheService = cacheService;
  }

  private final CacheService cacheService;

  @Value("${rateLimiterStrategy}")
  private String strategyType;

  @Value("${baseMaxRequestsPerPeriod}")
  private int maxRequests;

  @Value("${basePeriod}")
  private String basePeriod;

  public RateLimiterStrategy createStrategy(String key) {
    long period = Utils.getBasePeriod(basePeriod);
    return switch (strategyType) {
      case "fixed" -> new FixedWindowRateLimiter(maxRequests, period);
      case "sliding" -> new SlidingWindowRateLimiter(maxRequests, period);
      case "redis" -> new SlidingRedisLimiterStrategy(maxRequests, period, key, cacheService);
      //Leaky bucket could be implemented in future
      default -> throw new IllegalArgumentException("Unknown strategy type: " + strategyType);
    };
  }
}
