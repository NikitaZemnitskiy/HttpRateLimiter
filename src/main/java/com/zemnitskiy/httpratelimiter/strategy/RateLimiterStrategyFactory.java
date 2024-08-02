package com.zemnitskiy.httpratelimiter.strategy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RateLimiterStrategyFactory {

  @Value("${rateLimiterStrategy}")
  private String strategyType;

  @Value("${baseMaxRequestsPerPeriod}")
  private int maxRequests;

  @Value("${basePeriod}")
  private long period;

  public RateLimiterStrategy createStrategy() {
    return switch (strategyType) {
      case "fixed" -> new FixedWindowRateLimiter(maxRequests, period);
      case "sliding" -> new SlidingWindowRateLimiter(maxRequests, period);
      default -> throw new IllegalArgumentException("Unknown strategy type: " + strategyType);
    };
  }
}
