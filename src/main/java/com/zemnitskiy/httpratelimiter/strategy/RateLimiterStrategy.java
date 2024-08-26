package com.zemnitskiy.httpratelimiter.strategy;

/**
 * The {@code RateLimiterStrategy} interface defines a strategy for limiting the rate of requests.
 *
 * <p>Implementations of this interface are responsible for applying specific rate limiting algorithms,
 * such as fixed window, sliding window, or token bucket, to manage the frequency of requests
 * based on a unique key (e.g., client identifier).
 */
public interface RateLimiterStrategy {

  /**
   * Determines whether a request associated with the given key should be allowed or blocked.
   *
   * <p>This method applies the rate limiting logic and throws a {@link RateLimitExceededException}
   * if the request exceeds the allowed rate.
   *
   * @param key the unique key representing the client or request source
   * @throws RateLimitExceededException if the request exceeds the allowed rate limit
   */
  void allowRequest(String key) throws RateLimitExceededException;

}