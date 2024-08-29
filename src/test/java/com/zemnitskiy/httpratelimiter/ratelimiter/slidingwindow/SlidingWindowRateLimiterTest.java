package com.zemnitskiy.httpratelimiter.ratelimiter.slidingwindow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.testing.FakeTicker;
import com.zemnitskiy.httpratelimiter.strategy.RateLimitExceededException;
import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

class SlidingWindowRateLimiterTest {

  @InjectMocks
  private SlidingWindowRateLimiter rateLimiter;

  private FakeTicker ticker;

  private Cache<String, Queue<Long>> cache;

  private final Duration basePeriod = Duration.ofSeconds(10);
  private final int maxRequests = 5;
  private final Duration sleepDuration = Duration.ofSeconds(basePeriod.toSeconds() / maxRequests);

  @BeforeEach
  public void setUp() {
    cache = Caffeine.newBuilder().expireAfterAccess(basePeriod).build();
    rateLimiter = new SlidingWindowRateLimiter(maxRequests, basePeriod);
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
    Queue<Long> timestamps = cache.getIfPresent("client1");
    assertNotNull(timestamps, "Cache should contain timestamps for client1.");
    assertEquals(maxRequests, timestamps.size(), "Requests within the limit should be allowed.");
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
  void testCacheExpiration() {
    for (int i = 0; i < maxRequests; i++) {
      rateLimiter.allowRequest("client1");
      ticker.advance(sleepDuration);
    }

    ticker.advance(sleepDuration);

    Queue<Long> timestamps = cache.getIfPresent("client1");
    assertNull(timestamps, "Cache should be empty after expiration.");
  }

  @Test
  void testSlidingWindowRateLimiter_MultiThreaded() throws InterruptedException {
    int uniqueClientKeys = 100;
    int numberOfThreads = 100;

    AtomicInteger[] successCounters = new AtomicInteger[uniqueClientKeys];
    for (int i = 0; i < uniqueClientKeys; i++) {
      successCounters[i] = new AtomicInteger(0);
    }

    for (int i = 0; i < maxRequests; i++) {
      for (int j = 0; j < uniqueClientKeys; j++) {
        final String clientKey = "client" + j;
        try {
          rateLimiter.allowRequest(clientKey);
          successCounters[j].incrementAndGet();
        } catch (RateLimitExceededException ignored) {
        }
      }

      ticker.advance(sleepDuration);
    }

    for (int i = 0; i < uniqueClientKeys; i++) {
      assertEquals(maxRequests, successCounters[i].get(),
          "Requests within the limit should be allowed for client" + i);
    }

    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
    for (int i = 0; i < uniqueClientKeys; i++) {
      successCounters[i] = new AtomicInteger(0);
    }

    CountDownLatch latch = new CountDownLatch(numberOfThreads * uniqueClientKeys);
    AtomicInteger testCounter = new AtomicInteger(0);

    for (int i = 0; i < maxRequests; i++) {
      for (int j = 0; j < uniqueClientKeys; j++) {
        final String clientKey = "client" + j;
        int finalJ = j;
        for (int k = 0; k < numberOfThreads; k++) {
          CountDownLatch finalLatch = latch;
          executor.submit(() -> {
            try {
              rateLimiter.allowRequest(clientKey);
              successCounters[finalJ].incrementAndGet();
            } catch (RateLimitExceededException _) {
              //Expected error
            } finally {
              finalLatch.countDown();
            }
          });
        }
      }
      assertTrue(latch.await(1, TimeUnit.MINUTES));
      latch = new CountDownLatch(numberOfThreads * uniqueClientKeys);
      ticker.advance(sleepDuration);
    }
    executor.shutdown();
    boolean terminated = executor.awaitTermination(1, TimeUnit.MINUTES);
    assertTrue(terminated, "Executor did not terminate in the expected time");

    for (int i = 0; i < uniqueClientKeys; i++) {
      assertEquals(maxRequests, successCounters[i].get(),
          "Requests within the limit should be allowed for client" + i);
    }


  }
}
