package com.zemnitskiy.httpratelimiter.strategy;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import com.zemnitskiy.httpratelimiter.exception.RateLimitExceededException;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("fixedWindowRateLimiter")
public final class FixedWindowRateLimiter implements RateLimiterStrategy {

  @Value("${baseMaxRequestsPerPeriod}")
  private int maxRequests;

  @Value("${basePeriod}")
  private long basePeriod;

  private Cache<String, FixedWindowRateLimiterData> cache;

  @PostConstruct
  public void init() {
    this.cache = Caffeine.newBuilder()
        .expireAfterWrite(basePeriod, TimeUnit.NANOSECONDS)
        .build();
  }

  @Override
  public synchronized void allowRequestOrThrowException(String key) {
    long now = System.nanoTime();
    FixedWindowRateLimiterData currentKeyData = cache.get(key,
        _ -> new FixedWindowRateLimiterData());

    if (now - currentKeyData.getFirstRequestTimeInPeriod() > basePeriod) {
      currentKeyData.setRequestCount(1);
      currentKeyData.setFirstRequestTimeInPeriod(now);
      return;
    } else {
      if (currentKeyData.getRequestCount() < maxRequests) {
        currentKeyData.increaseRequestCount();
        return;
      }
    }

    throw new RateLimitExceededException("Too many requests. You have only " + maxRequests + " requests." + " for " + basePeriod/1000000000 + " seconds");
  }
}

final class FixedWindowRateLimiterData {

  private long firstRequestTimeInPeriod = 0;
  private int requestCount = 1;

  public long getFirstRequestTimeInPeriod() {
    return firstRequestTimeInPeriod;
  }

  public int getRequestCount() {
    return requestCount;
  }

  public void setFirstRequestTimeInPeriod(long firstRequestTimeInPeriod) {
    this.firstRequestTimeInPeriod = firstRequestTimeInPeriod;
  }

  public void setRequestCount(int requestCount) {
    this.requestCount = requestCount;
  }

  public void increaseRequestCount() {
    requestCount++;
  }
}
