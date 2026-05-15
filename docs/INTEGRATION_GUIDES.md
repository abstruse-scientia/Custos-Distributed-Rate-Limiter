# Custos Integration Guide


## 1. Installation

Custos is available on Maven Central. It requires **Java 17+** and a **Spring Boot** environment.

### Maven
```xml
<dependency>
    <groupId>io.github.abstruse-scientia</groupId>
    <artifactId>custos</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle
```groovy
implementation 'io.github.abstruse-scientia:custos:1.0.0'
```

---

## 2. Quick Start

```java
import io.github.abstruse_scientia.custos.annotations.RateLimit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    // Limits this endpoint to 10 requests per user
    @RateLimit
    @GetMapping("/hello")
    public String sayHello() {
        return "Hello, World!";
    }
}
```

When a request exceeds the limit, Custos throws a `RateLimitExceededException` and halts execution before your method is invoked.

---

## 3. Handling RateLimitExceededException

```java
import io.github.abstruse_scientia.custos.exception.RateLimitExceededException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class RateLimitExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<String> handleRateLimitExceeded(RateLimitExceededException ex) {
        HttpHeaders headers = new HttpHeaders();
        // Standard HTTP header instructing the client when to try again
        headers.add(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()));

        return new ResponseEntity<>(
            "Rate limit exceeded. Try again later.", 
            headers, 
            HttpStatus.TOO_MANY_REQUESTS
        );
    }
}
```

---

## 4. Configuring the @RateLimit Annotation

| Field | Type | Default | Description |
|---|---|---|---|
| `keytype` | `KeyType` | `KeyType.USER` | Determines how the request is identified (`USER`, `IP`, or `CUSTOM`). |
| `algorithm` | `Algorithm` | `Algorithm.TOKEN_BUCKET` | The pattern used to limit requests. |
| `capacity` | `int` | `10` | The maximum burst capacity, window size, or bucket size. |
| `rate` | `double` | `5` | The refill rate or number of allowed requests per given threshold. |

### Algorithm Configuration Reference

Different algorithms use the `capacity` and `rate` fields differently. For a detailed  breakdown of each algorithm, refer to the [Algorithms Explained](ALGORITHMS_EXPLAINED.md) documentation.

| Algorithm | `capacity` | `rate` |
|---|---|---|
| `TOKEN_BUCKET` | Maximum tokens the bucket can hold | Tokens added per second |
| `LEAKY_BUCKET` | Maximum queue size for delayed requests | Outflow rate (requests processed per second) |
| `SLIDING_WINDOW` | Maximum requests in derived window | Requests allowed per second (derives window duration) |
| `SLIDING_WINDOW_COUNTER` | Window size in seconds | Requests allowed per window |

**Example configuration:**
```java
@RateLimit(
    keytype = KeyType.IP,
    algorithm = Algorithm.SLIDING_WINDOW_COUNTER,
    capacity = 60, // 60 seconds window
    rate = 100     // 100 requests per 60 seconds
)
@GetMapping("/heavy-computation")
public String doWork() { ... }
```

---

## 5. Global Defaults via application.yml

You can specify global defaults in your `application.yml` file.

### In-Memory Store (Default)
```yaml
custos:
  # The store engine to use: 'memory' (default) or 'redis'
  store: memory
  
custos.token-bucket:
  # Global defaults for rate limiting
  capacity: 20
  rate: 10
```

### Redis Store (Distributed)
```yaml
custos:
  store: redis
  
custos.token-bucket:
  # Global defaults for rate limiting
  capacity: 20
  rate: 10

spring:
  data:
    redis:
      host: redis-server.example.com
      port: 6379
      password: your-redis-password  # Optional
      timeout: 5000ms  # Connection timeout
```

---

## 6. Key Resolution

Custos has three key resolution strategies.

### USER Mode (Default)
Tries to identify the user using the Spring Security context (the authenticated principal). **Requires Spring Security on the classpath**.

### IP Mode
Identifies requests by their source IP address. It works without Spring Security and is proxy-aware, reads reverse-proxy headers like `X-Forwarded-For` and `X-Real-IP` automatically.

### CUSTOM Mode
Use `KeyType.CUSTOM` when you want domain specific rate limiting (e.g., rate limit by account ID or API key header).

**Step 1:** Create your custom `KeyResolver`.
```java
import io.github.abstruse_scientia.custos.resolver.KeyResolver;
import io.github.abstruse_scientia.custos.resolver.KeyType;
import io.github.abstruse_scientia.custos.core.model.RequestContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class ApiKeyResolver implements KeyResolver {
    @Override
    public KeyType getKeyType() {
        return KeyType.CUSTOM;
    }

    @Override
    public String resolve(RequestContext context) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String apiKey = request.getHeader("X-API-KEY");
            return apiKey != null ? apiKey : "anonymous";
        }
        return "anonymous";
    }
}
```

**Step 2:** Use it on your annotation.
```java
@RateLimit(keytype = KeyType.CUSTOM)
@GetMapping("/data")
public String getData() { ... }
```
*(For internal resolution wiring, see [Architecture and Flow](ARCHITECTURE_AND_FLOW.md))*

---

## 7. Switching to Redis (Distributed Mode)

By default, Custos uses a thread safe in memory store. For distributed systems, you should switch to Redis. Custos uses atomic Lua scripts inside Redis.

When `custos.store=redis` is set, Custos automatically registers Redis strategy implementations that execute atomic Lua scripts inside Redis for each rate limit check. See [Architecture & Flow](ARCHITECTURE_AND_FLOW.md) for internals.

### Step 1: Add the Spring Boot Data Redis dependency
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### Step 2: Configure Redis in application.yml

**Basic configuration:**
```yaml
custos:
  store: redis

spring:
  data:
    redis:
      host: localhost
      port: 6379
```

**Production configuration (with connection pooling & resilience):**
```yaml
custos:
  store: redis
  
custos.token-bucket:
  capacity: 20
  rate: 10

spring:
  data:
    redis:
      host: redis-server.prod.com
      port: 6379
      password: ${REDIS_PASSWORD}
      timeout: 5000ms
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
          max-wait: -1ms
        shutdown-timeout: 100ms
```


### Error Handling: What Happens When Redis is Down

**IMPORTANT:** Custos does **NOT automatically fall back** to in memory limits if Redis becomes unreachable.

**Current behavior:**
- When Redis connection fails, **Spring Data Redis** throws a connection exception
- This exception **propagates uncaught** through Custos
- Your application receives HTTP 500 (Internal Server Error)
- Request is **rejected** (fail closed, not fail-open)

**Example error chain:**
```
RedisConnectionFailureException (from Spring Data Redis)
  ↝ RedisTokenBucketStrategy.allow(), RateLimiterEngine.allow(), RateLimitAspect.enforceRateLimit() has no no try-catch
  ↝ Your @RestController
  ↝ HTTP 500 error logged
```

### Implementing Fallback to In-Memory (Custom Resilience)

If you need automatic fallback to in memory limits when Redis fails, create a custom `RateLimitStore`:

```java
import io.github.abstruse_scientia.custos.core.store.RateLimitStore;
import io.github.abstruse_scientia.custos.core.store.InMemoryStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ResilientRateLimitStore implements RateLimitStore {
    
    private static final Logger logger = LoggerFactory.getLogger(ResilientRateLimitStore.class);
    
    private final StringRedisTemplate redisTemplate;
    private final InMemoryStore fallback = new InMemoryStore();
    private volatile boolean redisHealthy = true;

    public ResilientRateLimitStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Object get(String key) {
        try {
            if (redisHealthy) {
                String value = redisTemplate.opsForValue().get(key);
                return value;
            }
        } catch (Exception e) {
            logger.warn("Redis get() failed, switching to fallback", e);
            redisHealthy = false;
        }
        return fallback.get(key);
    }

    @Override
    public void put(String key, Object state) {
        try {
            if (redisHealthy) {
                redisTemplate.opsForValue().set(key, state.toString());
                return;
            }
        } catch (Exception e) {
            logger.warn("Redis put() failed, switching to fallback", e);
            redisHealthy = false;
        }
        fallback.put(key, state);
    }

    @Override
    public Object atomicCompute(String key, java.util.function.BiFunction<String, Object, Object> remappingFunction) {
        try {
            if (redisHealthy) {
                // For Redis, you'd need custom Lua script handling
                // For now, use fallback for atomic operations
            }
        } catch (Exception e) {
            logger.warn("Redis atomic operation failed, switching to fallback", e);
            redisHealthy = false;
        }
        return fallback.atomicCompute(key, remappingFunction);
    }
}
```

**Note:** This overrides the default `InMemoryStore` bean and provides graceful degradation.

### Handling Redis Errors in Your Controller

For explicit error handling, catch Redis exceptions in your `@ControllerAdvice`:

```java
import org.springframework.data.redis.connection.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<String> handleRateLimitExceeded(RateLimitExceededException ex) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()));
        return new ResponseEntity<>("Rate limit exceeded", headers, HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(RedisConnectionFailureException.class)
    public ResponseEntity<String> handleRedisFailure(RedisConnectionFailureException ex) {
        // Log the error
        logger.error("Redis connection failed: {}", ex.getMessage());
        
        // Option 1: Return 503 Service Unavailable
        return new ResponseEntity<>(
            "Service temporarily unavailable (rate limit service down)", 
            HttpStatus.SERVICE_UNAVAILABLE
        );
        
        // Option 2: Implement circuit breaker and use fallback store
        // (requires custom RateLimitStore implementation as shown above)
    }
}
```

---

## Comparison: In-Memory vs. Redis

| Aspect | In-Memory | Redis |
|--------|-----------|-------|
| **Setup Complexity** | None | Requires Redis server |
| **State per Instance** | Isolated | Shared across instances |
| **Latency** | <1ms | 1-5ms per operation |
| **Consistency** | Single-instance only | Distributed (atomic) |
| **Scalability** | Single JVM | Horizontal (unlimited instances) |
| **State Persistence** | Lost on restart | Persists (Redis TTL) |
| **Production Ready** | Single-instance only | Highly available |
| **Error Handling** | N/A | Must handle connection failures |
| **Use Case** | Dev/test, single instance | Microservices, clusters, cloud |

---

## 8. Comparison Check Before Switching

Before switching from in memory to Redis, confirm Redis is reachable, Spring data redis is on classpath and error handlers are configured.


---

## 9. Advanced Extension Points

All internal beans use @ConditionalOnMissingBean. Define your own bean to override default behavior.
    

### Override User Resolution
To change how Custos extracts the User identifier without abandoning `KeyType.USER`:
```java
@Bean
public UserIdResolver customUserIdResolver() {
    return (request) -> {
        // Your custom logic to extract the user ID
        return request.getHeader("X-User-ID");
    };
}
```

### Override IP Resolution
If you have a non-standard reverse proxy setup, you can override IP resolution:
```java
@Bean
public CustosIPResolver customIpResolver() {
    return (request) -> {
        // e.g., Cloudflare specific header
        return request.getHeader("CF-Connecting-IP"); 
    };
}
```

### Implement Custom Storage (e.g., Hazelcast or Memcached)

Implement `RateLimitSTore` to plug into any backend:
- **Hazelcast** (in-process distributed cache)
- **Memcached** (traditional distributed cache)
- **DynamoDB** (AWS managed key-value store)
- **Fallback resilience** (in-memory fallback when Redis fails)

Example - Custom Hazelcast store:

```java
import io.github.abstruse_scientia.custos.core.store.RateLimitStore;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

@Component
public class HazelcastRateLimitStore implements RateLimitStore {
    
    private final IMap<String, Object> rateLimitMap;

    public HazelcastRateLimitStore(HazelcastInstance hazelcast) {
        this.rateLimitMap = hazelcast.getMap("custos-rate-limits");
    }

    @Override
    public Object get(String key) {
        return rateLimitMap.get(key);
    }

    @Override
    public void put(String key, Object state) {
        // Set TTL to 60 seconds (same as Redis default)
        rateLimitMap.put(key, state, 60, TimeUnit.SECONDS);
    }

    @Override
    public Object atomicCompute(String key, BiFunction<String, Object, Object> remappingFunction) {
        return rateLimitMap.compute(key, (k, oldValue) -> 
            remappingFunction.apply(k, oldValue)
        );
    }
}
```
---

## 10. Common Pitfalls

- **Using `KeyType.USER` without Spring Security:** If you use the `USER` key type but do not have Spring Security on the classpath, Custos silently falls back to a `NoOpUserIdResolver`. This means all requests share a single global counter, severely restricting your API.

- **Annotating Private / Internal Methods:** Custos is built on Spring AOP. The `@RateLimit` annotation must be placed on `public` methods, and the method must be called from *outside* the class. Self-invocation inside the same bean bypasses the proxy and the rate limit.

- **Nonsensical Capacity to Rate Ratios:** If your `rate` configuration heavily outpaces your `capacity`, your bucket might replenish faster than users can consume it, effectively turning off the rate limit. Ensure your capacities act as burst buffers and your rates represent true sustained thresholds.

- **Assuming Automatic Redis Failover:** Custos does **NOT** automatically fall back to in-memory limits when Redis fails. When Redis is unreachable, **Spring Data Redis throws an exception that propagates to your application** (HTTP 500). If you need failover behavior, you must implement a custom `RateLimitStore` (see Section 9 for example).

- **Forgetting TTL in Custom Stores:** When implementing custom stores, remember to set TTL (time-to-live) on rate limit entries to prevent memory leaks. Custos respects the 60 second default used by Redis strategies.

---

## 11. Clarifications: What's Real vs. What Requires Custom Implementation

###  Built In 
- In memory rate limiting (thread safe, single instance)
- Redis distributed rate limiting (atomic, multi instance)
- Token Bucket, Leaky Bucket, Sliding Window, Sliding Window Counter algorithms
- Key resolution by USER (Spring Security), IP, or CUSTOM
- Exception handling via `@ControllerAdvice`

### ️ Requires Custom Implementation
- **Automatic Redis fallback to in memory**:  Not built in. Must create custom `RateLimitStore`.
- **Circuit breaker for Redis failures** :Not built in. Use Resilience4j or custom logic.
- **Custom distributed stores** (Hazelcast, DynamoDB, Memcached) : Not built in. Must implement `RateLimitStore`.
- **Multi-algorithm rate limiting per endpoint** : Not supported. Use separate endpoints or custom resolver.

###  Not Supported
- Automatic store switching at runtime (memory <-> Redis)
- Rate limiting based on request body content
- Dynamic rate limits (changing limits without restart)
- Token persistence across application restarts with in memory store


