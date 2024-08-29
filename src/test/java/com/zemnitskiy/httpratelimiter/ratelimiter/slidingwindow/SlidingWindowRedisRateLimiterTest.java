package com.zemnitskiy.httpratelimiter.ratelimiter.slidingwindow;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.Cache;
import com.zemnitskiy.httpratelimiter.strategy.RateLimitExceededException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SlidingWindowRedisRateLimiterTest {

  @Mock
  private RedisTemplate<String, String> redisTemplate;

  @Mock
  private Cache<String, Long> cache;

  @Mock
  private Resource luaScriptResource;

  @Captor
  private ArgumentCaptor<RedisScript<Long>> scriptCaptor;

  private SlidingWindowRedisRateLimiter rateLimiter;

  private static final int MAX_REQUEST = 5;
  private static final Duration BASE_PERIOD = Duration.ofSeconds(10);

  @BeforeEach
  public void setUp() throws IOException {
    when(luaScriptResource.getInputStream()).thenReturn(
        new ByteArrayInputStream("return 0".getBytes(StandardCharsets.UTF_8))
    );
    rateLimiter = new SlidingWindowRedisRateLimiter(redisTemplate, MAX_REQUEST, BASE_PERIOD,
        luaScriptResource);

    // Set private fields using reflection if necessary
    ReflectionTestUtils.setField(rateLimiter, "cache", cache);
  }

  @Test
  void testAllowRequest_WithinLimit() {
    when(redisTemplate.execute(any(), anyList(), anyString(), anyString())).thenReturn(0L);
    assertDoesNotThrow(() -> rateLimiter.allowRequest("client1"));
  }

  @Test
  void testAllowRequest_ExceedingLimit() {
    when(redisTemplate.execute(any(), anyList(), anyString(), anyString())).thenReturn(2000L);

    RateLimitExceededException exception = assertThrows(RateLimitExceededException.class,
        () -> rateLimiter.allowRequest("client1"));
    assertEquals("Too many requests. You have only 5 requests for 10 seconds",
        exception.getMessage());
  }

  @Test
  void testAllowRequest_ExceedingLimitWithCachedRetry() {
    when(cache.getIfPresent("client1")).thenReturn(System.currentTimeMillis() + 5000);

    RateLimitExceededException exception = assertThrows(RateLimitExceededException.class,
        () -> rateLimiter.allowRequest("client1"));
    assertEquals("Too many requests. You have only 5 requests for 10 seconds",
        exception.getMessage());
  }

  @Test
  void testRedisScriptExecution() {
    when(redisTemplate.execute(scriptCaptor.capture(), anyList(), anyString(),
        anyString())).thenReturn(0L);

    rateLimiter.allowRequest("client1");

    verify(redisTemplate).execute(scriptCaptor.capture(), anyList(), anyString(), anyString());
    assertNotNull(scriptCaptor.getValue());
  }

  @Test
  void testExceptionMessage() {
    String expectedMessage = "Too many requests. You have only 5 requests for 10 seconds";
    assertEquals(expectedMessage,
        ReflectionTestUtils.invokeMethod(rateLimiter, "getExceptionMessage"));
  }

  @Test
  void testRateLimitExceededException() {
    when(cache.getIfPresent(anyString())).thenReturn(null);
    when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString()))
        .thenReturn(1000L);

    assertThrows(RateLimitExceededException.class, () -> rateLimiter.allowRequest("clientKey"),
        "Too many requests. You have only 5 requests for 10 seconds");
  }

  @Test
  void testIllegalStateException() {
    when(cache.getIfPresent(anyString())).thenReturn(null);
    when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString()))
        .thenReturn(null);

    assertThrows(IllegalStateException.class, () ->
            rateLimiter.allowRequest("clientKey"),
        "Could not get result from Redis lua script for clientKey");
  }
}