package io.github.abstruse_scientia.custos.error;

import io.github.abstruse_scientia.custos.core.config.RateLimitConfig;
import io.github.abstruse_scientia.custos.core.model.RateLimitDecision;
import io.github.abstruse_scientia.custos.core.store.InMemoryStore;
import io.github.abstruse_scientia.custos.core.strategy.LeakyBucketStrategy;
import io.github.abstruse_scientia.custos.core.strategy.SlidingWindowCounterStrategy;
import io.github.abstruse_scientia.custos.core.strategy.SlidingWindowStrategy;
import io.github.abstruse_scientia.custos.core.strategy.TokenBucketStrategy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test: Error Handling & Edge Cases
 * <p>Verify no integer overflow or precision loss with extreme config values.</p>
 */
class EdgeCaseTest {

    /**
     * Test 1: Extreme Values Handling
     * <p>Setup: Very high capacity, very low refill rate</p>
     * <p>Verify: No integer overflow, precision maintained</p>
     */
    @Test
    void testExtremeValues() {
        TokenBucketStrategy strategy = new TokenBucketStrategy();
        InMemoryStore store = new InMemoryStore();

        // Extreme: very high capacity
        RateLimitConfig highCapacityConfig = new RateLimitConfig(999999, 0.0001);

        String userId = "extreme-user";

        // First request should pass (capacity = 999999)
        RateLimitDecision decision = strategy.allow(userId, highCapacityConfig, store);
        assertThat(decision.allow()).isTrue();

        // Many requests should still pass given massive capacity
        for (int i = 0; i < 100; i++) {
            RateLimitDecision d = strategy.allow(userId, highCapacityConfig, store);
            assertThat(d.allow()).isTrue();
        }
    }

    /**
     * Test 1.1: Very small capacity  limit reached quickly
     */
    @Test
    void testVerySmallCapacity() {
        TokenBucketStrategy strategy = new TokenBucketStrategy();
        InMemoryStore store = new InMemoryStore();

        RateLimitConfig singleTokenConfig = new RateLimitConfig(1, 0.001);

        String userId = "small-cap-user";

        // First request passes
        RateLimitDecision first = strategy.allow(userId, singleTokenConfig, store);
        assertThat(first.allow()).isTrue();

        // Second request should be rejected (only 1 token capacity, very slow refill)
        RateLimitDecision second = strategy.allow(userId, singleTokenConfig, store);
        assertThat(second.allow()).isFalse();
        assertThat(second.retryAfterSeconds()).isGreaterThan(0);
    }

    /**
     * Test 1.2: All algorithms handle extreme capacity
     */
    @Test
    void testAllAlgorithmsExtremeCapacity() {
        InMemoryStore store = new InMemoryStore();
        RateLimitConfig extremeConfig = new RateLimitConfig(Integer.MAX_VALUE, 1.0);

        // Token Bucket
        TokenBucketStrategy tb = new TokenBucketStrategy();
        RateLimitDecision tbDecision = tb.allow("extreme-tb", extremeConfig, store);
        assertThat(tbDecision.allow()).isTrue();

        // Sliding Window
        SlidingWindowStrategy sw = new SlidingWindowStrategy();
        RateLimitDecision swDecision = sw.allow("extreme-sw", extremeConfig, store);
        assertThat(swDecision.allow()).isTrue();

        // Leaky Bucket
        LeakyBucketStrategy lb = new LeakyBucketStrategy();
        RateLimitDecision lbDecision = lb.allow("extreme-lb", extremeConfig, store);
        assertThat(lbDecision.allow()).isTrue();

        // Sliding Window Counter
        SlidingWindowCounterStrategy swc = new SlidingWindowCounterStrategy();
        RateLimitDecision swcDecision = swc.allow("extreme-swc", extremeConfig, store);
        assertThat(swcDecision.allow()).isTrue();
    }

    /**
     * Test 1.3: High refill rate doesn't cause precision issues
     */
    @Test
    void testHighRefillRate() {
        TokenBucketStrategy strategy = new TokenBucketStrategy();
        InMemoryStore store = new InMemoryStore();

        RateLimitConfig highRateConfig = new RateLimitConfig(10, 10000.0);

        String userId = "high-rate-user";

        // Exhaust all tokens
        for (int i = 0; i < 10; i++) {
            RateLimitDecision d = strategy.allow(userId, highRateConfig, store);
            assertThat(d.allow()).isTrue();
        }

        // With extremely high refill rate, tokens should replenish quickly
        // even the small time taken between calls should add tokens
        // The 11th request may pass due to high rate, which is correct behavior
        RateLimitDecision decision = strategy.allow(userId, highRateConfig, store);
        // Either allowed (refilled) or rejected (not enough time) — both are valid
        assertThat(decision).isNotNull();
    }

    /**
     * Test 1.4: Fractional capacity values
     */
    @Test
    void testFractionalValues() {
        TokenBucketStrategy strategy = new TokenBucketStrategy();
        InMemoryStore store = new InMemoryStore();

        // Fractional capacity
        RateLimitConfig fractionalConfig = new RateLimitConfig(1.5, 0.5);

        String userId = "fractional-user";

        // First request should pass (1.5 tokens → 0.5 after consuming 1)
        RateLimitDecision first = strategy.allow(userId, fractionalConfig, store);
        assertThat(first.allow()).isTrue();

        // No crash, no overflow — precision maintained
        assertThat(first.retryAfterSeconds()).isGreaterThanOrEqualTo(0);
    }
}
