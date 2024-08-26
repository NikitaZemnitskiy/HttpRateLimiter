package com.zemnitskiy.httpratelimiter.ratelimiter.fixedwindow;

import com.github.benmanes.caffeine.cache.Cache;
import com.zemnitskiy.httpratelimiter.strategy.RateLimitExceededException;
import com.zemnitskiy.httpratelimiter.strategy.RateLimiterStrategy;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

public final class FixedWindowRateLimiter implements RateLimiterStrategy {

  @Value("${rateLimiter.maxRequestsPerPeriod}")
  private int maxRequests;

  @Value("${rateLimiter.basePeriod}")
  private Duration basePeriod;

  private final Cache<String, FixedWindowRateLimiterData> cache;

  private final Logger log = LoggerFactory.getLogger(FixedWindowRateLimiter.class);

  public FixedWindowRateLimiter(Cache<String, FixedWindowRateLimiterData> cache) {
    this.cache = cache;
  }

  @PostConstruct
  public void validateProperties() {
    if (maxRequests <= 0) {
      throw new IllegalArgumentException("maxRequestsPerPeriod must be greater than 0");
    }
    if (basePeriod == null){
      throw new IllegalArgumentException("basePeriod must be set");
    }
  }

  @Override
  public void allowRequest(String key) {
    FixedWindowRateLimiterData currentKeyData = cache.get(key,
        _ -> new FixedWindowRateLimiterData(new AtomicInteger(0), new AtomicLong(System.currentTimeMillis())));
    log.trace("Attempting to allow request for key: {}. Current count: {}", key,
        currentKeyData.counter().get());

    while (true) {
      int data = currentKeyData.counter().get();
      if (data < maxRequests) {
        if (currentKeyData.counter().compareAndExchange(data, data + 1) == data) {
          log.trace("Request allowed for key: {}. New count: {}", key, data + 1);
          return;
        }
      } else {
        log.trace("Rate limit exceeded for key: {}, Max requests: {}", key, maxRequests);
        int timeout = (int) ((int) basePeriod.toSeconds() - (System.currentTimeMillis() - currentKeyData.startTime().get())/1000L);
        throw new RateLimitExceededException(
            "Too many requests. You have only " + maxRequests + " requests." + " for "
                + basePeriod.toSeconds() + " seconds you could make new request in " + timeout + " seconds",timeout
            );
      }
    }
  }
}
