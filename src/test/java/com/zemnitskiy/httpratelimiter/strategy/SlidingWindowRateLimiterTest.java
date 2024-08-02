package com.zemnitskiy.httpratelimiter.strategy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.concurrent.*;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
public class SlidingWindowRateLimiterTest {

  @Value("${baseMaxRequestsPerPeriod}")
  private int maxRequests;

  @Value("${basePeriod}")
  private long period;

  private SlidingWindowRateLimiter slidingWindowRateLimiter;

  @BeforeEach
  public void setUp() {
    slidingWindowRateLimiter = new SlidingWindowRateLimiter(maxRequests, period);
  }

  @Test
  public void testAllowsRequestWithinLimit() {
    for (int i = 0; i < maxRequests; i++) {
      assertTrue(slidingWindowRateLimiter.allowRequest());
    }
  }

  @Test
  public void testDeniesRequestExceedingLimit() {
    for (int i = 0; i < maxRequests; i++) {
      assertTrue(slidingWindowRateLimiter.allowRequest());
    }
    assertFalse(slidingWindowRateLimiter.allowRequest());
  }

  @Test
  public void testResetsAfterPeriod() throws InterruptedException {
    for (int i = 0; i < maxRequests; i++) {
      assertTrue(slidingWindowRateLimiter.allowRequest());
    }
    assertFalse(slidingWindowRateLimiter.allowRequest());

    TimeUnit.SECONDS.sleep(period / 1000000000L);

    assertTrue(slidingWindowRateLimiter.allowRequest());
  }

  @Test
  public void testThreadSafety() throws InterruptedException, ExecutionException {
    int totalRequests = 20;
    int numberOfThreads = 5;
    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
    List<Future<Boolean>> results = new ArrayList<>();

    for (int i = 0; i < totalRequests; i++) {
      results.add(executor.submit(() -> slidingWindowRateLimiter.allowRequest()));
    }

    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.MINUTES);

    int allowedRequestsCount = 0;
    for (Future<Boolean> result : results) {
      if (result.get()) {
        allowedRequestsCount++;
      }
    }

    assertTrue(allowedRequestsCount <= maxRequests, "Allowed requests should not exceed the limit.");
  }

  @Test
  public void testSlidingWindowBehavior() throws InterruptedException {
    int sleepTime = (int) ((period / 1000000000L)/maxRequests);

    for (int i = 0; i < maxRequests; i++) {
      assertTrue(slidingWindowRateLimiter.allowRequest());
      TimeUnit.SECONDS.sleep(sleepTime);
    }
    for (int i = 0; i < maxRequests; i++) {
      assertTrue(slidingWindowRateLimiter.allowRequest());
      assertFalse(slidingWindowRateLimiter.allowRequest());
      TimeUnit.SECONDS.sleep(sleepTime);
    }
  }

}
