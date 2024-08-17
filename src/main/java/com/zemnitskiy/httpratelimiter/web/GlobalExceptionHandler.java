package com.zemnitskiy.httpratelimiter.web;

import com.zemnitskiy.httpratelimiter.strategy.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

  private final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(RateLimitExceededException.class)
  public ResponseEntity<String> handleRateLimitExceededException(RateLimitExceededException ex) {
    log.debug("RateLimitExceededException handled with ex: {} and retry after {}", ex.getMessage(),
        ex.getRetryAfter());
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header("Retry-After", String.valueOf(ex.getRetryAfter()))
        .body(ex.getMessage());
  }
}
