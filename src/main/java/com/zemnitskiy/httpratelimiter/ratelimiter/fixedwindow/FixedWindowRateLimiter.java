package com.zemnitskiy.httpratelimiter.ratelimiter.fixedwindow;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zemnitskiy.httpratelimiter.strategy.RateLimitExceededException;
import com.zemnitskiy.httpratelimiter.strategy.RateLimiterStrategy;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code FixedWindowRateLimiter} class implements a rate limiting strategy based on the fixed
 * window algorithm. It allows a maximum number of requests within a specified time period, known as
 * the window.
 *
 * <p>This class uses Caffeine cache to store and manage the state of the rate limit for each
 * client. It ensures that requests are tracked per key and the limit is enforced based on the
 * configured properties.
 *
 */
public final class FixedWindowRateLimiter implements RateLimiterStrategy {

  private final int maxRequests;

  private final Duration basePeriod;

  private final Cache<String, FixedWindowRateLimiterData> cache;

  private final Logger log = LoggerFactory.getLogger(FixedWindowRateLimiter.class);

  public FixedWindowRateLimiter(int maxRequests,
      Duration basePeriod) {
    if (maxRequests <= 0) {
      throw new IllegalArgumentException("maxRequestsPerPeriod must be greater than 0");
    }
    if (basePeriod == null) {
      throw new IllegalArgumentException("basePeriod must be set");
    }
    this.cache = Caffeine.newBuilder()
        .expireAfterWrite(basePeriod)
        .build();
    this.maxRequests = maxRequests;
    this.basePeriod = basePeriod;
  }

  /**
   * Attempts to allow a request for the given key based on a fixed window rate limiting strategy.
   *
   * <p>The fixed window rate limiting strategy works by dividing time into fixed-size windows (e.g., 1 minute) and
   * counting the number of requests within each window. If the number of requests exceeds the allowed limit within the
   * current window, additional requests are denied until the next window starts. The request count is reset at the start
   * of each new window.
   *
   * <p>If the number of requests within the current window for the key is below the maximum allowed, the request is
   * allowed, and the count is incremented. If the limit has been reached, a {@link RateLimitExceededException} is thrown,
   * indicating that the request cannot be processed until the next window period.
   *
   * <p>For more information on fixed window rate limiting, refer to the following resources:
   * <ul>
   *   <li><a href="https://en.wikipedia.org/wiki/Token_bucket_algorithm">Token Bucket Algorithm - Wikipedia</a></li>
   *   <li><a href="https://medium.com/@rakesh.singh.07/fixed-window-rate-limiting-in-spring-boot-a2f0c7e8f917">Fixed Window Rate Limiting in Spring Boot - Medium</a></li>
   * </ul>
   *
   * @param key the unique key representing the client or request source
   * @throws RateLimitExceededException if the rate limit for the key has been exceeded. The exception message
   * includes the maximum number of allowed requests, the time period for the limit, and the time remaining until
   * requests can be made again.
   */
  @Override
  public void allowRequest(String key) {
    FixedWindowRateLimiterData currentKeyData = cache.get(key,
        _ -> new FixedWindowRateLimiterData(new AtomicInteger(0),
            System.currentTimeMillis()));
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
        int timeout = (int) ((int) basePeriod.toSeconds()
            - (System.currentTimeMillis() - currentKeyData.startTime()) / 1000L);
        throw new RateLimitExceededException(
            "Too many requests. You have only " + maxRequests + " requests." + " for "
                + basePeriod.toSeconds() + " seconds you could make new request in " + timeout
                + " seconds", timeout
        );
      }
    }
  }
}
