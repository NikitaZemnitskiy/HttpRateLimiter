package com.zemnitskiy.httpratelimiter.service;

import java.util.List;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

@Service
public class CacheService {

  private final JedisPool jedisPool;

  public CacheService(JedisPool jedisPool) {
    this.jedisPool = jedisPool;
  }

  public boolean addItemToQueue(String queueKey, String item, long ttlInSeconds, long maxQueueSize) {
    try (Jedis jedis = jedisPool.getResource()) {
      while (true) {
        jedis.watch(queueKey);
        long queueSize = jedis.zcard(queueKey);

        if (queueSize < maxQueueSize) {
          Transaction transaction = jedis.multi();
          long score = System.currentTimeMillis() + ttlInSeconds * 1000;
          transaction.zadd(queueKey, score, item);
          List<Object> result = transaction.exec();

          if (result != null) {
            return true;
          }
        } else {
          jedis.unwatch();
          return false;
        }
      }
    }
  }

  public void removeExpiredItems(String queueKey) {
    try (Jedis jedis = jedisPool.getResource()) {
      long currentTime = System.currentTimeMillis();
      jedis.zremrangeByScore(queueKey, Double.NEGATIVE_INFINITY, currentTime);
    }
  }

  public long getQueueSize(String queueKey) {
    try (Jedis jedis = jedisPool.getResource()) {
      return jedis.zcard(queueKey);
    }
  }

  public double getOldestElementTTL(String queueKey) {
    try (Jedis jedis = jedisPool.getResource()) {
      return jedis.zrangeWithScores(queueKey, 0, -1).getFirst().getScore();
    }
  }
}
