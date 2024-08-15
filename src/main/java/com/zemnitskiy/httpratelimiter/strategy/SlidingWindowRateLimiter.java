package com.zemnitskiy.httpratelimiter.strategy;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zemnitskiy.httpratelimiter.exception.RateLimitExceededException;
import jakarta.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;


@Profile("slidingWindowRateLimiter")
@Component
public final class SlidingWindowRateLimiter implements RateLimiterStrategy {

  @Value("${baseMaxRequestsPerPeriod}")
  private int maxRequests;

  @Value("${basePeriod}")
  private long basePeriod;

  private Cache<String, Queue<Long>> cache;

  @PostConstruct
  public void init() {
    this.cache = Caffeine.newBuilder()
        .expireAfterWrite(basePeriod, TimeUnit.NANOSECONDS)
        .build();
  }

  @Override
  public synchronized void allowRequestOrThrowException(String key) {
    long now = System.nanoTime();
    Queue<Long> timestamps = cache.get(key, _ -> new LinkedList<>());

    while (!timestamps.isEmpty() && now - timestamps.peek() > basePeriod) {
      timestamps.poll();
    }

    if (timestamps.size() < maxRequests) {
      timestamps.offer(now);
      cache.put(key, timestamps);
      return;
    }
    throw new RateLimitExceededException("Too many requests. You have only " + maxRequests + " requests." + " for " + basePeriod/1000000000 + " seconds");
  }

}

