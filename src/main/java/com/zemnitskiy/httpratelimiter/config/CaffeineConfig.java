package com.zemnitskiy.httpratelimiter.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zemnitskiy.httpratelimiter.ratelimiter.fixedwindow.FixedWindowRateLimiterData;
import java.time.Duration;
import java.util.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CaffeineConfig {

  @Value("${rateLimiter.basePeriod}")
  private Duration basePeriod;

  @Bean
  public Cache<String, FixedWindowRateLimiterData> fixedWindowCache() {
    return Caffeine.newBuilder()
        .expireAfterWrite(basePeriod)
        .build();
  }

  @Bean
  public Cache<String, Queue<Long>> slidingWindowCache() {
    return Caffeine.newBuilder()
        .expireAfterAccess(basePeriod)
        .build();
  }

  @Bean
  public Cache<String, Long> slidingWindowRedis() {
    return Caffeine.newBuilder()
        .expireAfterAccess(basePeriod)
        .build();
  }
}
