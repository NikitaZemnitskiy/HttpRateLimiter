package com.zemnitskiy.httpratelimiter.service;

import com.zemnitskiy.httpratelimiter.strategy.RateLimiterStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

  private final Logger log = LoggerFactory.getLogger(RateLimiterService.class);
  private final RateLimiterStrategy rateLimiter;

  public RateLimiterService(RateLimiterStrategy rateLimiterStrategy) {
    this.rateLimiter = rateLimiterStrategy;
  }

  public boolean isAllowed(String clientKey) {
    rateLimiter.allowRequestOrThrowException(clientKey);
    log.debug("{}Endpoint access Allowed", clientKey);
    return true;
  }
}
