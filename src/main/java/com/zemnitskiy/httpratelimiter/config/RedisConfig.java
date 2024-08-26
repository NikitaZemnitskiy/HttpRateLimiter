package com.zemnitskiy.httpratelimiter.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * {@code RedisConfig} is a configuration class that sets up the RedisTemplate used for interacting
 * with Redis in the application. This configuration is only active when the property
 * {@code rateLimiter.mode} is set to {@code slidingWindowRedisRateLimiter}.
 *
 * <p>The {@link RedisTemplate} is configured with custom serializers:
 * <ul>
 *   <li>{@link StringRedisSerializer} for serializing the keys as strings.</li>
 *   <li>{@link GenericJackson2JsonRedisSerializer} for serializing the values as JSON objects.</li>
 * </ul>
 */
@Configuration
@ConditionalOnProperty(name = "rateLimiter.mode", havingValue = "slidingWindowRedisRateLimiter")
public class RedisConfig {

  /**
   * Defines the {@link RedisTemplate} bean used for interacting with Redis.
   *
   * <p>The template is configured to use {@link StringRedisSerializer} for keys
   * and {@link GenericJackson2JsonRedisSerializer} for values, ensuring that data
   * is stored in a format that is both human-readable and easy to deserialize.
   *
   * @param redisConnectionFactory the factory that provides Redis connections
   * @return a configured {@link RedisTemplate} instance
   */
  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(redisConnectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    return template;
  }
}