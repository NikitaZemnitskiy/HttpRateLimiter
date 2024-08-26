package com.zemnitskiy.httpratelimiter.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zemnitskiy.httpratelimiter.ratelimiter.fixedwindow.FixedWindowRateLimiterData;
import java.time.Duration;
import java.util.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@code CaffeineConfig} is a configuration class that defines various Caffeine caches
 * used for rate limiting strategies in the application. It utilizes Spring's {@link Configuration}
 * and {@link Bean} annotations to create and manage the cache beans.
 *
 * <p>The class provides three different Caffeine cache configurations:
 * <ul>
 *   <li>{@link #fixedWindowCache()}: Cache for storing data specific to the Fixed Window rate limiter strategy.</li>
 *   <li>{@link #slidingWindowCache()}: Cache for storing timestamps used in the Sliding Window rate limiter strategy.</li>
 *   <li>{@link #slidingWindowRedis()}: Cache for storing data related to the Sliding Window rate limiter with Redis integration.</li>
 * </ul>
 *
 * <p>The expiration policies for the caches are configured based on the {@code basePeriod}
 * value, which is injected from the application's properties.
 */
@Configuration
public class CaffeineConfig {

  /**
   * The base period used for cache expiration. This value is injected from the application's
   * configuration properties.
   */
  @Value("${rateLimiter.basePeriod}")
  private Duration basePeriod;

  /**
   * Defines a cache for the Fixed Window rate limiter strategy.
   *
   * <p>The cache stores {@link FixedWindowRateLimiterData} objects and is configured
   * to expire entries based on the {@code basePeriod} duration, using the "expireAfterWrite"
   * policy.
   *
   * @return a {@link Cache} instance configured for the Fixed Window rate limiter
   */
  @Bean
  public Cache<String, FixedWindowRateLimiterData> fixedWindowCache() {
    return Caffeine.newBuilder()
        .expireAfterWrite(basePeriod)
        .build();
  }

  /**
   * Defines a cache for the Sliding Window rate limiter strategy.
   *
   * <p>The cache stores {@link Queue} of {@link Long} objects representing timestamps and is configured
   * to expire entries based on the {@code basePeriod} duration, using the "expireAfterAccess"
   * policy.
   *
   * @return a {@link Cache} instance configured for the Sliding Window rate limiter
   */
  @Bean
  public Cache<String, Queue<Long>> slidingWindowCache() {
    return Caffeine.newBuilder()
        .expireAfterAccess(basePeriod)
        .build();
  }

  /**
   * Defines a cache for storing data related to the Sliding Window rate limiter with Redis integration.
   *
   * <p>The cache stores {@link Long} values and is configured to expire entries based on the {@code basePeriod}
   * duration, using the "expireAfterAccess" policy.
   *
   * @return a {@link Cache} instance for managing Sliding Window rate limiter data with Redis
   */
  @Bean
  public Cache<String, Long> slidingWindowRedis() {
    return Caffeine.newBuilder()
        .expireAfterAccess(basePeriod)
        .build();
  }
}