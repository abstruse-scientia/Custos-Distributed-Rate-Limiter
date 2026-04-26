package io.github.abstruse_scientia.custos.integration.store;

import io.github.abstruse_scientia.custos.core.config.RateLimitConfig;
import io.github.abstruse_scientia.custos.core.model.Algorithm;
import io.github.abstruse_scientia.custos.core.model.RateLimitDecision;
import io.github.abstruse_scientia.custos.core.store.RateLimitStore;
import io.github.abstruse_scientia.custos.core.strategy.RateLimiterStrategy;
import io.github.abstruse_scientia.custos.core.strategy.StrategyFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test for Store Failover.
 * <p>Configured for Redis, but Redis unavailable.</p>
 * <p>Verify: System gracefully raises appropriate error.</p>
 */
@SpringBootTest(properties = {
        "custos.store=redis"
})
@Testcontainers
public class StoreFailoverTest {

    @Container
    @ServiceConnection
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Autowired
    private StrategyFactory strategyFactory;

    @Autowired
    private RateLimitStore rateLimitStore;

    /**
     * Test: Store Failover (Memory -> Redis or vice versa)
     * <p>Setup: Configured for Redis, but Redis unavailable</p>
     * <p>Verify: System raises appropriate error</p>
     */
    @Test
    public void testStoreFailover() {
        RateLimitConfig config = new RateLimitConfig(10.0, 1.0);
        String userId = "failoverUser";

        RateLimiterStrategy rateLimiterStrategy = strategyFactory.get(Algorithm.TOKEN_BUCKET);

        // Initial check: Redis is accessible
        RateLimitDecision decision = rateLimiterStrategy.allow(userId, config, rateLimitStore);
        assertThat(decision).isNotNull();
        assertThat(decision.allow()).isTrue();

        // Simulate Redis outage
        redis.stop();

        // Verify appropriate exception is raised
        assertThatThrownBy(() -> rateLimiterStrategy.allow(userId, config, rateLimitStore))
                .isInstanceOf(Exception.class);
    }
}
