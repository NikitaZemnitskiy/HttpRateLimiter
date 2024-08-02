package com.zemnitskiy.httpratelimiter.service;

import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Service
public class CacheService {
  private final JedisPool jedisPool;

  public CacheService(JedisPool jedisPool) {
    this.jedisPool = jedisPool;
  }

  public void set(String key, String value, int ttl) {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.set(key, value);
      jedis.expire(key, ttl);
    }
  }

  public String get(String key) {
    try (Jedis jedis = jedisPool.getResource()) {
      return jedis.get(key);
    }
  }

  public void delete(String key) {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.del(key);
    }
  }
}
