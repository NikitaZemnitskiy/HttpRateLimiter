package com.zemnitskiy.httpratelimiter.service;

import com.zemnitskiy.httpratelimiter.strategy.RateLimiterStrategy;
import com.zemnitskiy.httpratelimiter.strategy.RateLimiterStrategyFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

  private final RateLimiterStrategyFactory strategyFactory;
  private final ConcurrentHashMap<String, RateLimiterStrategy> clients = new ConcurrentHashMap<>();

  public RateLimiterService(RateLimiterStrategyFactory strategyFactory) {
    this.strategyFactory = strategyFactory;
  }

  public boolean isAllowed(String clientKey) {
    RateLimiterStrategy rateLimiter = clients.computeIfAbsent(clientKey,
        _ -> strategyFactory.createStrategy());
    return rateLimiter.allowRequest();
  }
}
