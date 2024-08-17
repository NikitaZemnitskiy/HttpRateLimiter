package com.zemnitskiy.httpratelimiter.ratelimiter.fixedwindow;

import com.github.benmanes.caffeine.cache.Cache;
import com.zemnitskiy.httpratelimiter.strategy.RateLimitExceededException;
import com.zemnitskiy.httpratelimiter.strategy.RateLimiterStrategy;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("fixedWindowRateLimiter")
public final class FixedWindowRateLimiter implements RateLimiterStrategy {

  @Value("${rateLimiter.maxRequestsPerPeriod}")
  private int maxRequests;

  @Value("${rateLimiter.basePeriod}")
  private Duration basePeriod;

  private final Cache<String, AtomicInteger> cache;

  private final Logger log = LoggerFactory.getLogger(FixedWindowRateLimiter.class);

  public FixedWindowRateLimiter(Cache<String, AtomicInteger> cache) {
    this.cache = cache;
  }

  @Override
  public void allowRequest(String key) {
    AtomicInteger currentKeyData = cache.get(key,
        _ -> new AtomicInteger(0));
    log.trace("Attempting to allow request for key: {}. Current count: {}", key,
        currentKeyData.get());

    while (true) {
      int data = currentKeyData.get();
      if (data < maxRequests) {
        if (currentKeyData.compareAndExchange(data, data + 1) == data) {
          log.trace("Request allowed for key: {}. New count: {}", key, data + 1);
          return;
        }
      } else {
        log.trace("Rate limit exceeded for key: {}, Max requests: {}", key, maxRequests);
        throw new RateLimitExceededException(
            "Too many requests. You have only " + maxRequests + " requests." + " for "
                + basePeriod.toSeconds() + " seconds", 5);
      }
    }
  }
}
