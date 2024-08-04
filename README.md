# HttpRateLimiter

## Description

HttpRateLimiter is a library for rate limiting incoming requests to your server. It supports various rate limiting strategies, including fixed window, sliding window, and sliding window using Redis for data persistence.

## Configuration

To use HttpRateLimiter, you need to add the following parameters to your `application.properties` file:

```properties
spring.application.name=HttpRateLimiter
baseMaxRequestsPerPeriod=10
basePeriod=1m
rateLimiterStrategy=redis
```

# Parameter Descriptions

## `spring.application.name`
The name of your application.

## `baseMaxRequestsPerPeriod`
The maximum number of requests allowed in the specified period.

## `basePeriod`
The period for rate limiting (can be "second", "minute", "hour").

## `rateLimiterStrategy`
The rate limiting strategy. Available strategies are:
- **fixed**: Fixed window.
- **sliding**: Sliding window.
- **redis**: Sliding window using Redis.

---

## Rate Limiting Strategies

### Fixed Window
**Advantages:**
- Low computational overhead.

**Disadvantages:**
- Can cause request spikes at the start of each window, potentially overloading the server.
- Less precise rate limiting compared to other methods.

### Sliding Window
**Advantages:**
- More accurate rate limiting.
- Avoids request spikes typical of fixed window.

**Disadvantages:**
- Higher resource usage compared to the fixed window.

### Redis (Sliding Window with Redis)
**Advantages:**
- Suitable for distributed systems with multiple application instances.
- Stores data in Redis, ensuring consistent rate limiting across different instances.

**Disadvantages:**
- Requires Redis to be installed and configured.
- Dependency on an external service may add latency and increase system complexity.

---

## Usage

To start using HttpRateLimiter, ensure your `application.properties` file is configured correctly as shown above. Currently, the client key can only be the client's IP address.

---

## Running the Spring Application

To run the Spring application, follow these steps:

1. Build the project: Run `mvn clean install` 
2. Start the application: Run `java -jar target/HttpRateLimiter.jar` for Maven

---

## Connecting to Redis

If you are using the redis strategy, ensure Redis is installed and configured. The project also includes a Docker file for starting Redis locally:

Run `docker-compose up -d`

This will start a Redis container in the background, ready to be used by your application.

---