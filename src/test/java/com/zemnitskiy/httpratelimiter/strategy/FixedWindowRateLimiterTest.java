package com.zemnitskiy.httpratelimiter.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zemnitskiy.httpratelimiter.utils.Utils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class FixedWindowRateLimiterTest {

  @Value("${baseMaxRequestsPerPeriod}")
  private int maxRequests;

  @Value("${basePeriod}")
  private String periodInSeconds;

  private long period;


  private FixedWindowRateLimiter fixedWindowRateLimiter;

  @BeforeEach
  public void setUp() {
    period = Utils.getBasePeriod(periodInSeconds);
    fixedWindowRateLimiter = new FixedWindowRateLimiter(maxRequests, period);
  }

  @Test
  public void testAllowsRequestWithinLimit() {
    for (int i = 0; i < maxRequests; i++) {
      assertTrue(fixedWindowRateLimiter.allowRequest());
    }
  }

  @Test
  public void testDeniesRequestExceedingLimit() {
    for (int i = 0; i < maxRequests; i++) {
      assertTrue(fixedWindowRateLimiter.allowRequest());
    }
    assertFalse(fixedWindowRateLimiter.allowRequest());
  }

  @Test
  public void testResetsAfterPeriod() throws InterruptedException {
    for (int i = 0; i < maxRequests; i++) {
      assertTrue(fixedWindowRateLimiter.allowRequest());
    }
    assertFalse(fixedWindowRateLimiter.allowRequest());

    TimeUnit.SECONDS.sleep(period / 1000000000L);

    assertTrue(fixedWindowRateLimiter.allowRequest());
  }

  @Test
  public void testThreadSafety() throws InterruptedException, ExecutionException {
    int totalRequests = maxRequests * 2;
    int numberOfThreads = 5;
    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
    List<Future<Boolean>> results = new ArrayList<>();

    for (int i = 0; i < totalRequests; i++) {
      results.add(executor.submit(() -> fixedWindowRateLimiter.allowRequest()));
    }

    executor.shutdown();
    var _ = executor.awaitTermination(1, TimeUnit.MINUTES);

    int allowedRequestsCount = 0;
    for (Future<Boolean> result : results) {
      if (result.get()) {
        allowedRequestsCount++;
      }
    }

    assertEquals(allowedRequestsCount, maxRequests,
        "Allowed requests should not exceed the limit.");
  }
}