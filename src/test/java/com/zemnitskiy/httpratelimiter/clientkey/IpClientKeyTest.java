package com.zemnitskiy.httpratelimiter.clientkey;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IpClientKeyTest {

  @Mock
  HttpServletRequest request;

  private final IpClientKey ipClientKey = new IpClientKey();


  @Test
  public void testGetClientIP_XForwardedFor() {
    Mockito.when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.0.1, 192.168.0.2");
    assertEquals("192.168.0.1", ipClientKey.getClientKey(request));
  }

  @Test
  public void testGetClientIP_XForwardedForSingleIP() {
    Mockito.when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.0.1");
    assertEquals("192.168.0.1", ipClientKey.getClientKey(request));
  }

  @Test
  public void testGetClientIP_XRealIP() {
    Mockito.when(request.getHeader("X-Forwarded-For")).thenReturn(null);
    Mockito.when(request.getHeader("X-Real-IP")).thenReturn("192.168.0.3");
    assertEquals("192.168.0.3", ipClientKey.getClientKey(request));
  }

  @Test
  public void testGetClientIP_RemoteAddr() {
    Mockito.when(request.getHeader("X-Forwarded-For")).thenReturn(null);
    Mockito.when(request.getHeader("X-Real-IP")).thenReturn(null);
    Mockito.when(request.getRemoteAddr()).thenReturn("192.168.0.4");
    assertEquals("192.168.0.4", ipClientKey.getClientKey(request));
  }
}