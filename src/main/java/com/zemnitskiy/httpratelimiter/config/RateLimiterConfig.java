package com.zemnitskiy.httpratelimiter.config;


import com.github.benmanes.caffeine.cache.Cache;
import com.zemnitskiy.httpratelimiter.ratelimiter.fixedwindow.FixedWindowRateLimiter;
import com.zemnitskiy.httpratelimiter.ratelimiter.fixedwindow.FixedWindowRateLimiterData;
import com.zemnitskiy.httpratelimiter.ratelimiter.slidingwindow.SlidingWindowRateLimiter;
import com.zemnitskiy.httpratelimiter.ratelimiter.slidingwindow.SlidingWindowRedisRateLimiter;
import com.zemnitskiy.httpratelimiter.strategy.RateLimiterStrategy;
import java.util.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Configuration class for setting up rate limiter strategies.
 */
@Configuration
@DependsOn("caffeineConfig")
public class RateLimiterConfig {

  /**
   * Configures the FixedWindowRateLimiter strategy.
   *
   * @param cache the cache for tracking request counts
   * @return the configured FixedWindowRateLimiter instance
   */
  @Bean
  @ConditionalOnProperty(name = "rateLimiter.mode", havingValue = "fixedWindowRateLimiter")
  public RateLimiterStrategy fixedWindowRateLimiter(
      Cache<String, FixedWindowRateLimiterData> cache) {
    return new FixedWindowRateLimiter(cache);
  }

  /**
   * Configures the SlidingWindowRedisRateLimiter strategy.
   *
   * @param redisTemplate the Redis template for interacting with Redis
   * @param cache         the cache for tracking request timestamps
   * @return the configured SlidingWindowRedisRateLimiter instance
   */
  @Bean
  @ConditionalOnProperty(name = "rateLimiter.mode", havingValue = "slidingWindowRedisRateLimiter")
  public RateLimiterStrategy slidingWindowRedisRateLimiter(
      RedisTemplate<String, String> redisTemplate, Cache<String, Long> cache) {
    return new SlidingWindowRedisRateLimiter(redisTemplate, cache);
  }

  /**
   * Configures the SlidingWindowRateLimiter strategy.
   *
   * @param cache the cache for tracking request timestamps
   * @return the configured SlidingWindowRateLimiter instance
   */
  @Bean
  @ConditionalOnProperty(name = "rateLimiter.mode", havingValue = "slidingWindowRateLimiter")
  public RateLimiterStrategy slidingWindowRateLimiter(Cache<String, Queue<Long>> cache) {
    return new SlidingWindowRateLimiter(cache);
  }
}
