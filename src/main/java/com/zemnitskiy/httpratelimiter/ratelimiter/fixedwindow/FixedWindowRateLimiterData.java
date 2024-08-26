package com.zemnitskiy.httpratelimiter.ratelimiter.fixedwindow;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public record FixedWindowRateLimiterData (AtomicInteger counter, AtomicLong startTime){}
