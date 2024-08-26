package com.zemnitskiy.httpratelimiter.ratelimiter.slidingwindow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zemnitskiy.httpratelimiter.strategy.RateLimitExceededException;
import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;


public class SlidingWindowRateLimiterTest {

  @InjectMocks
  private SlidingWindowRateLimiter rateLimiter;

  private Cache<String, Queue<Long>> cache;

  private final Duration basePeriod = Duration.ofSeconds(10);
  private final int maxRequests = 5;

  @BeforeEach
  public void setUp() {
    cache = Caffeine.newBuilder().expireAfterAccess(basePeriod).build();
    rateLimiter = new SlidingWindowRateLimiter(cache);

    ReflectionTestUtils.setField(rateLimiter, "maxRequests", maxRequests);
    ReflectionTestUtils.setField(rateLimiter, "basePeriod", basePeriod);

    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testAllowRequest_WithinLimit_ShouldBeAllowed() {
    for (int i = 0; i < maxRequests; i++) {
      rateLimiter.allowRequest("client1");
    }
    Queue<Long> timestamps = cache.getIfPresent("client1");
    assertNotNull(timestamps, "Cache should contain timestamps for client1.");
    assertEquals(maxRequests, timestamps.size(), "Requests within the limit should be allowed.");
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
  public void testSlidingWindowBehavior() throws InterruptedException {
    for (int i = 0; i < maxRequests; i++) {
      rateLimiter.allowRequest("client1");
      TimeUnit.SECONDS.sleep(basePeriod.toSeconds() / maxRequests);
    }

    for (int i = 0; i < maxRequests; i++) {
      rateLimiter.allowRequest("client1");
      assertThrows(RateLimitExceededException.class, () -> rateLimiter.allowRequest("client1"),
          "A request exceeding the limit should throw a RateLimitExceededException.");
      TimeUnit.SECONDS.sleep(basePeriod.toSeconds() / maxRequests);
    }

    Queue<Long> timestamps = cache.getIfPresent("client1");
    assertNotNull(timestamps, "Cache should contain the count for client1.");
    assertEquals(maxRequests, timestamps.size(),
        "The sliding window should allow requests after the period.");
  }

  @Test
  public void testSlidingWindowWithMultipleKeys() throws InterruptedException {
    for (int i = 0; i < maxRequests; i++) {
      rateLimiter.allowRequest("client1");
      rateLimiter.allowRequest("client2");
      TimeUnit.SECONDS.sleep(basePeriod.toSeconds() / maxRequests);
    }

    for (int i = 0; i < maxRequests; i++) {
      rateLimiter.allowRequest("client1");
      rateLimiter.allowRequest("client2");
      assertThrows(RateLimitExceededException.class, () -> rateLimiter.allowRequest("client1"),
          "A request exceeding the limit for client1 should throw a RateLimitExceededException.");
      assertThrows(RateLimitExceededException.class, () -> rateLimiter.allowRequest("client2"),
          "A request exceeding the limit for client2 should throw a RateLimitExceededException.");

      TimeUnit.SECONDS.sleep(basePeriod.toSeconds() / maxRequests);
    }

    Queue<Long> timestampsClient1 = cache.getIfPresent("client1");
    Queue<Long> timestampsClient2 = cache.getIfPresent("client2");

    assertNotNull(timestampsClient1, "Cache should contain timestamps for client1.");
    assertNotNull(timestampsClient2, "Cache should contain timestamps for client2.");
    assertEquals(maxRequests, timestampsClient1.size(),
        "The sliding window should allow requests for client1 after the period.");
    assertEquals(maxRequests, timestampsClient2.size(),
        "The sliding window should allow requests for client2 after the period.");
  }

  @Test
  public void testCacheExpiration() throws InterruptedException {
    for (int i = 0; i < maxRequests; i++) {
      rateLimiter.allowRequest("client1");
      TimeUnit.SECONDS.sleep(basePeriod.toSeconds() / maxRequests);
    }

    TimeUnit.SECONDS.sleep(basePeriod.toSeconds() + 1);

    Queue<Long> timestamps = cache.getIfPresent("client1");
    assertNull(timestamps, "Cache should be empty after expiration.");
  }

  @Test
  public void testSlidingWindowRateLimiter_MultiThreaded() throws InterruptedException {
    int numberOfThreads = 1000;
    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

    AtomicInteger successfulRequests = new AtomicInteger(0);

    for (int i = 0; i < maxRequests; i++) {
      rateLimiter.allowRequest("client1");
      TimeUnit.SECONDS.sleep(basePeriod.toSeconds() / maxRequests);
    }

    for (int i = 0; i < numberOfThreads; i++) {
      executor.submit(() -> {
            for (int j = 0; j < maxRequests; j++) {
              try {
                rateLimiter.allowRequest("client1");
                successfulRequests.incrementAndGet();
                rateLimiter.allowRequest("client1");
                successfulRequests.incrementAndGet();
              } catch (RateLimitExceededException e) {
                // Expected exception for requests beyond the limit
              } finally {
                try {
                  TimeUnit.SECONDS.sleep(basePeriod.toSeconds() / maxRequests);
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
              }
            }
          }
      );

    }

    executor.shutdown();
    var _ = executor.awaitTermination(1, TimeUnit.MINUTES);

    assertEquals(maxRequests, successfulRequests.get(),
        "Only up to maxRequests should be allowed in a given sliding window period.");
  }
}