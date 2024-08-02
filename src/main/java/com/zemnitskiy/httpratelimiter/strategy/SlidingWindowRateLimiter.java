package com.zemnitskiy.httpratelimiter.strategy;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public final class SlidingWindowRateLimiter implements RateLimiterStrategy {

  private final int maxRequests;
  private final long period;
  private final ConcurrentLinkedQueue<Long> requestTimestamps;
  private final AtomicInteger requestCount;

  public SlidingWindowRateLimiter(int maxRequests, long period) {
    this.maxRequests = maxRequests;
    this.period = period;
    this.requestTimestamps = new ConcurrentLinkedQueue<>();
    this.requestCount = new AtomicInteger(0);
  }

  @Override
  public synchronized boolean allowRequest() {
    long now = System.nanoTime();
    long windowStart = now - period;

    while (!requestTimestamps.isEmpty() && requestTimestamps.peek() < windowStart) {
      requestTimestamps.poll();
      requestCount.decrementAndGet();
    }

    if (requestCount.get() < maxRequests) {
      requestTimestamps.add(now);
      requestCount.incrementAndGet();
      return true;
    }

    return false;
  }

}
