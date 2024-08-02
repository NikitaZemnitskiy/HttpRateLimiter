package com.zemnitskiy.httpratelimiter.strategy;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class IpClientKeyStrategy implements ClientKeyStrategy {

  @Override
  public String getClientKey(HttpServletRequest request) {
    String clientIp = request.getHeader("X-Forwarded-For");
    if (clientIp == null || clientIp.isEmpty()) {
      clientIp = request.getRemoteAddr();
    }
    return clientIp;
  }
}
