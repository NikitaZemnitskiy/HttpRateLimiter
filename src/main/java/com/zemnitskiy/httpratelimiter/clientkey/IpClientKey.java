package com.zemnitskiy.httpratelimiter.clientkey;

import com.zemnitskiy.httpratelimiter.strategy.ClientKeyStrategy;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

/**
 * {@code IpClientKey} is a service class that implements the {@link ClientKeyStrategy}
 * interface to extract the client's IP address from an HTTP request. This IP address
 * is used as the client key in rate limiting strategies.
 *
 * <p>The class first checks for the presence of the "X-Forwarded-For" header, which is
 * commonly used in proxy setups to identify the original IP address of the client.
 * If multiple IP addresses are found in this header, the first IP address in the list
 * is used. If the "X-Forwarded-For" header is not available or is empty, it then checks
 * the "X-Real-IP" header. If neither header is available, the IP address is retrieved
 * from the request's remote address.
 *
 * <p>This class is annotated with {@link Service}, making it a Spring-managed bean.
 */
@Service
public class IpClientKey implements ClientKeyStrategy {

  /**
   * Extracts the client's IP address from the given {@link HttpServletRequest}.
   *
   * <p>The method first checks the "X-Forwarded-For" header for the client's IP address.
   * If this header contains multiple IP addresses (comma-separated), the first one is
   * returned. If the header is absent or empty, the method checks the "X-Real-IP" header.
   * If neither header is available, the method returns the remote address of the request.
   *
   * @param request the {@link HttpServletRequest} from which the client's IP address is to be extracted
   * @return the client's IP address, either from the "X-Forwarded-For" header, the "X-Real-IP" header,
   * or the request's remote address
   */
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