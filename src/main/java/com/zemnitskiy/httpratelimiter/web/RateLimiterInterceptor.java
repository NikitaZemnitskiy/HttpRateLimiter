package com.zemnitskiy.httpratelimiter.web;

import com.zemnitskiy.httpratelimiter.strategy.ClientKeyStrategy;
import com.zemnitskiy.httpratelimiter.strategy.RateLimiterStrategy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


@Component
public class RateLimiterInterceptor implements HandlerInterceptor {

  private final RateLimiterStrategy rateLimiter;
  private final ClientKeyStrategy clientKeyStrategy;
  private final Logger log = LoggerFactory.getLogger(RateLimiterInterceptor.class);

  public RateLimiterInterceptor(RateLimiterStrategy rateLimiter,
      ClientKeyStrategy clientKeyStrategy) {
    this.rateLimiter = rateLimiter;
    this.clientKeyStrategy = clientKeyStrategy;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
      Object handler) {
    String clientKey = clientKeyStrategy.getClientKey(request);
    log.debug("{} Trying access endpoint", clientKey);
    rateLimiter.allowRequest(clientKey);
    log.debug("{} Endpoint access Allowed", clientKey);
    return true;
  }
}
