package com.zemnitskiy.httpratelimiter.ratelimiter.fixedwindow;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.testing.FakeTicker;
import com.zemnitskiy.httpratelimiter.strategy.RateLimitExceededException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FixedWindowRateLimiterTest {

  private FixedWindowRateLimiter rateLimiter;
  private FakeTicker ticker;

  private final Duration basePeriod = Duration.ofSeconds(10);
  private final int maxRequests = 800;

  private Cache<String, FixedWindowRateLimiterData> cache;

  @BeforeEach
  public void setUp() {
    rateLimiter = new FixedWindowRateLimiter(maxRequests, basePeriod);
    ticker = new FakeTicker();
    cache = Caffeine.newBuilder()
        .expireAfterWrite(basePeriod)
        .ticker(ticker::read)
        .build();
    setField(rateLimiter, "cache", cache);
  }

  @Test
  void testAllowRequest_WithinLimit_ShouldBeAllowed() {
    for (int i = 0; i < maxRequests; i++) {
      rateLimiter.allowRequest("client1");
    }
    AtomicInteger count = Objects.requireNonNull(cache.getIfPresent("client1")).counter();
    assertEquals(maxRequests, count.get(), "Requests within the limit should be allowed.");
  }

  @Test
  void testAllowRequest_ExceedingLimit_ShouldThrowException() {
    for (int i = 0; i < maxRequests; i++) {
      rateLimiter.allowRequest("client1");
    }
    assertThrows(RateLimitExceededException.class, () -> rateLimiter.allowRequest("client1"),
        "A request exceeding the limit should throw a RateLimitExceededException.");
  }

  @Test
  void testCacheClearedAfterBasePeriod() {
    for (int i = 0; i < maxRequests; i++) {
      rateLimiter.allowRequest("client1");
    }

    AtomicInteger count = Objects.requireNonNull(cache.getIfPresent("client1")).counter();
    assertEquals(maxRequests, count.get(), "The request count should be equal to maxRequests.");
    ticker.advance(basePeriod);

    FixedWindowRateLimiterData fixedWindowRateLimiterData = cache.getIfPresent("client1");
    assertNull(fixedWindowRateLimiterData, "Cache should be cleared after the base period.");
  }

  @Test
  void testAllowRequest_MultipleKeys_ShouldRespectLimitsIndependently() {
    String[] clientKeys = {"client1", "client2", "client3"};

    for (String key : clientKeys) {
      for (int i = 0; i < maxRequests; i++) {
        rateLimiter.allowRequest(key);
      }
      AtomicInteger count = Objects.requireNonNull(cache.getIfPresent("client1")).counter();
      assertEquals(maxRequests, count.get(),
          "Requests within the limit for " + key + " should be allowed.");
    }

    for (String key : clientKeys) {
      assertThrows(RateLimitExceededException.class, () -> rateLimiter.allowRequest(key),
          "A request exceeding the limit for " + key
              + " should throw a RateLimitExceededException.");
    }
  }

  @Test
  void testAllowRequest_MultipleThreads_LimitedUniqueKeys() throws InterruptedException {
    int numberOfThreads = 1000;
    int uniqueClientKeys = 10;
    ExecutorService executor1 = Executors.newFixedThreadPool(numberOfThreads);
    ExecutorService executor2 = Executors.newFixedThreadPool(numberOfThreads);

    // Array to track successful requests for each unique client key
    AtomicInteger[] successCounters = new AtomicInteger[uniqueClientKeys];
    for (int i = 0; i < uniqueClientKeys; i++) {
      successCounters[i] = new AtomicInteger(0);
    }

    // Submit the first set of tasks
    for (int i = 0; i < numberOfThreads; i++) {
      final String clientKey = "client" + (i % uniqueClientKeys);
      final int keyIndex = i % uniqueClientKeys;
      executor1.submit(() -> {
        for (int j = 0; j < maxRequests * 2; j++) {
          try {
            rateLimiter.allowRequest(clientKey);
            successCounters[keyIndex].incrementAndGet(); // Increment successful request count
          } catch (RateLimitExceededException ignored) {
            // Expect some requests to exceed the limit
          }
        }
      });
    }

    executor1.shutdown();
    boolean terminated1 = executor1.awaitTermination(1, TimeUnit.MINUTES);
    assertTrue(terminated1, "Executor1 did not terminate in the expected time");

    // Check that only maxRequests were allowed per key
    for (int i = 0; i < uniqueClientKeys; i++) {
      assertEquals(maxRequests, successCounters[i].get(),
          "Requests within the limit should be allowed for client" + i);
    }

    // Fake Sleep for the duration of basePeriod
    ticker.advance(basePeriod);

    // Reset counters for another round of testing
    for (int i = 0; i < uniqueClientKeys; i++) {
      successCounters[i].set(0);
    }

    // Submit the second set of tasks after the sleep period
    for (int i = 0; i < numberOfThreads; i++) {
      final String clientKey = "client" + (i % uniqueClientKeys);
      final int keyIndex = i % uniqueClientKeys;
      executor2.submit(() -> {
        for (int j = 0; j < maxRequests * 2; j++) {
          try {
            rateLimiter.allowRequest(clientKey);
            successCounters[keyIndex].incrementAndGet(); // Increment successful request count
          } catch (RateLimitExceededException ignored) {
            // Expect some requests to exceed the limit
          }
        }
      });
    }

    executor2.shutdown();
    boolean terminated2 = executor2.awaitTermination(1, TimeUnit.MINUTES);
    assertTrue(terminated2, "Executor2 did not terminate in the expected time");

    // Check that only maxRequests were allowed per key after the sleep period
    for (int i = 0; i < uniqueClientKeys; i++) {
      assertEquals(maxRequests, successCounters[i].get(),
          "Requests within the limit should be allowed after sleep for client" + i);
    }
  }

}