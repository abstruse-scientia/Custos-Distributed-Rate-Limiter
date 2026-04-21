package io.github.abstruse_scientia.custos.core.strategy;

import io.github.abstruse_scientia.custos.core.config.RateLimitConfig;
import io.github.abstruse_scientia.custos.core.model.BucketState;
import io.github.abstruse_scientia.custos.core.model.RateLimitDecision;
import io.github.abstruse_scientia.custos.core.store.InMemoryStore;
import io.github.abstruse_scientia.custos.core.store.RateLimitStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for Token Bucket Rate Limiting Strategy
 *
 * Implements Category 1 - Core Algorithm Tests 1-4:
 * 1. testBasicTokenConsumption: Verifies basic token consumption
 * 2. testBucketRefillOverTime: Verifies bucket refills tokens over time
 * 3. testPerUserIsolation: Confirms rate limiting is per-user, not global
 * 4. testCapacityOverflowPrevention: Ensures tokens don't exceed capacity
 */
public class TokenBucketStrategyTest {

    private TokenBucketStrategy strategy;
    private RateLimitStore store;
    private RateLimitConfig config;

    @BeforeEach
    public void setup() {
        strategy = new TokenBucketStrategy();
        store = new InMemoryStore();
        config = mock(RateLimitConfig.class);
    }

    /**
     * Test 1: Basic Token Consumption
     * Setup: Bucket with 10 token capacity, refill rate = 0 (no refill)
     * Verify: First 10 requests pass, 11th request is rejected
     */
    @Test
    public void testBasicTokenConsumption() {
        when(config.getCapacity()).thenReturn(10.0);
        when(config.getRate()).thenReturn(0.0);

        String userId = "user1";
        for (int i = 0; i < 10; i++) {
            assertThat(strategy.allow(userId, config, store).allow()).as(
                    "Request " + (i + 1) + " should be allowed"
            ).isTrue();
        }
        RateLimitDecision rejectedDecision = strategy.allow(userId, config, store);
        assertThat(rejectedDecision.allow()).isFalse();

    }

    /**
     * Test 2: Bucket Refill Over Time
     * Setup: Initial capacity, completely consume, wait for refill 
     * Verify: Bucket correctly accounts for elapsed time to refill tokens
     */
    @Test
    public void testBucketRefillOverTime() throws InterruptedException {
        when(config.getCapacity()).thenReturn(10.0);
        when(config.getRate()).thenReturn(0.5);

        String userId = "user1";
        
        for(int i = 0; i < 10; i++) {
            strategy.allow(userId, config, store);
        }

        RateLimitDecision initialDecision = strategy.allow(userId, config, store);
        assertThat(initialDecision.allow())
            .as("First request with empty bucket should be rejected")
            .isFalse();

        Thread.sleep(4000);

        RateLimitDecision decision1 = strategy.allow(userId, config, store);
        assertThat(decision1.allow())
            .as("First request after refill should be allowed")
            .isTrue();

        RateLimitDecision decision2 = strategy.allow(userId, config, store);
        assertThat(decision2.allow())
            .as("Second request after refill should be allowed")
            .isTrue();
    }

    /**
     * Test 3: Per-User Isolation
     * Setup: Two users, separate limits
     * Verify: User1 consuming full allocation does not impact user2 capacity.
     */
    @Test
    public void testPerUserIsolation() {
        when(config.getCapacity()).thenReturn(5.0);
        when(config.getRate()).thenReturn(0.0);

        String user1 = "user1";
        String user2 = "user2";

        for (int i = 0; i < 5; i++) {
            RateLimitDecision decision = strategy.allow(user1, config, store);
            assertThat(decision.allow())
                .as("User1 request %d should be allowed", i + 1)
                .isTrue();
        }

        RateLimitDecision user1Rejected = strategy.allow(user1, config, store);
        assertThat(user1Rejected.allow())
            .as("User1 6th request should be rejected")
            .isFalse();

        for (int i = 0; i < 5; i++) {
            RateLimitDecision decision = strategy.allow(user2, config, store);
            assertThat(decision.allow())
                .as("User2 request %d should be allowed (independent from user1)", i + 1)
                .isTrue();
        }
    }

    /**
     * Test 4: Capacity Overflow Prevention
     * Setup: Capacity = 10, current tokens = 8, refill adds 5 tokens
     * Verify: Tokens don't exceed capacity (stay at 10, not 13)
     */
    @Test
    public void testCapacityOverflowPrevention() throws InterruptedException {
        when(config.getCapacity()).thenReturn(10.0);
        when(config.getRate()).thenReturn(10.0);

        String userId = "user1";
        long initialTime = System.currentTimeMillis();

        BucketState bucketState = new BucketState(8.0, initialTime);
        store.put(userId, bucketState);

        Thread.sleep(1000);

        RateLimitDecision decision = strategy.allow(userId, config, store);

        BucketState refillState = (BucketState) store.get(userId);

        assertThat(refillState.getTokens())
            .as("Bucket tokens should not exceed capacity")
            .isLessThanOrEqualTo(10.0);
    }
}
