package com.zemnitskiy.httpratelimiter.ratelimiter.fixedwindow;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zemnitskiy.httpratelimiter.strategy.RateLimitExceededException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class FixedWindowRateLimiterTest {

  private FixedWindowRateLimiter rateLimiter;

  private Cache<String, FixedWindowRateLimiterData> cache;

  private final int basePeriod = 10;
  private final int maxRequests = 800;

  @BeforeEach
  public void setUp() {
    cache = Caffeine.newBuilder().expireAfterWrite(basePeriod, TimeUnit.SECONDS).build();
    rateLimiter = new FixedWindowRateLimiter(cache);
    ReflectionTestUtils.setField(rateLimiter, "maxRequests", maxRequests);
    ReflectionTestUtils.setField(rateLimiter, "basePeriod", Duration.ofSeconds(basePeriod));

  }

  @Test
  public void testAllowRequest_WithinLimit_ShouldBeAllowed() {
    for (int i = 0; i < maxRequests; i++) {
      rateLimiter.allowRequest("client1");
    }
    AtomicInteger count = Objects.requireNonNull(cache.getIfPresent("client1")).counter();
    assertEquals(maxRequests, count.get(), "Requests within the limit should be allowed.");
  }

  @Test
  public void testAllowRequest_ExceedingLimit_ShouldThrowException() {
    for (int i = 0; i < maxRequests; i++) {
      rateLimiter.allowRequest("client1");
    }
    assertThrows(RateLimitExceededException.class, () -> rateLimiter.allowRequest("client1"),
        "A request exceeding the limit should throw a RateLimitExceededException.");
  }

  @Test
  public void testCacheClearedAfterBasePeriod() throws InterruptedException {
    for (int i = 0; i < maxRequests; i++) {
      rateLimiter.allowRequest("client1");
    }

    AtomicInteger count = Objects.requireNonNull(cache.getIfPresent("client1")).counter();
    assertEquals(maxRequests, count.get(), "The request count should be equal to maxRequests.");

    TimeUnit.SECONDS.sleep(basePeriod);

    FixedWindowRateLimiterData fixedWindowRateLimiterData = cache.getIfPresent("client1");
    assertNull(fixedWindowRateLimiterData, "Cache should be cleared after the base period.");
  }

  @Test
  public void testAllowRequest_MultipleKeys_ShouldRespectLimitsIndependently() {
    String[] clientKeys = {"client1", "client2", "client3"};

    for (String key : clientKeys) {
      for (int i = 0; i < maxRequests; i++) {
        rateLimiter.allowRequest(key);
      }
      AtomicInteger count = Objects.requireNonNull(cache.getIfPresent("client1")).counter();
      assertEquals(maxRequests, count.get(), "Requests within the limit for " + key + " should be allowed.");
    }

    for (String key : clientKeys) {
      assertThrows(RateLimitExceededException.class, () -> rateLimiter.allowRequest(key),
          "A request exceeding the limit for " + key + " should throw a RateLimitExceededException.");
    }
  }


  @Test
  public void testAllowRequest_AfterBasePeriod_ShouldAllowAgain() throws InterruptedException {
    int numberOfThreads = 1000;
    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
    ExecutorService newExecutor = Executors.newFixedThreadPool(numberOfThreads);

    AtomicInteger successfulRequests = new AtomicInteger(0);
    AtomicInteger newSuccessfulRequests = new AtomicInteger(0);

    for (int i = 0; i < numberOfThreads; i++) {
      executor.submit(() -> {
        try {
          rateLimiter.allowRequest("client1");
          successfulRequests.incrementAndGet();
        } catch (RateLimitExceededException ignored) {
          // We expect exceptions for requests beyond the limit
        }
      });
    }

    TimeUnit.SECONDS.sleep(basePeriod);

    for (int i = 0; i < numberOfThreads; i++) {
      newExecutor.submit(() -> {
        try {
          rateLimiter.allowRequest("client1");
          newSuccessfulRequests.incrementAndGet();
        } catch (RateLimitExceededException ignored) {
          // We expect exceptions for requests beyond the limit
        }
      });
    }
    executor.shutdown();
    newExecutor.shutdown();
    boolean _ = executor.awaitTermination(1, TimeUnit.SECONDS);
    boolean _ = newExecutor.awaitTermination(1, TimeUnit.SECONDS);
    assertEquals(maxRequests, successfulRequests.get(),
        "Only 5 requests should have been allowed after the base period.");
    assertEquals(maxRequests, newSuccessfulRequests.get(),
        "Only 5 requests should have been allowed after the base period.");

  }

}