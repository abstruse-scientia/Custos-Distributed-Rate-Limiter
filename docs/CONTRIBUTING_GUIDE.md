# Contributing to Custos

## Asking Questions

Use GitHub Discussions for questions about usage, architecture, or rate limiting concepts. Use GitHub Issues only for confirmed bugs or concrete feature proposals.

Link: https://github.com/abstruse-scientia/Custos-Distributed-Rate-Limiter/discussions

## Reporting Bugs

Search existing issues before filing a new one. Don't make us read duplicates.

When reporting a bug, provide the following:

- Custos version
- Java version
- Spring Boot version
- Minimal reproduction case (preferably a JUnit test)
- Expected behavior
- Actual behavior
- Full stack trace

Issues without sufficient detail will be closed.

## Proposing Features

Submit an issue before writing any code. For major changes, we need to talk it out in an issue and wait for  feedback first.

Custos won't accept features that break the `@ConditionalOnMissingBean` overridability pattern. See [Architecture and Flow](ARCHITECTURE_AND_FLOW.md) for details.

New rate limiting algorithms must include both in-memory and Redis implementations. Redis implementations require a corresponding Lua script in `src/main/resources/scripts/`.

## Development Environment Setup

### Prerequisites

- Java 17 or higher
- Maven 3.8 or higher
- Docker (required for integration tests)

### Initial Setup

Fork and clone the repository:

```bash
git clone https://github.com/YOUR-USERNAME/Custos-Distributed-Rate-Limiter.git
cd Custos-Distributed-Rate-Limiter
```

### Running Tests

Run unit tests:

```bash
mvn test -DskipITs=true -Dmaven.javadoc.skip=true -Dmaven.source.skip=true -nsu
```

Run integration tests:

```bash
mvn verify -DskipTests
```

Integration tests require Docker and Testcontainers. Redis will be started automatically by Testcontainers before test execution.

## Submitting a Pull Request

Follow this process:

1. Create a branch from `main` using the naming convention: `feature/*` for new features, `fix/*` for bug fixes. Example: `feature/sliding-window-counter-redis` or `fix/token-bucket-concurrency`.

2. Write code following the guidelines in the Code Standards section.

3. Add unit tests for all new code. We won't merge new strategy implementations if they don't include integration tests.

4. Run both test suites locally and verify they pass:
   ```bash
   mvn test -DskipITs=true -Dmaven.javadoc.skip=true -Dmaven.source.skip=true -nsu
   mvn verify -DskipTests
   ```

5. Don't modify the version in `pom.xml`. Version increments are handled by maintainers during release.

6. Use this commit message format: `Issue #<number>: <description>`. Example: `Issue #42: Add Sliding Window Counter Redis strategy`.

7. Push and open a pull request against the `main` branch.

All pull requests must pass both CI jobs (unit tests and integration tests) before merging.

## Code Standards

### Package Structure

Don't introduce new top-level packages without prior issue discussion. We like the existing structure:

- `annotations/` €” `@RateLimit` annotation
- `aop/` €” `RateLimitAspect` (AOP interception)
- `core/engine/` €” `RateLimiterEngine`
- `core/strategy/` €” In-memory strategy implementations
- `core/strategy/redis/` €” Redis strategy implementations
- `core/store/` €” `RateLimitStore` interface and `InMemoryStore`
- `core/config/` €” Autoconfiguration and properties
- `resolver/` €” `KeyResolver` implementations
- `utility/` €” `UserIdResolver` and `CustosIPResolver` implementations
- `resources/scripts/` €” Lua scripts for Redis strategies

See [Architecture and Flow](ARCHITECTURE_AND_FLOW.md) for component contracts.

### Implementing Rate Limiting Strategies

New `RateLimiterStrategy` implementations must:

- Implement `getAlgorithm()` returning the correct `Algorithm` enum value
- Be registered as a Spring `@Bean` in `CustosAutoConfiguration`
- Use `@ConditionalOnProperty` to select between in-memory and Redis variants
- For Redis implementations: create a corresponding Lua script in `src/main/resources/scripts/` following the naming pattern `LUA_SCRIPT_<ALGORITHM_NAME>.lua`

### Implementing Key Resolvers

New `KeyResolver` implementations must:

- Implement the `KeyResolver` interface
- Handle null and missing request values gracefully
- Default to a reasonable fallback identifier if extraction fails

### Dependencies

Don't add dependencies to `pom.xml` without prior issue discussion. We want leaner builds, so dependency additions require justification and maintainer approval.

## License

Contributions are licensed under the Apache License 2.0. Submitting a pull request constitutes acceptance of this license.

For details, see LICENSE at the repository root.

## References

- Repository: https://github.com/abstruse-scientia/Custos-Distributed-Rate-Limiter
- Issue Tracker: https://github.com/abstruse-scientia/Custos-Distributed-Rate-Limiter/issues
- Discussions: https://github.com/abstruse-scientia/Custos-Distributed-Rate-Limiter/discussions

