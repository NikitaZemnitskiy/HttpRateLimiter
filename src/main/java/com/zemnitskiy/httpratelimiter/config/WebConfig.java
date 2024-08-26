package com.zemnitskiy.httpratelimiter.config;

import com.zemnitskiy.httpratelimiter.web.RateLimiterInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * {@code WebConfig} is a configuration class that implements the {@link WebMvcConfigurer}
 * interface to customize the Spring MVC configuration.
 *
 * <p>This class registers a custom interceptor, {@link RateLimiterInterceptor}, which applies
 * rate limiting logic to incoming HTTP requests. The interceptor is added to the Spring MVC
 * interceptor registry.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final RateLimiterInterceptor rateLimiterInterceptor;

  /**
   * Constructs a new {@code WebConfig} with the specified {@link RateLimiterInterceptor}.
   *
   * @param rateLimiterInterceptor the rate limiter interceptor to be added to the registry
   */
  public WebConfig(RateLimiterInterceptor rateLimiterInterceptor) {
    this.rateLimiterInterceptor = rateLimiterInterceptor;
  }

  /**
   * Adds the {@link RateLimiterInterceptor} to the Spring MVC interceptor registry.
   *
   * <p>This method is called by the Spring framework to register the interceptor
   * with the application, ensuring that it is applied to all incoming HTTP requests.
   *
   * @param registry the {@link InterceptorRegistry} used to register the interceptor
   */
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(rateLimiterInterceptor);
  }
}