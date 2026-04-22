package io.github.abstruse_scientia.custos.core.strategy;

import io.github.abstruse_scientia.custos.core.config.RateLimitConfig;
import io.github.abstruse_scientia.custos.core.model.RateLimitDecision;
import io.github.abstruse_scientia.custos.core.store.InMemoryStore;
import io.github.abstruse_scientia.custos.core.store.RateLimitStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for Sliding Window Rate limiting Strategy
 * 1. testBasicRequestCounting: cheks basic requests are being allowed within defined capacity,
 * while the rest of them are rejected.
 * 2. testWindowExpiration: check if the window slides and new requests are allowed
 * 3. testMultipleWindowTracking: as the name suggests,
 * checks if different user will have different window of request.
 * 4. testWindowBoundaryCondition: check whether request is being rejected on boundary or not.
 */
public class SlidingWindowStrategyTest {

    private SlidingWindowStrategy strategy;
    private RateLimitStore store;
    private RateLimitConfig config;

    @BeforeEach
    public void setup() {
        strategy = new SlidingWindowStrategy();
        store = new InMemoryStore();
        config = mock(RateLimitConfig.class);
    }

    @Test
    public void testBasicRequestCounting() {
        when(config.getCapacity()).thenReturn(10.0);
        when(config.getRate()).thenReturn(10.0 / 60.0);

        String userId = "user1";

        for (int i = 0; i < 10; i++) {
            RateLimitDecision decision = strategy.allow(userId, config, store);
            assertThat(decision.allow())
                .as("Request %d should be allowed within window", i + 1)
                .isTrue();
        }

        RateLimitDecision decision11 = strategy.allow(userId, config, store);
        assertThat(decision11.allow())
            .as("11th request should be rejected (window limit exceeded)")
            .isFalse();
    }

    @Test
    public void testWindowExpiration() throws InterruptedException {
        when(config.getCapacity()).thenReturn(5.0);
        when(config.getRate()).thenReturn(5.0);

        String userId = "user1";

        for (int i = 0; i < 5; i++) {
            RateLimitDecision decision = strategy.allow(userId, config, store);
            assertThat(decision.allow())
                .as("Request %d in first window should be allowed", i + 1)
                .isTrue();
        }

        Thread.sleep(1100);

        for (int i = 0; i < 5; i++) {
            RateLimitDecision decision = strategy.allow(userId, config, store);
            assertThat(decision.allow())
                .as("Request %d in new window should be allowed", i + 1)
                .isTrue();
        }
    }

    @Test
    public void testMultipleWindowTracking() {
        when(config.getCapacity()).thenReturn(5.0);
        when(config.getRate()).thenReturn(5.0 / 60.0);

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
            .as("User1 request should be rejected (window full)")
            .isFalse();

        for (int i = 0; i < 5; i++) {
            RateLimitDecision decision = strategy.allow(user2, config, store);
            assertThat(decision.allow())
                .as("User2 request %d should be allowed (independent window)", i + 1)
                .isTrue();
        }
    }

    @Test
    public void testWindowBoundaryCondition() throws InterruptedException {
        when(config.getCapacity()).thenReturn(5.0);
        when(config.getRate()).thenReturn(5.0);

        String userId = "user1";

        for (int i = 0; i < 5; i++) {
            RateLimitDecision decision = strategy.allow(userId, config, store);
            assertThat(decision.allow())
                .as("Request %d in first window should be allowed", i + 1)
                .isTrue();
        }

        RateLimitDecision boundaryDecision = strategy.allow(userId, config, store);
        assertThat(boundaryDecision.allow())
            .as("Request at boundary (still in first window) should be rejected")
            .isFalse();

        Thread.sleep(1100);

        RateLimitDecision afterBoundary = strategy.allow(userId, config, store);
        assertThat(afterBoundary.allow())
            .as("Request after window boundary should be allowed (new window)")
            .isTrue();
    }
}
