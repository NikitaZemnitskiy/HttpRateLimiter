package com.zemnitskiy.httpratelimiter.config;


import com.zemnitskiy.httpratelimiter.ratelimiter.fixedwindow.FixedWindowRateLimiter;
import com.zemnitskiy.httpratelimiter.ratelimiter.slidingwindow.SlidingWindowRateLimiter;
import com.zemnitskiy.httpratelimiter.ratelimiter.slidingwindow.SlidingWindowRedisRateLimiter;
import com.zemnitskiy.httpratelimiter.strategy.RateLimiterStrategy;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Configuration class for setting up rate limiter strategies.
 */
@Configuration
public class RateLimiterConfig {

  @Value("${rateLimiter.maxRequestsPerPeriod}")
  private int maxRequests;

  @Value("${rateLimiter.basePeriod}")
  private Duration basePeriod;

  @Value("classpath:rate_limiter.lua")
  private Resource luaScript;

  /**
   * Configures the FixedWindowRateLimiter strategy.
   *
   * @return the configured FixedWindowRateLimiter instance
   */
  @Bean
  @ConditionalOnProperty(name = "rateLimiter.mode", havingValue = "fixedWindowRateLimiter")
  public RateLimiterStrategy fixedWindowRateLimiter() {
    return new FixedWindowRateLimiter(maxRequests, basePeriod);
  }

  /**
   * Configures the SlidingWindowRedisRateLimiter strategy.
   *
   * @param redisTemplate the Redis template for interacting with Redis
   * @return the configured SlidingWindowRedisRateLimiter instance
   */
  @Bean
  @ConditionalOnProperty(name = "rateLimiter.mode", havingValue = "slidingWindowRedisRateLimiter")
  public RateLimiterStrategy slidingWindowRedisRateLimiter(
      RedisTemplate<String, String> redisTemplate) {
    return new SlidingWindowRedisRateLimiter(redisTemplate, maxRequests, basePeriod, luaScript);
  }

  /**
   * Configures the SlidingWindowRateLimiter strategy.
   *
   * @return the configured SlidingWindowRateLimiter instance
   */
  @Bean
  @ConditionalOnProperty(name = "rateLimiter.mode", havingValue = "slidingWindowRateLimiter")
  public RateLimiterStrategy slidingWindowRateLimiter() {
    return new SlidingWindowRateLimiter(maxRequests, basePeriod);
  }
}
