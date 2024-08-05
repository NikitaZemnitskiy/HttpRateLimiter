package com.zemnitskiy.httpratelimiter.service;

import com.zemnitskiy.httpratelimiter.strategy.RateLimiterStrategy;
import com.zemnitskiy.httpratelimiter.strategy.RateLimiterStrategyFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

  private final RateLimiterStrategyFactory strategyFactory;
  private final Logger slf4jLogger = Logger.getLogger("RateLimiterService");
  private final ConcurrentHashMap<String, RateLimiterStrategy> clients = new ConcurrentHashMap<>();

  public RateLimiterService(RateLimiterStrategyFactory strategyFactory) {
    this.strategyFactory = strategyFactory;
  }

  public boolean isAllowed(String clientKey) {
    RateLimiterStrategy rateLimiter = clients.computeIfAbsent(clientKey,
        _ -> strategyFactory.createStrategy(clientKey));
    boolean response = rateLimiter.allowRequest();
    slf4jLogger.log(Level.INFO, clientKey + "Endpoint access " + (response?" Allowed":" Denied"));
    return rateLimiter.allowRequest();
  }
}
