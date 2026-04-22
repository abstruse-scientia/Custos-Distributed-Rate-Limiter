package io.github.abstruse_scientia.custos.core.strategy;

import io.github.abstruse_scientia.custos.core.config.RateLimitConfig;
import io.github.abstruse_scientia.custos.core.model.RateLimitDecision;
import io.github.abstruse_scientia.custos.core.store.InMemoryStore;
import io.github.abstruse_scientia.custos.core.store.RateLimitStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Leaky Bucket Rate Limiting Strategy
 * 1. testRequestQueueing: Validates capacity filling behaves properly.
 * 2. testLeakRateOverTime: Validates requests leak freeing up bucket space.
 * 3. testPerUserBuckets: Independent bucket tracking for multiple users.
 * 4. testQueueCapacityExceeded: Rejection when passing capacity.
 */
public class LeakyBucketStrategyTest {

    private LeakyBucketStrategy strategy;
    private RateLimitStore store;
    private RateLimitConfig config;

    @BeforeEach
    public void setup() {
        strategy = new LeakyBucketStrategy();
        store = new InMemoryStore();
    }

    @Test
    public void testRequestQueueing() {

        config = new RateLimitConfig(3, 0.0);
        String userId = "user1";

        for (int i = 0; i < 3; i++) {
            RateLimitDecision decision = strategy.allow(userId, config, store);
            assertThat(decision.allow())
                .as("Request %d should be accepted", i + 1)
                .isTrue();
        }

        RateLimitDecision decision4 = strategy.allow(userId, config, store);
        assertThat(decision4.allow())
            .as("4th request should be rejected")
            .isFalse();

        RateLimitDecision decision5 = strategy.allow(userId, config, store);
        assertThat(decision5.allow())
            .as("5th request should be rejected")
            .isFalse();

        assertThat(decision4.retryAfterSeconds())
            .as("Rejected request should have positive retry-after time")
            .isGreaterThan(0);
    }


    @Test
    public void testLeakRateOverTime() throws InterruptedException {
        config = new RateLimitConfig(3, 1.0);
        String userId = "user1";
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 3; i++) {
            RateLimitDecision decision = strategy.allow(userId, config, store);
            assertThat(decision.allow())
                .as("Request %d should be accepted", i + 1)
                .isTrue();
        }

        RateLimitDecision rejectedDecision = strategy.allow(userId, config, store);
        assertThat(rejectedDecision.allow())
            .as("4th request should be rejected")
            .isFalse();

        long timeToWait = rejectedDecision.retryAfterSeconds() + 1;

        Thread.sleep(1100);

        RateLimitDecision afterLeakDecision1 = strategy.allow(userId, config, store);
        assertThat(afterLeakDecision1.allow())
            .as("Request after leak should be allowed")
            .isTrue();

        Thread.sleep(1000);

        RateLimitDecision afterLeakDecision2 = strategy.allow(userId, config, store);
        assertThat(afterLeakDecision2.allow())
            .as("Second request after leak should be allowed")
            .isTrue();
    }

    @Test
    public void testPerUserBuckets() {
        config = new RateLimitConfig(3, 0.0);
        String user1 = "user1";
        String user2 = "user2";

        for (int i = 0; i < 3; i++) {
            RateLimitDecision decision = strategy.allow(user1, config, store);
            assertThat(decision.allow())
                .as("User1 request %d should be accepted", i + 1)
                .isTrue();
        }

        RateLimitDecision user1Rejected = strategy.allow(user1, config, store);
        assertThat(user1Rejected.allow())
            .as("User1 4th request should be rejected")
            .isFalse();

        for (int i = 0; i < 3; i++) {
            RateLimitDecision decision = strategy.allow(user2, config, store);
            assertThat(decision.allow())
                .as("User2 request %d should be accepted", i + 1)
                .isTrue();
        }

        RateLimitDecision user2Rejected = strategy.allow(user2, config, store);
        assertThat(user2Rejected.allow())
            .as("User2 4th request should be rejected")
            .isFalse();
    }


    @Test
    public void testQueueCapacityExceeded() {
        config = new RateLimitConfig(3, 0.0);
        String userId = "user1";

        for (int i = 0; i < 3; i++) {
            RateLimitDecision decision = strategy.allow(userId, config, store);
            assertThat(decision.allow())
                .as("Request %d should be accepted", i + 1)
                .isTrue();
        }

        int rejectedCount = 0;
        for (int i = 0; i < 10; i++) {
            RateLimitDecision decision = strategy.allow(userId, config, store);
            if (!decision.allow()) {
                rejectedCount++;
            }
        }

        assertThat(rejectedCount)
            .as("Requests should be rejected when queue is full")
            .isGreaterThan(0);

        RateLimitDecision rejectedDecision = strategy.allow(userId, config, store);
        assertThat(rejectedDecision.retryAfterSeconds())
            .as("Rejected request should provide retry-after time")
            .isGreaterThan(0);
    }
}

