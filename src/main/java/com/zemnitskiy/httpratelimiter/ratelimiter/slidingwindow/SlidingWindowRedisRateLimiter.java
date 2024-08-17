package com.zemnitskiy.httpratelimiter.ratelimiter.slidingwindow;

import com.github.benmanes.caffeine.cache.Cache;
import com.zemnitskiy.httpratelimiter.strategy.RateLimitExceededException;
import com.zemnitskiy.httpratelimiter.strategy.RateLimiterStrategy;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Profile("slidingWindowRedisRateLimiter")
@Component
public final class SlidingWindowRedisRateLimiter implements RateLimiterStrategy {

  private final RedisTemplate<String, String> redisTemplate;
  private final RedisScript<Long> rateLimiterScript;

  @Value("${rateLimiter.maxRequestsPerPeriod}")
  private int maxRequests;

  @Value("${rateLimiter.basePeriod}")
  private Duration basePeriod;

  private final Cache<String, Long> cache;

  private final Logger log = LoggerFactory.getLogger(SlidingWindowRedisRateLimiter.class);

  private String exceptionMessage;

  public SlidingWindowRedisRateLimiter(RedisTemplate<String, String> redisTemplate,
      Cache<String, Long> cache) {
    this.redisTemplate = redisTemplate;
    this.cache = cache;
    this.rateLimiterScript = createRateLimiterScript();
  }

  @Override
  public void allowRequest(String clientKey) {

    log.trace("Attempting to allow request for key: {}", clientKey);
    Long cachedRetryTime = cache.getIfPresent(clientKey);
    Long currentTime = System.currentTimeMillis();
    if (cachedRetryTime != null && cachedRetryTime > currentTime) {
      throw new RateLimitExceededException(getExceptionMessage(),
          (int) (cachedRetryTime - currentTime) / 1000);
    }
    Long result = redisTemplate.execute(rateLimiterScript,
        Collections.singletonList(clientKey),
        String.valueOf(maxRequests),
        String.valueOf(basePeriod.toMillis()));

    if (result == null) {
      throw new IllegalStateException(
          "Could not get result from Redis lua script for " + clientKey);
    }

    if (result != 0) {
      log.trace("Rate limit exceeded for client: {}. Retry after {} millis", clientKey, result);
      cache.put(clientKey, System.currentTimeMillis() + result);
      throw new RateLimitExceededException(getExceptionMessage(), result.intValue() / 1000);
    }

    log.trace("Request allowed for client: {}", clientKey);
  }

  private RedisScript<Long> createRateLimiterScript() {
    try (InputStream inputStream = getClass().getClassLoader()
        .getResourceAsStream("rate_limiter.lua")) {
      if (inputStream == null) {
        throw new IllegalArgumentException("Lua script not found in resources");
      }
      String script = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      return RedisScript.of(script, Long.class);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to load Lua script", e);
    }
  }

  private String getExceptionMessage() {
    if (exceptionMessage == null) {
      this.exceptionMessage = String.format(
          "Too many requests. You have only %d requests for %d seconds",
          maxRequests,
          basePeriod.toSeconds()
      );
    }
    return exceptionMessage;
  }

}
