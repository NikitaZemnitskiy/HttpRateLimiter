/*
package com.zemnitskiy.httpratelimiter.config;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.zemnitskiy.httpratelimiter.strategy.RateLimitExceededException;
import com.zemnitskiy.httpratelimiter.service.RateLimiterService;
import com.zemnitskiy.httpratelimiter.strategy.ClientKeyStrategy;

import com.zemnitskiy.httpratelimiter.web.RateLimiterInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
public class RateLimiterInterceptorTest {

  @Mock
  private RateLimiterService rateLimiterService;

  @Mock
  private ClientKeyStrategy clientKeyStrategy;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @InjectMocks
  private RateLimiterInterceptor rateLimiterInterceptor;

  @BeforeEach
  public void setUp() {
    rateLimiterInterceptor = new RateLimiterInterceptor(rateLimiterService, clientKeyStrategy);
  }

  @Test
  public void testPreHandle_AllowsRequest() throws Exception {
    String clientKey = "client-ip-1";
    when(clientKeyStrategy.getClientKey(request)).thenReturn(clientKey);
    when(rateLimiterService.isAllowed(clientKey)).thenReturn(true);

    boolean result = rateLimiterInterceptor.preHandle(request, response, new Object());

    assertTrue(result);
    verify(response, never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    verify(response, never()).addHeader(eq("Retry-After"), anyString());
  }

  @Test
  public void testPreHandle_DeniesRequest() throws Exception {
    String clientKey = "client-ip-2";
    when(clientKeyStrategy.getClientKey(request)).thenReturn(clientKey);
    when(rateLimiterService.isAllowed(clientKey)).thenReturn(false);

    RateLimitExceededException exception = assertThrows(RateLimitExceededException.class, () -> {
      rateLimiterInterceptor.preHandle(request, response, new Object());
    });

    assertEquals("Too many requests", exception.getMessage());
    verify(response, never()).setStatus(HttpServletResponse.SC_OK);
    verify(response, never()).addHeader(eq("Retry-After"), anyString());
  }
}*/
