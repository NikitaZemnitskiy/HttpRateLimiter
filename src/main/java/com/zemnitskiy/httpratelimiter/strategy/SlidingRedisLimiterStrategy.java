package com.zemnitskiy.httpratelimiter.strategy;

import com.zemnitskiy.httpratelimiter.exception.RateLimitExceededException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Profile("slidingWindowRedisRateLimiter")
@Component
public final class SlidingRedisLimiterStrategy implements RateLimiterStrategy {

  private final RedisTemplate<String, String> redisTemplate;

  @Value("${baseMaxRequestsPerPeriod}")
  private int maxRequests;

  @Value("${basePeriod}")
  private long basePeriod;

  private String luaScript;

  public SlidingRedisLimiterStrategy(RedisTemplate<String, String> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @PostConstruct
  public void init()  {
    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("rate_limiter.lua")) {
      if (inputStream == null) {
        throw new IOException("Lua script not found");
      }
      luaScript = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load Lua script", e);
    }
  }

  public void allowRequestOrThrowException(String clientKey) {

    Long result = redisTemplate.execute((RedisCallback<Long>) connection ->
        connection.eval(luaScript.getBytes(),
            ReturnType.INTEGER,
            1,
            clientKey.getBytes(),
            String.valueOf(maxRequests).getBytes(),
            String.valueOf(basePeriod).getBytes()
        ));
    if (result == null || result != 1) {
      throw new RateLimitExceededException("Too many requests. You have only " + maxRequests + " requests." + " for " + basePeriod/1000000000 + " seconds");
    }
  }

}
