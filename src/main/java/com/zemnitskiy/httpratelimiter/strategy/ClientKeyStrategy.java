package com.zemnitskiy.httpratelimiter.strategy;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The {@code ClientKeyStrategy} interface defines a strategy for extracting a unique client key
 * from an {@link HttpServletRequest}. This key is used in rate limiting to identify individual clients.
 *
 * <p>Implementations of this interface provide different ways to generate or extract a client key,
 * such as using IP addresses, headers, or session IDs.
 */
public interface ClientKeyStrategy {

  /**
   * Extracts a unique client key from the given {@link HttpServletRequest}.
   *
   * <p>This method is used to identify the client making the request, which is essential
   * for applying rate limiting on a per-client basis.
   *
   * @param request the HTTP request from which the client key should be extracted
   * @return a unique client key as a {@code String}
   */
  String getClientKey(HttpServletRequest request);
}