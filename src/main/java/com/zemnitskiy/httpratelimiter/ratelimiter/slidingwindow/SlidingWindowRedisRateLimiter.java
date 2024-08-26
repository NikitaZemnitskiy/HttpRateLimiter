package com.zemnitskiy.httpratelimiter.ratelimiter.slidingwindow;

import com.github.benmanes.caffeine.cache.Cache;
import com.zemnitskiy.httpratelimiter.strategy.RateLimitExceededException;
import com.zemnitskiy.httpratelimiter.strategy.RateLimiterStrategy;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * The {@code SlidingWindowRedisRateLimiter} class implements a rate limiting strategy using the sliding window algorithm
 * and Redis for distributed request management. This allows rate limiting across multiple instances of a service.
 *
 * <p>This class is final and cannot be subclassed.
 */
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

  /**
   * Constructs a new {@code SlidingWindowRedisRateLimiter} with the provided Redis template and Caffeine cache.
   *
   * @param redisTemplate the Redis template used for interacting with Redis
   * @param cache the cache to store the retry times for clients who exceeded the rate limit
   */
  public SlidingWindowRedisRateLimiter(RedisTemplate<String, String> redisTemplate,
      Cache<String, Long> cache) {
    this.redisTemplate = redisTemplate;
    this.cache = cache;
    this.rateLimiterScript = createRateLimiterScript();
  }

  /**
   * Validates the properties of the rate limiter after construction.
   *
   * <p>Ensures that {@code maxRequests} is greater than 0 and that {@code basePeriod} is not null.
   * This method is executed after dependency injection is complete.
   *
   * @throws IllegalArgumentException if {@code maxRequests} is less than or equal to 0 or if {@code basePeriod} is null
   */
  @PostConstruct
  public void validateProperties() {
    if (maxRequests <= 0) {
      throw new IllegalArgumentException("maxRequestsPerPeriod must be greater than 0");
    }
    if (basePeriod == null) {
      throw new IllegalArgumentException("basePeriod must be set");
    }
  }

  /**
   * Attempts to allow a request for the given client key.
   *
   * <p>If the number of requests within the sliding window exceeds the maximum allowed,
   * a {@link RateLimitExceededException} is thrown, indicating that the request cannot be processed until
   * the rate limit resets. The method uses a Lua script in Redis to manage the sliding window algorithm.
   *
   * @param clientKey the unique key representing the client or request source
   * @throws RateLimitExceededException if the rate limit for the client has been exceeded
   */
  @Override
  public void allowRequest(String clientKey) {

    log.trace("Attempting to allow request for key: {}", clientKey);
    Long cachedRetryTime = cache.getIfPresent(clientKey);
    Long currentTime = System.currentTimeMillis();
    if (cachedRetryTime != null && cachedRetryTime > currentTime) {
      throw new RateLimitExceededException(getExceptionMessage(),
          Math.toIntExact(Math.ceilDiv(cachedRetryTime - currentTime, 1000)));
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

  /**
   * Loads the Lua script for rate limiting from the resources directory.
   *
   * <p>The script is used by Redis to implement the sliding window rate limiting logic.
   *
   * @return the {@code RedisScript<Long>} representing the loaded Lua script
   * @throws IllegalArgumentException if the Lua script cannot be found or loaded
   */
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

  /**
   * Constructs an exception message indicating that the rate limit has been exceeded.
   *
   * @return the constructed exception message
   */
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