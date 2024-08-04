package com.zemnitskiy.httpratelimiter.strategy;

import java.util.concurrent.ConcurrentLinkedQueue;

public final class SlidingWindowRateLimiter implements RateLimiterStrategy {

  private final int maxRequests;
  private final long period;
  private final ConcurrentLinkedQueue<Long> requestTimestamps;

  public SlidingWindowRateLimiter(int maxRequests, long period) {
    this.maxRequests = maxRequests;
    this.period = period;
    this.requestTimestamps = new ConcurrentLinkedQueue<>();
  }

  @Override
  public boolean allowRequest() {
    long now = System.nanoTime();
    long windowStart = now - period;

    requestTimestamps.removeIf(timestamp -> timestamp < windowStart);
    if (requestTimestamps.size() >= maxRequests) {
      return false;
    }
    synchronized (this) {
      if (requestTimestamps.size() < maxRequests) {
        requestTimestamps.add(now);
        return true;
      }
    }
    return false;
  }

}
