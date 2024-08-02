package com.zemnitskiy.httpratelimiter.config;

import com.zemnitskiy.httpratelimiter.exception.RateLimitExceededException;
import com.zemnitskiy.httpratelimiter.service.RateLimiterService;
import com.zemnitskiy.httpratelimiter.strategy.ClientKeyStrategy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


@Component
public class RateLimiterInterceptor implements HandlerInterceptor {

  private final RateLimiterService rateLimiterService;
  private final ClientKeyStrategy clientKeyStrategy;

  public RateLimiterInterceptor(RateLimiterService rateLimiterService,
      ClientKeyStrategy clientKeyStrategy) {
    this.rateLimiterService = rateLimiterService;
    this.clientKeyStrategy = clientKeyStrategy;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    String clientKey = clientKeyStrategy.getClientKey(request);
    if (!rateLimiterService.isAllowed(clientKey)) {
      throw new RateLimitExceededException("Too many requests");
    }
    return true;
  }
}
