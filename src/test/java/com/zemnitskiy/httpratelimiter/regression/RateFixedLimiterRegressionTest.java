package com.zemnitskiy.httpratelimiter.regression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RateFixedLimiterRegressionTest {

  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;

  private static final Duration BASE_PERIOD = Duration.ofSeconds(10);
  private static final int MAX_REQUEST_PER_PERIOD = 5;
  private static final int NUMBER_OF_THREADS = 1000;

  @DynamicPropertySource
  private static void registerRedisProperties(DynamicPropertyRegistry registry) {
    registry.add("rateLimiter.mode", () -> "fixedWindowRateLimiter");
    registry.add("rateLimiter.maxRequestsPerPeriod", () -> MAX_REQUEST_PER_PERIOD);
    registry.add("rateLimiter.basePeriod", () -> BASE_PERIOD);
  }

  private ExecutorService executor;

  @BeforeEach
  public void setUp() {
    executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
  }

  @AfterEach
  public void tearDown() throws InterruptedException {
    if (executor != null && !executor.isShutdown()) {
      executor.shutdown();
      boolean terminated = executor.awaitTermination(1, TimeUnit.MINUTES);
      assertTrue(terminated, "Executor1 did not terminate in the expected time");
    }
  }

  @Test
  void testFixedWindowBehaviorThreadSafety() throws InterruptedException {
    String url = "http://localhost:" + port + "/test";
    HttpEntity<String> entity = createHttpEntity("client1");

    AtomicInteger successfulRequests = new AtomicInteger(0);
    CountDownLatch latch = new CountDownLatch(NUMBER_OF_THREADS);

    // Run concurrent requests and wait for all threads to finish
    runConcurrentRequests(url, entity, latch, successfulRequests, MAX_REQUEST_PER_PERIOD);

    latch.await();
    // Verify that only up to MAX_REQUEST_PER_PERIOD requests are allowed in a given window period
    assertEquals(MAX_REQUEST_PER_PERIOD, successfulRequests.get(),
        "Only up to maxRequests should be allowed in a given window period.");

    successfulRequests.set(0);
    latch = new CountDownLatch(MAX_REQUEST_PER_PERIOD);

    // Run concurrent requests again to verify that no more requests are allowed after the limit
    runConcurrentRequests(url, entity, latch, successfulRequests, MAX_REQUEST_PER_PERIOD);

    latch.await();
    assertEquals(0, successfulRequests.get(),
        "No requests should be allowed after the limit in a given window period.");

    successfulRequests.set(0);
    latch = new CountDownLatch(MAX_REQUEST_PER_PERIOD);

    TimeUnit.SECONDS.sleep(getRetryAfterHeader(url, entity));

    // Run concurrent requests again to verify that requests are allowed after the sliding window period
    runConcurrentRequests(url, entity, latch, successfulRequests, MAX_REQUEST_PER_PERIOD);

    latch.await();
    assertEquals(MAX_REQUEST_PER_PERIOD, successfulRequests.get(),
        "Only up to maxRequests should be allowed in a given window period.");
  }

  private HttpEntity<String> createHttpEntity(String clientId) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Forwarded-For", clientId);
    return new HttpEntity<>(headers);
  }

  private void runConcurrentRequests(String url, HttpEntity<String> entity, CountDownLatch latch,
      AtomicInteger successfulRequests, int requestsPerThread) {
    for (int i = 0; i < NUMBER_OF_THREADS; i++) {
      executor.submit(() -> {
        try {
          for (int j = 0; j < requestsPerThread; j++) {
            var response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
              successfulRequests.incrementAndGet();
            }
          }
        } finally {
          latch.countDown();
        }
      });
    }
  }

  private long getRetryAfterHeader(String url, HttpEntity<String> entity) {
    var response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    List<String> retryAfter = response.getHeaders().get("Retry-After");
    assertTrue(retryAfter != null && !retryAfter.isEmpty(),
        "Retry-After header should be present.");
    return Long.parseLong(retryAfter.getFirst());
  }
}

