package com.zemnitskiy.httpratelimiter.strategy;

import jakarta.servlet.http.HttpServletRequest;

public interface ClientKeyStrategy {

  String getClientKey(HttpServletRequest request);
}
