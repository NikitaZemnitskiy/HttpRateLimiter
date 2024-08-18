package com.zemnitskiy.httpratelimiter.clientkey;

import com.zemnitskiy.httpratelimiter.strategy.ClientKeyStrategy;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class IpClientKey implements ClientKeyStrategy {

  @Override
  public String getClientKey(HttpServletRequest request) {
    String ipAddress = request.getHeader("X-Forwarded-For");
    if (ipAddress != null && !ipAddress.isEmpty()) {
      int index = ipAddress.indexOf(",");
      if (index > 0) {
        return ipAddress.substring(0, index).trim();
      } else {
        return ipAddress.trim();
      }
    }
    ipAddress = request.getHeader("X-Real-IP");
    if (ipAddress != null && !ipAddress.isEmpty()) {
      return ipAddress.trim();
    }
    return request.getRemoteAddr();
  }
}
