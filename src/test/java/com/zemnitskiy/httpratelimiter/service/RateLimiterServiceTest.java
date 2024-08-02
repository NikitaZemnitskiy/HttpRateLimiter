package com.zemnitskiy.httpratelimiter.service;

import com.zemnitskiy.httpratelimiter.strategy.FixedWindowRateLimiter;
import com.zemnitskiy.httpratelimiter.strategy.RateLimiterStrategyFactory;
import com.zemnitskiy.httpratelimiter.strategy.SlidingWindowRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class RateLimiterServiceTest {

  @Mock
  private RateLimiterStrategyFactory strategyFactory;

  private RateLimiterService rateLimiterServiceToTest;

  @Value("${baseMaxRequestsPerPeriod}")
  private int maxRequests;

  @Value("${basePeriod}")
  private long period;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    rateLimiterServiceToTest = new RateLimiterService(strategyFactory);
  }

  @Test
  public void testFixedWindowRateLimiter_ThreadSafety()
      throws InterruptedException, ExecutionException {
    when(strategyFactory.createStrategy()).thenReturn(createFixedWindowRateLimiter());

    String clientKey = "client4";
    int totalRequests = 20;
    int numberOfThreads = 5;
    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

    List<Future<Boolean>> results = new ArrayList<>();

    for (int i = 0; i < totalRequests; i++) {
      results.add(executor.submit(() -> rateLimiterServiceToTest.isAllowed(clientKey)));
    }

    executor.shutdown();
    var _ = executor.awaitTermination(1, TimeUnit.MINUTES);

    int allowedRequestsCount = 0;
    for (Future<Boolean> result : results) {
      if (result.get()) {
        allowedRequestsCount++;
      }
    }

    assertTrue(allowedRequestsCount <= maxRequests,
        "Allowed requests should not exceed the limit.");
  }

  @Test
  public void testSlidingWindowRateLimiter_ThreadSafety()
      throws InterruptedException, ExecutionException {
    when(strategyFactory.createStrategy()).thenReturn(createSlidingWindowRateLimiter());

    String clientKey = "client4";
    int totalRequests = 20;
    int numberOfThreads = 5;
    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
    List<Future<Boolean>> results = new ArrayList<>();

    for (int i = 0; i < totalRequests; i++) {
      results.add(executor.submit(() -> rateLimiterServiceToTest.isAllowed(clientKey)));
    }

    executor.shutdown();
    var _ = executor.awaitTermination(1, TimeUnit.MINUTES);

    int allowedRequestsCount = 0;
    for (Future<Boolean> result : results) {
      if (result.get()) {
        allowedRequestsCount++;
      }
    }

    assertTrue(allowedRequestsCount <= maxRequests,
        "Allowed requests should not exceed the limit.");
  }

  private FixedWindowRateLimiter createFixedWindowRateLimiter() {
    return new FixedWindowRateLimiter(maxRequests, period);
  }

  private SlidingWindowRateLimiter createSlidingWindowRateLimiter() {
    return new SlidingWindowRateLimiter(maxRequests, period);
  }
}