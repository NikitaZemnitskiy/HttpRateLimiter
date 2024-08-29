package com.zemnitskiy.httpratelimiter.ratelimiter.slidingwindow;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zemnitskiy.httpratelimiter.strategy.RateLimitExceededException;
import com.zemnitskiy.httpratelimiter.strategy.RateLimiterStrategy;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * The {@code SlidingWindowRedisRateLimiter} class implements a rate limiting strategy using the
 * sliding window algorithm and Redis for distributed request management. This allows rate limiting
 * across multiple instances of a service.
 *
 */
public final class SlidingWindowRedisRateLimiter implements RateLimiterStrategy {

  private final RedisTemplate<String, String> redisTemplate;
  private final RedisScript<Long> rateLimiterScript;

  private final int maxRequests;

  private final Duration basePeriod;

  private final Cache<String, Long> cache;

  private final Logger log = LoggerFactory.getLogger(SlidingWindowRedisRateLimiter.class);

  private String exceptionMessage;

  public SlidingWindowRedisRateLimiter(RedisTemplate<String, String> redisTemplate,
      int maxRequests, Duration basePeriod, Resource luaScriptResource) {
    if (maxRequests <= 0) {
      throw new IllegalArgumentException("maxRequestsPerPeriod must be greater than 0");
    }
    if (basePeriod == null) {
      throw new IllegalArgumentException("basePeriod must be set");
    }
    this.cache = Caffeine.newBuilder()
        .expireAfterWrite(basePeriod)
        .build();
    this.maxRequests = maxRequests;
    this.basePeriod = basePeriod;
    this.redisTemplate = redisTemplate;
    try (InputStream inputStream = luaScriptResource.getInputStream()) {
      this.rateLimiterScript = RedisScript.of(
          new String(inputStream.readAllBytes(), StandardCharsets.UTF_8), Long.class);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to load Lua script", e);
    }
  }

  /**
   * Attempts to allow a request for the given client key.
   *
   * <p>If the number of requests within the sliding window for the client exceeds the maximum
   * allowed, a {@link RateLimitExceededException} is thrown, indicating that the request cannot be
   * processed until the rate limit resets.
   *
   * <p>The sliding window algorithm maintains a moving window of requests that considers the
   * most recent requests within the defined time period. If the number of requests within this
   * window exceeds the maximum allowed, the request is denied and the client must wait until
   * the oldest request in the window falls outside the time period before making additional requests.
   *
   * <p>This method uses Redis to track and manage request counts, ensuring that the rate limiting
   * is enforced across distributed systems.
   *
   * <p><b>Thread Safety:</b> This method is designed to be thread-safe. Redis handles concurrent
   * access to the rate limiting data, ensuring that requests are properly managed even in a
   * multiThreaded environment. This prevents race conditions and ensures consistent enforcement
   * of rate limits across multiple threads or instances of the application.
   *
   * @param clientKey the unique key representing the client or request source
   * @throws RateLimitExceededException if the rate limit for the client has been exceeded
   * @see <a href="https://redis.io/learn/develop/dotnet/aspnetcore/rate-limiting/sliding-window">Rate Limiting with Redis</a>
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