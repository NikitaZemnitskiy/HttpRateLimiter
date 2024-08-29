package com.zemnitskiy.httpratelimiter.ratelimiter.fixedwindow;

import java.util.concurrent.atomic.AtomicInteger;

public record FixedWindowRateLimiterData(AtomicInteger counter, long startTime) {}
