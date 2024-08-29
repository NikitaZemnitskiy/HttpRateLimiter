package com.zemnitskiy.httpratelimiter.regression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
class RateSlidingLimiterRegressionTest {

  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;

  private static final Duration BASE_PERIOD = Duration.ofSeconds(10);
  private static final int MAX_REQUEST_PER_PERIOD = 5;

  @DynamicPropertySource
  private static void registerRedisProperties(DynamicPropertyRegistry registry) {
    registry.add("rateLimiter.mode", () -> "slidingWindowRateLimiter");
    registry.add("rateLimiter.maxRequestsPerPeriod", () -> MAX_REQUEST_PER_PERIOD);
    registry.add("rateLimiter.basePeriod", () -> BASE_PERIOD);
  }

  @Test
  void testSlidingWindowBehaviorThreadSafety()
      throws InterruptedException {
    String url = "http://localhost:" + port + "/test";

    int numberOfThreads = 1000;
    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

    AtomicInteger successfulRequests = new AtomicInteger(0);

    for (int i = 0; i < MAX_REQUEST_PER_PERIOD; i++) {
      HttpHeaders headers = new HttpHeaders();
      headers.set("X-Forwarded-For", "client1");
      HttpEntity<String> entity = new HttpEntity<>(headers);
      restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
      TimeUnit.SECONDS.sleep(BASE_PERIOD.toSeconds() / MAX_REQUEST_PER_PERIOD);
    }

    for (int i = 0; i < numberOfThreads; i++) {
      executor.submit(() -> {
            for (int j = 0; j < MAX_REQUEST_PER_PERIOD; j++) {
              HttpHeaders headers = new HttpHeaders();
              headers.set("X-Forwarded-For", "client1");
              HttpEntity<String> entity = new HttpEntity<>(headers);
              var firstResponse = restTemplate.exchange(url, HttpMethod.GET, entity, String.class)
                  .getStatusCode();
              if (firstResponse.is2xxSuccessful()) {
                successfulRequests.incrementAndGet();
              }
              var secondResponse = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
              if (secondResponse.getStatusCode().is2xxSuccessful()) {
                fail("To much accepted request");
              }
              try {
                TimeUnit.SECONDS.sleep(BASE_PERIOD.toSeconds() / MAX_REQUEST_PER_PERIOD);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            }
          }
      );

    }

    executor.shutdown();

    boolean terminated = executor.awaitTermination(1, TimeUnit.MINUTES);
    assertTrue(terminated, "Executor1 did not terminate in the expected time");

    assertEquals(MAX_REQUEST_PER_PERIOD, successfulRequests.get(),
        "Only up to maxRequests should be allowed in a given sliding window period.");
  }

}

