# HttpRateLimiter

## Description

HttpRateLimiter is a library for rate limiting incoming requests to your server. It supports various rate limiting strategies, including fixed window, sliding window, and sliding window using Redis for data persistence.

## Configuration

To use HttpRateLimiter, you need to add the following parameters to your `application.properties` file:

```properties
rateLimiter.maxRequestsPerPeriod=5
rateLimiter.basePeriod=30s
rateLimiter.mode=fixedWindowRateLimiter
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

# Parameter Descriptions

## `baseMaxRequestsPerPeriod`
The maximum number of requests allowed in the specified period.

## `basePeriod`
The period for rate limiting (can be "second", "minute", "hour").

## `mode`
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

## Testing the Application

To test the application, you can use Maven commands. There are two main types of tests: unit tests and regression tests.

### Running Unit Tests

To run unit tests only, use the following Maven command:

```sh
mvn clean test
```

### Running Regression Tests

To run regression tests, you need to use a specific Maven profile. This profile is set up to execute tests that are more comprehensive and time-consuming.

Running Regression Tests with the Profile
To run regression tests, use the following Maven command:

```sh
mvn clean install -PrunRegressionTests
```