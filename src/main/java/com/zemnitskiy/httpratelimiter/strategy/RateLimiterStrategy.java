package com.zemnitskiy.httpratelimiter.strategy;

public interface RateLimiterStrategy  {

  void allowRequestOrThrowException(String key);

}
