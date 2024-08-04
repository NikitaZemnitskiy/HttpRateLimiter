package com.zemnitskiy.httpratelimiter.strategy;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class FixedWindowRateLimiter implements RateLimiterStrategy {

  private final int maxRequests;
  private final AtomicLong firstRequestTimeInPeriod = new AtomicLong(0);
  private final AtomicInteger requestCount = new AtomicInteger(0);
  private final long period;

  public FixedWindowRateLimiter(int maxRequests, long period) {
    this.maxRequests = maxRequests;
    this.period = period;
  }

  @Override
  public boolean allowRequest() {
    long now = System.nanoTime();
    long newFirstRequestTime = firstRequestTimeInPeriod.updateAndGet(
        f -> now - f > period ? now : f);

    if (newFirstRequestTime == now) {
      requestCount.set(1);
      return true;
    }

    while (true) {
      int currentRequestCount = requestCount.get();
      if (currentRequestCount >= maxRequests) {
        return false;
      }
      if (requestCount.compareAndSet(currentRequestCount, currentRequestCount + 1)) {
        return true;
      }
    }
  }


}
