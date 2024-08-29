package com.zemnitskiy.httpratelimiter.ratelimiter.slidingwindow;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zemnitskiy.httpratelimiter.strategy.RateLimitExceededException;
import com.zemnitskiy.httpratelimiter.strategy.RateLimiterStrategy;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code SlidingWindowRateLimiter} class implements a rate limiting strategy using the sliding
 * window algorithm. It allows a maximum number of requests within a rolling time period, ensuring a
 * more dynamic control over the request rate.
 *
 * <p>This class uses Caffeine cache to store the request timestamps per client key, maintaining
 * the state of the sliding window.`
 */
public final class SlidingWindowRateLimiter implements RateLimiterStrategy {

  private final int maxRequests;

  private final Duration basePeriod;

  private final Cache<String, Queue<Long>> cache;

  private final Logger log = LoggerFactory.getLogger(SlidingWindowRateLimiter.class);

  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
      .withZone(ZoneId.systemDefault());


  public SlidingWindowRateLimiter(int maxRequests, Duration basePeriod) {
    if (maxRequests <= 0) {
      throw new IllegalArgumentException("maxRequestsPerPeriod must be greater than 0");
    }
    if (basePeriod == null) {
      throw new IllegalArgumentException("basePeriod must be set");
    }
    this.cache = Caffeine.newBuilder()
        .expireAfterAccess(basePeriod)
        .build();
    this.maxRequests = maxRequests;
    this.basePeriod = basePeriod;
  }


  /**
   * Attempts to allow a request for the given key based on a sliding window rate limiting strategy.
   *
   * <p>The sliding window rate limiting strategy works by maintaining a queue of timestamps representing
   * the times of past requests. It calculates the current window based on the configured time period and
   * allows or denies new requests based on the number of requests within this window. The window "slides"
   * as time progresses, meaning that outdated timestamps are removed from the queue to reflect the new
   * window period.
   *
   * <p>If the number of requests within the sliding window for the key is below the maximum allowed,
   * the request is allowed, and the current timestamp is recorded. If the limit has been reached, a
   * {@link RateLimitExceededException} is thrown, indicating that the request cannot be processed until
   * an earlier timestamp falls out of the sliding window.
   *
   *
   * @param key the unique key representing the client or request source
   * @throws RateLimitExceededException if the rate limit for the key has been exceeded. The exception message
   * includes the maximum number of allowed requests, the time period for the limit, and the time remaining
   * until requests can be made again.
   */
  @Override
  public void allowRequest(String key) {
    log.trace("Attempting to allow request for key: {}", key);

    Queue<Long> timestamps = cache.get(key, _ -> new LinkedList<>());
    synchronized (timestamps) {
      long now = System.currentTimeMillis();
      long oldestAllowedRequestTime = now - basePeriod.toMillis();
      if (log.isTraceEnabled()) {
        log.trace("Current time: {}, Oldest allowed request time: {}",
            formatter.format(Instant.ofEpochMilli(now)),
            formatter.format(Instant.ofEpochMilli(oldestAllowedRequestTime)));
      }

      // Remove outdated timestamps
      while (!timestamps.isEmpty() && oldestAllowedRequestTime > timestamps.peek()) {
        long removedTimestamp = timestamps.poll();
        if (log.isTraceEnabled()) {
          log.trace("Removed outdated timestamp: {}",
              formatter.format(Instant.ofEpochMilli(removedTimestamp)));
        }
      }

      if (timestamps.size() < maxRequests) {
        timestamps.offer(now);
        log.trace("Request allowed for key: {}. Current queue size: {}", key, timestamps.size());
        return;
      }

      @SuppressWarnings("DataFlowIssue")
      long waitTime = timestamps.peek() - oldestAllowedRequestTime;
      int retryAfterSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(waitTime);
      log.trace("Too many requests for key: {}. Retry after: {} seconds", key, retryAfterSeconds);

      throw new RateLimitExceededException(
          "Too many requests. You have only " + maxRequests + " requests for "
              + basePeriod.toSeconds() + " seconds.",
          retryAfterSeconds);
    }
  }
}

