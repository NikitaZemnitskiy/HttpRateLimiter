package com.zemnitskiy.httpratelimiter.regression;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("slidingWindowRateLimiter")
public class RateSlidingLimiterRegressionTest {

  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;

  @Value("${rateLimiter.basePeriod}")
  private Duration basePeriod;

  @Value("${rateLimiter.maxRequestsPerPeriod}")
  private int baseMaxRequestsPerPeriod;

  private String generateIp(int i) {
    return "192.168.0." + i;
  }

  @Test
  public void testSlidingWindowBehaviorThreadSafety()
      throws InterruptedException, ExecutionException {
    String url = "http://localhost:" + port + "/test";
    long period = basePeriod.toNanos();

    var time = System.currentTimeMillis();

    int concurrentThreads = 100;
    ExecutorService executorService = Executors.newFixedThreadPool(concurrentThreads);
    ExecutorService executorService2 = Executors.newFixedThreadPool(concurrentThreads);
    int sleepTime = (int) ((period / 1000000000L) / baseMaxRequestsPerPeriod);

    List<Future<ResponseEntity<String>>> futures = new ArrayList<>();
    List<Future<ResponseEntity<String>>> futures2 = new ArrayList<>();

    for (int i = 0; i < concurrentThreads * baseMaxRequestsPerPeriod; i++) {
      int finalI = i;
      futures.add(executorService.submit(() -> {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Forwarded-For", generateIp(finalI));
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
      }));
    }

    TimeUnit.SECONDS.sleep(sleepTime);

    for (int i = 0; i < concurrentThreads * baseMaxRequestsPerPeriod; i++) {
      int finalI = i;
      futures2.add(executorService2.submit(() -> {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Forwarded-For", generateIp(finalI));
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
      }));
    }

    for (Future<ResponseEntity<String>> future : futures) {
      ResponseEntity<String> response = future.get();
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
    futures.clear();

    for (Future<ResponseEntity<String>> future : futures2) {
      ResponseEntity<String> response = future.get();
      if (futures.indexOf(future) < concurrentThreads) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      } else {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
      }
    }
    futures2.clear();

    executorService.shutdown();
    executorService2.shutdown();
    var _ = executorService.awaitTermination(1, TimeUnit.MINUTES);
    var _ = executorService2.awaitTermination(1, TimeUnit.MINUTES);

    System.out.println("Завершено за " + (System.currentTimeMillis() - time));
  }

}

