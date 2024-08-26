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
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class SlidingWindowRedisRateLimiterTest {

  @Mock
  private RedisTemplate<String, String> redisTemplate;

  @Mock
  private Cache<String, Long> cache;

  @InjectMocks
  private SlidingWindowRedisRateLimiter rateLimiter;

  @Captor
  private ArgumentCaptor<RedisScript<Long>> scriptCaptor;

  private final int maxRequests = 5;
  private final Duration basePeriod = Duration.ofSeconds(10);

  @BeforeEach
  public void setUp() {
    ReflectionTestUtils.setField(rateLimiter, "maxRequests", maxRequests);
    ReflectionTestUtils.setField(rateLimiter, "basePeriod", basePeriod);
  }

  @Test
  public void testAllowRequest_WithinLimit() {
    when(redisTemplate.execute(any(), anyList(), anyString(), anyString())).thenReturn(0L);
    assertDoesNotThrow(() -> rateLimiter.allowRequest("client1"));
  }

  @Test
  public void testAllowRequest_ExceedingLimit() {
    when(redisTemplate.execute(any(), anyList(), anyString(), anyString())).thenReturn(2000L);

    RateLimitExceededException exception = assertThrows(RateLimitExceededException.class,
        () -> rateLimiter.allowRequest("client1"));
    assertEquals("Too many requests. You have only 5 requests for 10 seconds",
        exception.getMessage());
  }

  @Test
  public void testAllowRequest_ExceedingLimitWithCachedRetry() {
    when(cache.getIfPresent("client1")).thenReturn(System.currentTimeMillis() + 5000);

    RateLimitExceededException exception = assertThrows(RateLimitExceededException.class,
        () -> rateLimiter.allowRequest("client1"));
    assertEquals("Too many requests. You have only 5 requests for 10 seconds",
        exception.getMessage());
  }

  @Test
  public void testRedisScriptExecution() {
    when(redisTemplate.execute(scriptCaptor.capture(), anyList(), anyString(),
        anyString())).thenReturn(0L);

    rateLimiter.allowRequest("client1");

    verify(redisTemplate).execute(scriptCaptor.capture(), anyList(), anyString(), anyString());
    assertNotNull(scriptCaptor.getValue());
  }

  @Test
  public void testExceptionMessage() {
    String expectedMessage = "Too many requests. You have only 5 requests for 10 seconds";
    assertEquals(expectedMessage,
        ReflectionTestUtils.invokeMethod(rateLimiter, "getExceptionMessage"));
  }

  @Test
  public void testCreateRateLimiterScript() {
    RedisScript<Long> script = ReflectionTestUtils.invokeMethod(rateLimiter,
        "createRateLimiterScript");
    assertNotNull(script, "The Lua script should be loaded properly.");
  }

  @Test
  public void testRateLimitExceededException() {
    when(cache.getIfPresent(anyString())).thenReturn(null);
    when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString()))
        .thenReturn(1000L);

    assertThrows(RateLimitExceededException.class, () -> {
      rateLimiter.allowRequest("clientKey");
    }, "Too many requests. You have only 5 requests for 10 seconds");
  }

  @Test
  public void testIllegalStateException() {
    when(cache.getIfPresent(anyString())).thenReturn(null);
    when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString()))
        .thenReturn(null);

    assertThrows(IllegalStateException.class, () ->
            rateLimiter.allowRequest("clientKey"),
        "Could not get result from Redis lua script for clientKey");
  }
}