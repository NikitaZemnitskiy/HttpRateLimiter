package com.zemnitskiy.httpratelimiter.strategy;

public interface RateLimiterStrategy {

  void allowRequest(String key) throws RateLimitExceededException;

}
