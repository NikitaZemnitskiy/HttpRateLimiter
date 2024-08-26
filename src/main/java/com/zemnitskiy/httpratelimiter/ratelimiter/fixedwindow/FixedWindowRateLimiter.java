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

/**
 * The {@code FixedWindowRateLimiter} class implements a rate limiting strategy based on the fixed window algorithm.
 * It allows a maximum number of requests within a specified time period, known as the window.
 *
 * <p>This class uses Caffeine cache to store and manage the state of the rate limit for each client.
 * It ensures that requests are tracked per key and the limit is enforced based on the configured properties.
 *
 * <p>The class is final, meaning it cannot be subclassed.
 */
public final class FixedWindowRateLimiter implements RateLimiterStrategy {

  @Value("${rateLimiter.maxRequestsPerPeriod}")
  private int maxRequests;

  @Value("${rateLimiter.basePeriod}")
  private Duration basePeriod;

  private final Cache<String, FixedWindowRateLimiterData> cache;

  private final Logger log = LoggerFactory.getLogger(FixedWindowRateLimiter.class);

  /**
   * Constructs a new {@code FixedWindowRateLimiter} with the provided Caffeine cache.
   *
   * @param cache the cache to use for storing rate limiter data
   */
  public FixedWindowRateLimiter(Cache<String, FixedWindowRateLimiterData> cache) {
    this.cache = cache;
  }

  /**
   * Validates the rate limiter properties after the class has been constructed.
   *
   * <p>This method ensures that the {@code maxRequests} is greater than 0 and that {@code basePeriod} is not null.
   * It is annotated with {@code @PostConstruct}, meaning it is executed after the dependency injection is complete.
   *
   * @throws IllegalArgumentException if {@code maxRequests} is less than or equal to 0 or if {@code basePeriod} is null
   */
  @PostConstruct
  public void validateProperties() {
    if (maxRequests <= 0) {
      throw new IllegalArgumentException("maxRequestsPerPeriod must be greater than 0");
    }
    if (basePeriod == null) {
      throw new IllegalArgumentException("basePeriod must be set");
    }
  }

  /**
   * Attempts to allow a request for the given key.
   *
   * <p>If the number of requests within the current window for the key is below the maximum allowed,
   * the request is allowed, and the count is incremented. If the limit has been reached, a
   * {@link RateLimitExceededException} is thrown, indicating that the request cannot be processed
   * until the next window period.
   *
   * @param key the unique key representing the client or request source
   * @throws RateLimitExceededException if the rate limit for the key has been exceeded
   */
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
                + basePeriod.toSeconds() + " seconds you could make new request in " + timeout + " seconds", timeout
        );
      }
    }
  }
}
