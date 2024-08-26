package com.zemnitskiy.httpratelimiter.web;

import com.zemnitskiy.httpratelimiter.strategy.ClientKeyStrategy;
import com.zemnitskiy.httpratelimiter.strategy.RateLimitExceededException;
import com.zemnitskiy.httpratelimiter.strategy.RateLimiterStrategy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import reactor.util.annotation.NonNull;

/**
 * The {@code RateLimiterInterceptor} class implements a Spring MVC interceptor to apply rate limiting
 * based on a client's unique key.
 *
 * <p>This interceptor checks each incoming request, determines the client's key, and applies the
 * appropriate rate limiting strategy. If the request exceeds the rate limit, an exception is thrown.
 */
@Component
public class RateLimiterInterceptor implements HandlerInterceptor {

  private final RateLimiterStrategy rateLimiter;
  private final ClientKeyStrategy clientKeyStrategy;
  private final Logger log = LoggerFactory.getLogger(RateLimiterInterceptor.class);

  /**
   * Constructs a {@code RateLimiterInterceptor} with the specified rate limiter and client key strategy.
   *
   * @param rateLimiter the strategy to be used for rate limiting
   * @param clientKeyStrategy the strategy to determine the client's unique key
   */
  public RateLimiterInterceptor(RateLimiterStrategy rateLimiter,
      ClientKeyStrategy clientKeyStrategy) {
    this.rateLimiter = rateLimiter;
    this.clientKeyStrategy = clientKeyStrategy;
  }

  /**
   * Intercepts the request before it reaches the controller, applies rate limiting logic, and allows or blocks
   * the request based on the rate limit.
   *
   * <p>If the request is allowed, it proceeds to the next step in the request processing chain. If the rate limit
   * is exceeded, an exception is thrown.
   *
   * @param request the current HTTP request
   * @param response the current HTTP response
   * @param handler the chosen handler to execute, for type and/or instance examination
   * @return {@code true} if the request is allowed to proceed; {@code false} otherwise
   * @throws RateLimitExceededException if the rate limit is exceeded for the client's key
   */
  @Override
  public boolean preHandle(@NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler) {
    String clientKey = clientKeyStrategy.getClientKey(request);
    log.debug("{} Trying access endpoint", clientKey);
    rateLimiter.allowRequest(clientKey);
    log.debug("{} Endpoint access Allowed", clientKey);
    return true;
  }
}