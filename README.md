# Custos

Distributed, native Spring Boot rate limiting library. Zero Config. Zero Boilerplate.


[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://img.shields.io/badge/Maven--Central-1.0.0-brightgreen.svg)](https://search.maven.org/artifact/io.github.abstruse-scientia/custos)



#### Get dependency
Custos is distributed through [Maven Central](https://search.maven.org/):

##### Java 17 dependency
```xml
<!-- For java 17+ -->
<dependency>
  <groupId>io.github.abstruse-scientia</groupId>
  <artifactId>custos</artifactId>
  <version>1.0.0</version>
</dependency>
```

##### Java 8 dependencies
Custos does not support Java 8. It is designed optimally for modern Spring Boot applications, leveraging modern Java features, and thus strictly requires **Java 17 or higher**.

#### Quick start
Custos removes the need for programmatic builder patterns entirely. Just add the `@RateLimit` annotation to any Spring method, and it is automatically protected.

```java
import io.github.abstruse_scientia.custos.annotations.RateLimit;
import io.github.abstruse_scientia.custos.core.model.Algorithm;
import io.github.abstruse_scientia.custos.resolver.KeyType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiController {

    // Rate limits to 20 requests per user, refilling 1 token every 6 seconds (rate = 0.16)
    @GetMapping("/api/protected")
    @RateLimit(
        keytype = KeyType.USER, 
        algorithm = Algorithm.TOKEN_BUCKET, 
        capacity = 20, 
        rate = 0.16
    )
    public String doSomethingProtected() {
        // If the limit is exceeded, Custos automatically throws a RateLimitExceededException
        // which you can catch using a global @ControllerAdvice to return HTTP 429.
        return "Protected data accessed successfully!";
    }
}
```

More examples [can be found in the examples directory](https://github.com/abstruse-scientia/Custos-Distributed-Rate-Limiter/tree/main/examples).

## [Documentation](https://github.com/abstruse-scientia/Custos-Distributed-Rate-Limiter/tree/main/docs)
* [Architecture & Flow](docs/ARCHITECTURE_AND_FLOW.md)
* [Algorithms Explained](docs/ALGORITHMS_EXPLAINED.md)
* [Integration Guides](docs/INTEGRATION_GUIDES.md)

## Custos basic features
* *Zero Boilerplate* - Custos removes the need for programmatic DSLs. Rate limiting is applied effortlessly using the `@RateLimit` annotation directly on your controllers or service methods.
* *Pluggable Key Resolution* - Out of the box support for rate limiting by `USER` (via Spring Security Principal) or `IP` (via HTTP request) with the ability to inject custom extraction logic for either.
* *Effective implementation in terms of concurrency*: Default in-memory stores use highly concurrent, thread-safe data structures (`ConcurrentHashMap`) for robust scaling in single-instance deployments without locking bottlenecks.
* *Multiple Algorithm Strategies*: Supports Token Bucket, Sliding Window, and Leaky Bucket algorithms natively. You can also deploy custom strategies cleanly via Spring's `@Component`.
* *Graceful Exception Handling*: Automatically throws a dedicated `RateLimitExceededException` that includes metadata (like retry-after), allowing clean translation to HTTP 429 responses via standard `@ControllerAdvice`.

## Custos distributed features
In addition to  basic features described above, `Custos` provides the ability to implement rate-limiting in a cluster of JVMs:
- *Native Redis Support* - No need for JCache wrappers or extensive middleware. By simply setting `custos.store=redis` in your `application.yml`, Custos acts as a cluster-ready distributed store.
- *Atomic Lua Scripting* - Distributed rate limiting relies on Lua scripts executed atomically within Redis. This completely prevents Time-of-Check to Time-of-Use (TOCTOU) race conditions in high-throughput horizontal deployments.
- *Fail-Closed By Default* - If Redis becomes unavailable , Spring Data will throw connection exception (for example: `RedisConnectionFailureException`) which will propagate uncaught through Custos, preventing silent limit bypasses. You should catch these exceptions in a @ControllerAdvice.

## Contributing
We welcome contributions! Please see our [Contributing Guide](docs/CONTRIBUTING_GUIDE.md) for details on how to get started, set up your local environment, and submit pull requests.

## Have a question?
Feel free to ask via:
* [Custos github issue tracker](https://github.com/abstruse-scientia/Custos-Distributed-Rate-Limiter/issues/new) to report a bug.
* [Custos github discussions](https://github.com/abstruse-scientia/Custos-Distributed-Rate-Limiter/discussions) for questions, feature proposals, sharing of experience.

## License
Licensed under the Apache Software License, Version 2.0: <http://www.apache.org/licenses/LICENSE-2.0>.
