# Custos

Distributed, native Spring Boot rate limiting library. All you have to do is add an annotation to your controller method.


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

##### Gradle
```gradle
implementation 'io.github.abstruse-scientia:custos:1.0.0'
```

##### Java 8 dependencies
Custos doesn't support Java 8. It's built for modern Spring Boot applications using modern Java features, so it strictly requires **Java 17 or higher**. Frankly, if you're still on Java 8, rate limiting isn't your biggest problem right now.

#### Quick start
Add @Ratelimit to any spring method.Done.
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

## Custos features
* *Zero Boilerplate* - Nobody likes writing DSL configuration. You can apply rate limiting perfectly by using the `@RateLimit` annotation directly on your controllers or service methods.
* *Pluggable Key Resolution* - Out of the box, there is support rate limiting by `USER` (via Spring Security Principal) or `IP` (via HTTP request). You'll also get the ability to inject custom extraction logic for either.
* *Effective implementation in terms of concurrency*: Default in memory stores rely on thread-safe data structures like `ConcurrentHashMap` for single-instance deployments without locking bottlenecks. 
* *Multiple Algorithm Strategies*: Supports Token Bucket, Sliding Window, and Leaky Bucket algorithms natively. You can also deploy custom strategies cleanly via Spring's `@Component`.
* *Graceful Exception Handling*: Custos automatically throws a dedicated `RateLimitExceededException` that includes metadata like retry after.

## Custos distributed features
Besides the standard features, `Custos` gives you the ability to implement rate-limiting in a cluster of JVMs. Distributed state shouldn't give you a headache. 
- *Native Redis Support* - No need for JCache wrappers or extensive middleware. By simply setting `custos.store=redis` in your `application.yml`, Custos acts as a cluster-ready distributed store.
- *Atomic Lua Scripting* - Distributed rate limiting relies on Lua scripts executed atomically within Redis. Check-and-set loops are for amateurs, so we completely prevent Time-of-Check to Time-of-Use (TOCTOU) race conditions in high-throughput horizontal deployments.
- *Fail-Closed By Default* - If Redis becomes unavailable, Spring Data will throw a connection exception like `RedisConnectionFailureException`. This forces your hand to catch these exceptions in a @ControllerAdvice so you won't silently bypass limits.

## Contributing
Contributions are welcome. See the [Contributing Guide](docs/CONTRIBUTING_GUIDE.md) for details on how to get started, set up your local environment, and submit pull requests.

## Have a question?
Feel free to ask via:
* [Custos github issue tracker](https://github.com/abstruse-scientia/Custos-Distributed-Rate-Limiter/issues/new) to report a bug.
* [Custos github discussions](https://github.com/abstruse-scientia/Custos-Distributed-Rate-Limiter/discussions) for questions, feature proposals, sharing of experience.

## License
Licensed under the Apache Software License, Version 2.0: <http://www.apache.org/licenses/LICENSE-2.0>.
