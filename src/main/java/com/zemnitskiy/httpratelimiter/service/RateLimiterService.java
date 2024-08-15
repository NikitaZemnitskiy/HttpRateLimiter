package com.zemnitskiy.httpratelimiter.service;

import com.zemnitskiy.httpratelimiter.strategy.RateLimiterStrategy;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

  private final Logger slf4jLogger = Logger.getLogger("RateLimiterService");
  private final RateLimiterStrategy rateLimiter;

  public RateLimiterService(RateLimiterStrategy rateLimiterStrategy) {
    this.rateLimiter = rateLimiterStrategy;
  }

  public boolean isAllowed(String clientKey) {
    rateLimiter.allowRequestOrThrowException(clientKey);
    slf4jLogger.log(Level.INFO, clientKey + "Endpoint access Allowed");
    return true;
  }
}
