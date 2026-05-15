# Internal Architecture & Request Flow

This documentation details the internal architecture, component design and request lifecycle of **Custos**. It is written with intention to provide key insight to core contributors, maintainers, and nerds who want to understand the internal mechanics of the library. 

## 1. Overview

Custos is a Spring Boot auto configured rate limiting library. Architecturally, it relies on three foundational patterns:
- **Aspect-Oriented Programming (AOP)**: Intercepts requests  at the method level.
- **Strategy Pattern**: Provides interchangeable rate limiting algorithms per request.
- **Abstract Data Store**: Decouples limit evaluation from state persistence, enabling deployment in both local (in-memory) and distributed (Redis) environments.

The library can default to an in-memory mode which is highly concurrent and thread safe, but can switch seamlessly to distributed mode when configured for Redis.

## 2. The `@RateLimit` Annotation

The `@RateLimit` annotation acts as the primary configuration contract between the user's code and the Custos engine. It contains the following properties that govern internal execution:

- `keytype` (`KeyType`): Determines which `KeyResolver` will be used (e.g., `USER`, `IP`).
- `algorithm` (`Algorithm`): Acts as the routing key for the `StrategyFactory` to select the correct `RateLimiterStrategy`.
- `capacity` (`int`): Defines the maximum burst capacity or bucket size.
- `rate` (`double`): Defines the refill rate or window limit depending on the chosen algorithm.

These values are locally scoped to the annotated method but are subsequently merged with global defaults by the `ConfigResolver`.

## 3. Request Interception Flow

The lifecycle of a rate-limited request follows a strict pipeline from interception to execution.

```text
Incoming HTTP Request
         |
         v
[ Annotated Target Method ]
         | (Intercepted by)
         v
+-----------------------+
|  RateLimitAspect      |
+-----------------------+
         | 1. Intercepts method
         | 2. Builds RequestContext(userId, ipAddress)
         | 3. Extracts @RateLimit annotation
         v
+-----------------------+
|  RateLimiterEngine    | <--- Orchestrates the limit evaluation pipeline
+-----------------------+
         |
         |---> [ KeyResolverFactory ] ----> Resolves string key (e.g., "user:123")
         |
         |---> [ ConfigResolver ] --------> Merges annotation + CustosProperties -> RateLimitConfig
         |
         |---> [ StrategyFactory ] -------> Selects RateLimiterStrategy based on Algorithm
         |
         |---> [ RateLimiterStrategy ] ---> Evaluates limit against RateLimitStore
                     |
                     v
         [ RateLimitDecision(allow, retryAfterSeconds) ]
                     |
+--------------------+--------------------+
|                                         |
v (Denied)                                v (Allowed)
Throws RateLimitExceededException         Proceeds to Method Invocation
(caught globally by consumer)             (Returns result to caller)
```

## 4. Core Engine Pipeline

The `RateLimiterEngine` acts as the orchestrator. Its `allow()` method performs four sequential steps to evaluate a request:

1. **Key Resolution**: Delegates to the `KeyResolverFactory` to determine the unique string identifier for the limit counter based on the annotation's `KeyType` and the current `RequestContext`.
2. **Config Resolution**: Passes the annotation metadata to the `ConfigResolver`. The resolver merges method-level properties with global defaults defined in `CustosProperties` to produce a finalized `RateLimitConfig` object.
3. **Strategy Selection**: Uses the `Algorithm` enum from the annotation to fetch the corresponding implementation of `RateLimiterStrategy` via the `StrategyFactory`.
4. **Decision Evaluation**: Invokes `allow(key, config, store)` on the selected strategy. The strategy interacts with the `RateLimitStore` and returns a `RateLimitDecision`.

## 5. Store Layer

The `RateLimitStore` interface provides a strict contract for state management, completely decoupling algorithm logic from the storage mechanism.

**Interface Contract:**
- `get(key)`: Retrieves current state.
- `put(key, state)`: Persists state.
- `atomicCompute(key, remappingFunction)`: Atomically updates state based on current values.

**In-Memory implementation:**
The default `InMemoryStore` is backed by a `ConcurrentHashMap`. Crucially, to prevent race conditions in highly concurrent local environments, it utilizes `ConcurrentHashMap.compute()`. This lock free approach ensures that the state modification function runs atomically per key without requiring global synchronization method.

*Note: The store layer is fully extensible; users can provide custom implementations by registering a bean of type `RateLimitStore`.*

## 6. Redis Distributed Mode & Lua Atomicity

When configured for horizontal scaling, Custos switches from manipulating local `State Models` to executing remote scripts via `StringRedisTemplate`.

| Algorithm | In-Memory Strategy | Redis Strategy | State Model / Script |
|-----------|--------------------|----------------|----------------------|
| Token Bucket | `TokenBucketStrategy` | `RedisTokenBucketStrategy` | `BucketState` / `LUA_SCRIPT_TOKEN_BUCKET.lua` |
| Sliding Window Log | `SlidingWindowStrategy` | `RedisSlidingWindowStrategy` | `SlidingWindowState` / `LUA_SCRIPT_SLIDING_WINDOW.lua` |
| Leaky Bucket | `LeakyBucketStrategy` | `RedisLeakyBucketStrategy` | `LeakyBucketState` / `LUA_SCRIPT_LEAKY_BUCKET.lua` |
| Window Counter | `SlidingWindowCounterStrategy` | `RedisSlidingWindowCounterStrategy` | `SlidingWindowCounterState` / `LUA_SCRIPT_SLIDING_WINDOW_COUNTER.lua` |

### TOCTOU Prevention Pipeline

In a distributed environment, evaluating a limit requires reading state, applying logic (e.g., refilling tokens), and writing state back. If executed sequentially as individual Redis commands, interleaved requests from different JVMs cause Time-of-Check to Time-of-Use (TOCTOU) race conditions, resulting in over allocation.

To natively solve this, Custos embeds the algorithmic logic inside Redis using atomic Lua scripts. 
- The `Redis Strategy` classes do **not** pull state into the JVM.
- Instead, they pass `RateLimitConfig` parameters as `ARGV` and the key as `KEYS` into `StringRedisTemplate.execute()`.
- Redis ensures Lua scripts execute fully atomically, guaranteeing thread-safe limits across a distributed cluster.

*(For detailed algorithm math, refer to [ALGORITHMS_EXPLAINED.md](ALGORITHMS_EXPLAINED.md)).*

## 7. Auto-Configuration & Conditional Wiring

Custos relies entirely on Spring Boot's auto-configuration mechanics. `CustosAutoConfiguration` is registered via the `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` file.

**Dependency Graph & Conditions:**

```text
CustosAutoConfiguration
|
+-- @ConditionalOnMissingBean
|   |-- CustosProperties
|   |-- CustosMainProperties
|   |-- ConfigResolver
|   |-- RateLimiterEngine
|   |-- RateLimitAspect
|
+-- Key Resolvers (@ConditionalOnMissingBean)
|   |-- IPKeyResolver
|   |-- UserKeyResolver
|   |-- KeyResolverFactory
|
+-- Store Topology
        |-- @ConditionalOnProperty(name="custos.store", havingValue="redis", matchIfMissing=false)
        |   |-- RedisStrategy implementations (x4) (these use StringRedisTemplate directly)
    |
    |-- @ConditionalOnMissingBean(RateLimitStore.class) -> Fallback
        |-- InMemoryStore
        |-- In-Memory Strategy implementations (x4)
``` 

By heavily utilizing `@ConditionalOnMissingBean`, Custos guarantees that every internal component from resolution to strategy selection is fully overridable by the consumer.

The security components dynamically adapt based on the classpath. The `UserIdResolver` defaults to `NoOpUserIdResolver`, but seamlessly promotes itself to `SpringSecurityUserIdResolver` via `@ConditionalOnClass` if Spring Security classes are detected.

## 8. Key Resolution Architecture

The `KeyResolverFactory` maintains a registry of `KeyResolver` implementations. When invoked, it looks up the appropriate resolver based on the `KeyType` enum.

- **`UserKeyResolver`**: Delegates to the active `UserIdResolver`. Extracts the primary identifier (typically an authenticated principal).
- **`IPKeyResolver`**: Delegates to the `CustosIPResolver`. By default, `DefaultCustosIPResolver` interrogates the HTTP request, walking down a strict priority chain to penetrate intermediate proxies:
  1. `X-Forwarded-For` header
  2. `X-Real-IP` header
  3. `ServletRequest.getRemoteAddr()`

- **Custom Resolution**: Consumers can provide custom extraction logic by registering their own custom resolver beans for `UserIdResolver` or `CustosIPResolver` if they need to retrieve user or IP data from other sources.

## 9. Exception Flow

When an algorithm determines a request violates the configured thresholds, the `RateLimiterStrategy` returns a `RateLimitDecision` where `allow = false`.

The `RateLimitAspect` evaluates this decision immediately. Rather than returning a modified HTTP response directly (which would violate framework boundaries and AOP constraints), it intentionally throws a `RateLimitExceededException`. 

This runtime exception carries contextual metadata:
- `key` (`String`): The identifier that exceeded the limit.
- `retryAfterSeconds` (`long`): The algorithm-calculated duration until the next request is permitted.

The exception halts the AOP chain, skipping the target controller invocation. It propagates to the application context, where the consumer manages it (e.g., via global configuration; see [INTEGRATION_GUIDES.md](INTEGRATION_GUIDES.md) for patterns).

## See Also
* [Algorithms Explained](ALGORITHMS_EXPLAINED.md)
* [Integration Guides](INTEGRATION_GUIDES.md)
* [Contributing Guide](CONTRIBUTING_GUIDE.md)
