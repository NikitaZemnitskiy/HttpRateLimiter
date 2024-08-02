package com.zemnitskiy.httpratelimiter.strategy;

public final class FixedWindowRateLimiter implements RateLimiterStrategy {

  private final int maxRequests;
  private long firstRequestTimeInPeriod;
  private int requestCount;
  private final long period;

  public FixedWindowRateLimiter(int maxRequests, long period) {
    this.maxRequests = maxRequests;
    this.period = period;
    this.firstRequestTimeInPeriod = 0;
    this.requestCount = 0;
  }

  @Override
  public synchronized boolean allowRequest() {
    long now = System.nanoTime();
    if (now - firstRequestTimeInPeriod > period) {
      firstRequestTimeInPeriod = now;
      requestCount = 1;
      return true;
    }

    if (requestCount < maxRequests) {
      requestCount++;
      return true;
    }

    return false;
  }
}
