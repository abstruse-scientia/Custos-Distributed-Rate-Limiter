package io.github.abstruse_scientia.custos.config;

import io.github.abstruse_scientia.custos.core.config.RateLimitConfig;
import io.github.abstruse_scientia.custos.core.model.Algorithm;
import io.github.abstruse_scientia.custos.core.model.RateLimitDecision;
import io.github.abstruse_scientia.custos.core.store.InMemoryStore;
import io.github.abstruse_scientia.custos.core.store.RateLimitStore;
import io.github.abstruse_scientia.custos.core.strategy.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test: Configuration & Initialization Tests
 * <p>Verify all algorithms initialize correctly without state corruption.</p>
 */
class AlgorithmInitializationTest {

    /**
     * Test 1: Algorithm Initialization
     * <p>Setup: Initialize Token Bucket, Sliding Window, Leaky Bucket, Sliding Window Counter</p>
     * <p>Verify: All algorithms initialize without state corruption</p>
     */
    @Test
    void testAlgorithmInitialization() {
        RateLimitStore store = new InMemoryStore();
        RateLimitConfig config = new RateLimitConfig(10, 5);

        // Token Bucket
        TokenBucketStrategy tokenBucket = new TokenBucketStrategy();
        assertThat(tokenBucket.getAlgorithm()).isEqualTo(Algorithm.TOKEN_BUCKET);
        RateLimitDecision tbDecision = tokenBucket.allow("init-user-tb", config, store);
        assertThat(tbDecision).isNotNull();
        assertThat(tbDecision.allow()).isTrue();

        // Sliding Window
        SlidingWindowStrategy slidingWindow = new SlidingWindowStrategy();
        assertThat(slidingWindow.getAlgorithm()).isEqualTo(Algorithm.SLIDING_WINDOW);
        RateLimitDecision swDecision = slidingWindow.allow("init-user-sw", config, store);
        assertThat(swDecision).isNotNull();
        assertThat(swDecision.allow()).isTrue();

        // Leaky Bucket
        LeakyBucketStrategy leakyBucket = new LeakyBucketStrategy();
        assertThat(leakyBucket.getAlgorithm()).isEqualTo(Algorithm.LEAKY_BUCKET);
        RateLimitDecision lbDecision = leakyBucket.allow("init-user-lb", config, store);
        assertThat(lbDecision).isNotNull();
        assertThat(lbDecision.allow()).isTrue();

        // Sliding Window Counter
        SlidingWindowCounterStrategy swCounter = new SlidingWindowCounterStrategy();
        assertThat(swCounter.getAlgorithm()).isEqualTo(Algorithm.SLIDING_WINDOW_COUNTER);
        RateLimitDecision swcDecision = swCounter.allow("init-user-swc", config, store);
        assertThat(swcDecision).isNotNull();
        assertThat(swcDecision.allow()).isTrue();
    }

    /**
     * Test 1.1: StrategyFactory maps algorithms correctly
     */
    @Test
    @DisplayName("Test 39b: StrategyFactory correctly maps all algorithms")
    void testStrategyFactoryMapping() {
        TokenBucketStrategy tb = new TokenBucketStrategy();
        SlidingWindowStrategy sw = new SlidingWindowStrategy();
        LeakyBucketStrategy lb = new LeakyBucketStrategy();
        SlidingWindowCounterStrategy swc = new SlidingWindowCounterStrategy();

        StrategyFactory factory = new StrategyFactory(
                java.util.List.of(tb, sw, lb, swc)
        );

        assertThat(factory.get(Algorithm.TOKEN_BUCKET)).isSameAs(tb);
        assertThat(factory.get(Algorithm.SLIDING_WINDOW)).isSameAs(sw);
        assertThat(factory.get(Algorithm.LEAKY_BUCKET)).isSameAs(lb);
        assertThat(factory.get(Algorithm.SLIDING_WINDOW_COUNTER)).isSameAs(swc);
    }

    /**
     * Test 1.2: Algorithms don't corrupt each other's state when sharing same store
     */
    @Test
    @DisplayName("Test 39c: Different algorithms don't corrupt each other's state")
    void testAlgorithmStateIsolation() {
        InMemoryStore store = new InMemoryStore();
        RateLimitConfig config = new RateLimitConfig(5, 2);

        TokenBucketStrategy tb = new TokenBucketStrategy();
        SlidingWindowStrategy sw = new SlidingWindowStrategy();

        // Use different keys for different algorithms
        for (int i = 0; i < 3; i++) {
            tb.allow("tb-key", config, store);
            sw.allow("sw-key", config, store);
        }

        // Both algorithms should still allow requests (3/5 capacity used each)
        RateLimitDecision tbDecision = tb.allow("tb-key", config, store);
        RateLimitDecision swDecision = sw.allow("sw-key", config, store);

        assertThat(tbDecision.allow()).isTrue();
        assertThat(swDecision.allow()).isTrue();
    }
}
