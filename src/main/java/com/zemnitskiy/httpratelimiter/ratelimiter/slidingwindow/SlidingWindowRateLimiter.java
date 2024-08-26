package com.zemnitskiy.httpratelimiter.ratelimiter.slidingwindow;

import com.github.benmanes.caffeine.cache.Cache;
import com.zemnitskiy.httpratelimiter.strategy.RateLimitExceededException;
import com.zemnitskiy.httpratelimiter.strategy.RateLimiterStrategy;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

public final class SlidingWindowRateLimiter implements RateLimiterStrategy {

  @Value("${rateLimiter.maxRequestsPerPeriod}")
  private int maxRequests;

  @Value("${rateLimiter.basePeriod}")
  private Duration basePeriod;

  private final Cache<String, Queue<Long>> cache;

  private final Logger log = LoggerFactory.getLogger(SlidingWindowRateLimiter.class);

  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
      .withZone(ZoneId.systemDefault());

  public SlidingWindowRateLimiter(Cache<String, Queue<Long>> cache) {
    this.cache = cache;
  }

  @PostConstruct
  public void validateProperties() {
    if (maxRequests <= 0) {
      throw new IllegalArgumentException("maxRequestsPerPeriod must be greater than 0");
    }
    if (basePeriod == null) {
      throw new IllegalArgumentException("basePeriod must be set");
    }
  }

  @Override
  public void allowRequest(String key) {
    log.trace("Attempting to allow request for key: {}", key);

    Queue<Long> timestamps = cache.get(key, _ -> new LinkedList<>());
    synchronized (timestamps) {
      long now = System.currentTimeMillis();
      long oldestAllowedRequestTime = now - basePeriod.toMillis();
      log.trace("Current time: {}, Oldest allowed request time: {}",
          formatter.format(Instant.ofEpochMilli(now)),
          formatter.format(Instant.ofEpochMilli(oldestAllowedRequestTime)));

      // Remove outdated timestamps
      while (!timestamps.isEmpty() && oldestAllowedRequestTime > timestamps.peek()) {
        long removedTimestamp = timestamps.poll();
        log.trace("Removed outdated timestamp: {}",
            formatter.format(Instant.ofEpochMilli(removedTimestamp)));
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

