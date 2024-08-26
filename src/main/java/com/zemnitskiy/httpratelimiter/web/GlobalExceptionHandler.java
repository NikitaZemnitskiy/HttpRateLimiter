package com.zemnitskiy.httpratelimiter.web;

import com.zemnitskiy.httpratelimiter.strategy.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global exception handler for the application.
 * <p>
 * This class handles exceptions globally across the application by intercepting exceptions thrown
 * from controllers and providing a standardized response.
 * </p>
 */
@ControllerAdvice
public class GlobalExceptionHandler {

  private final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Handles {@link RateLimitExceededException} thrown by rate limiter strategies.
   * <p>
   * This method logs the exception details and returns a {@link ResponseEntity} with HTTP status
   * 429 (Too Many Requests) and a Retry-After header indicating the time in seconds until the client
   * can make a new request.
   * </p>
   *
   * @param ex the {@link RateLimitExceededException} to handle
   * @return a {@link ResponseEntity} containing the exception message and Retry-After header
   */
  @ExceptionHandler(RateLimitExceededException.class)
  public ResponseEntity<String> handleRateLimitExceededException(RateLimitExceededException ex) {
    log.debug("RateLimitExceededException handled with ex: {} and retry after {}", ex.getMessage(),
        ex.getRetryAfter());
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header("Retry-After", String.valueOf(ex.getRetryAfter()))
        .body(ex.getMessage());
  }
}
