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
    FixedWindowRateLimiterData currentKeyData = cache.get(key,
        _ -> new FixedWindowRateLimiterData());

    if (currentKeyData.getRequestCount() < maxRequests) {
      currentKeyData.increaseRequestCount();
      return;
    }

    throw new RateLimitExceededException(
        "Too many requests. You have only " + maxRequests + " requests." + " for "
            + basePeriod / 1000000000 + " seconds");
  }
}

final class FixedWindowRateLimiterData {

  private int requestCount = 0;

  public int getRequestCount() {
    return requestCount;
  }

  public void increaseRequestCount() {
    requestCount++;
  }
}
