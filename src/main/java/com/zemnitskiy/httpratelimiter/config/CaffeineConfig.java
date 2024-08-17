package com.zemnitskiy.httpratelimiter.config;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class CaffeineConfig {

  @Value("${rateLimiter.basePeriod}")
  private Duration basePeriod;

  @Bean
  @Profile("fixedWindowRateLimiter")
  public Cache<String, AtomicInteger> atomicIntegerCache() {
    return Caffeine.newBuilder()
        .expireAfterWrite(basePeriod)
        .build();
  }

  @Bean
  @Profile("slidingWindowRateLimiter")
  public Cache<String, Queue<Long>> longQueueCache() {
    return Caffeine.newBuilder()
        .expireAfterAccess(basePeriod)
        .build();
  }

  @Bean
  @Profile("slidingWindowRedisRateLimiter")
  public Cache<String, Long> longCache() {
    return Caffeine.newBuilder()
        .expireAfterAccess(basePeriod)
        .build();
  }
}
