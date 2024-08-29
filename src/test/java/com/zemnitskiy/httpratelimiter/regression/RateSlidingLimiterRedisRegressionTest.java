package com.zemnitskiy.httpratelimiter.regression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.redis.testcontainers.RedisContainer;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class RateSlidingLimiterRedisRegressionTest {

  @Container
  private static final RedisContainer REDIS_CONTAINER =
      new RedisContainer(DockerImageName.parse("redis:5.0.3-alpine")).withExposedPorts(6379);

  @Autowired
  private TestRestTemplate restTemplate;


  @Autowired
  private RedisTemplate<String, String> redisTemplate;

  @LocalServerPort
  private int port;

  private static final Duration BASE_PERIOD = Duration.ofSeconds(10);
  private static final int MAX_REQUEST_PER_PERIOD = 5;

  @DynamicPropertySource
  private static void registerRedisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
    registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379).toString());
    registry.add("rateLimiter.mode", () -> "slidingWindowRedisRateLimiter");
    registry.add("rateLimiter.maxRequestsPerPeriod", () -> MAX_REQUEST_PER_PERIOD);
    registry.add("rateLimiter.basePeriod", () -> BASE_PERIOD);
  }

  @BeforeEach
  public void clearRedis() {
    redisTemplate.getConnectionFactory().getConnection().flushAll();
  }

  @Test
  void givenRedisContainerConfiguredWithDynamicProperties_whenCheckingRunningStatus_thenStatusIsRunning() {
    assertTrue(REDIS_CONTAINER.isRunning());
  }

  @Test
  void givenSingleRequest_whenCheckingRedisEntry_thenEntryIsCreated() {
    String url = "http://localhost:" + port + "/test";
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Forwarded-For", "192.168.0.1");
    HttpEntity<String> entity = new HttpEntity<>(headers);
    restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

    String redisKey = "192.168.0.1";
    Set<String> redisEntries = redisTemplate.opsForZSet().range(redisKey, 0, -1);

    assertNotNull(redisEntries);
    assertFalse(redisEntries.isEmpty(), "Redis entry should exist after the request.");
  }

  @Test
  public void givenMultipleRequests_whenCheckingRedisEntries_thenOnlyMaxRequestAreStored() {
    String url = "http://localhost:" + port + "/test";
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Forwarded-For", "192.168.0.1");
    HttpEntity<String> entity = new HttpEntity<>(headers);

    for (int i = 0; i < MAX_REQUEST_PER_PERIOD * 100; i++) {
      restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }

    String redisKey = "192.168.0.1";
    Long redisEntriesCount = redisTemplate.opsForZSet().zCard(redisKey);

    assertNotNull(redisEntriesCount);
    assertEquals(MAX_REQUEST_PER_PERIOD, redisEntriesCount.intValue(),
        "Redis should store only 5 entries.");
  }

  @Test
  void givenMultipleRequestsAndWaiting_whenAddingOneMore_thenOnlyOneEntryRemainsInRedis()
      throws InterruptedException {
    String url = "http://localhost:" + port + "/test";
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Forwarded-For", "192.168.0.1");
    HttpEntity<String> entity = new HttpEntity<>(headers);

    for (int i = 0; i < MAX_REQUEST_PER_PERIOD; i++) {
      restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }

    TimeUnit.SECONDS.sleep(BASE_PERIOD.toSeconds());

    restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

    String redisKey = "192.168.0.1";
    Long redisEntriesCount = redisTemplate.opsForZSet().zCard(redisKey);

    assertNotNull(redisEntriesCount);
    assertEquals(1, redisEntriesCount.intValue(), "Redis should store only 1 entry after waiting.");
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
                Assertions.fail("To much accepted request");
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
    assertTrue(terminated, "Executor did not terminate in the expected time");

    assertEquals(MAX_REQUEST_PER_PERIOD, successfulRequests.get(),
        "Only up to maxRequests should be allowed in a given sliding window period.");
  }

}