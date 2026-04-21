package io.github.abstruse_scientia.custos.core.strategy;

import io.github.abstruse_scientia.custos.core.config.RateLimitConfig;
import io.github.abstruse_scientia.custos.core.model.RateLimitDecision;
import io.github.abstruse_scientia.custos.core.model.SlidingWindowCounterState;
import io.github.abstruse_scientia.custos.core.store.InMemoryStore;
import io.github.abstruse_scientia.custos.core.store.RateLimitStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Unit tests to check sliding window counter strategy for rate limiting
 * 1. testBasicCounterAccumulation: check basic requirements for strategy being met.
 * 2. testOverlappingWindowWeighting: check the request being rejected, due to heavy previous window weight.
 * 3. testPureWindowExpiration: check if the window is being expired properly
 * 4. testMultipleUsersCounterIsolation: check if the counter works properly for multiple users in isolation.
 */
public class SlidingWindowCounterStrategyTest {

    private SlidingWindowCounterStrategy strategy;
    private RateLimitStore store;
    private RateLimitConfig config;

    @BeforeEach
    public void setup() {
        strategy = new SlidingWindowCounterStrategy();
        store = new InMemoryStore();
        config = mock(RateLimitConfig.class);
    }

    @Test
    public void testBasicCounterAccumulation() {
        when(config.getCapacity()).thenReturn(10.0);
        String userId = "user1";

        for (int i = 0; i < 10; i++) {
            RateLimitDecision decision = strategy.allow(userId, config, store);
            assertThat(decision.allow()).isTrue();
        }

        RateLimitDecision decision11 = strategy.allow(userId, config, store);
        assertThat(decision11.allow()).isFalse();
    }

    @Test
    public void testOverlappingWindowsWeighting() {
        when(config.getCapacity()).thenReturn(10.0);
        String userId = "userOverlap";

        long currentMinute = System.currentTimeMillis() / 60000;
        SlidingWindowCounterState state = new SlidingWindowCounterState(currentMinute);
        state.setPreviousWindowCount(1000); 
        store.put(userId, state);

        RateLimitDecision decision = strategy.allow(userId, config, store);
        assertThat(decision.allow())
                .as("Request should be rejected due to heavy previous window weight")
                .isFalse();
    }

    @Test
    public void testPureWindowExpiration() {
        when(config.getCapacity()).thenReturn(10.0);
        String userId = "userExpired";

        long currentMinute = System.currentTimeMillis() / 60000;
        SlidingWindowCounterState state = new SlidingWindowCounterState(currentMinute - 2);
        state.setCurrentWindowCount(100); 
        store.put(userId, state);

        RateLimitDecision decision = strategy.allow(userId, config, store);
        assertThat(decision.allow()).isTrue();
        
        SlidingWindowCounterState updatedState = (SlidingWindowCounterState) store.get(userId);
        assertThat(updatedState.getPreviousWindowCount()).isEqualTo(0);
        assertThat(updatedState.getCurrentWindowCount()).isEqualTo(1);
    }

    @Test
    public void testMultipleUsersCounterIsolation() {
        when(config.getCapacity()).thenReturn(10.0);
        String user1 = "user1";
        String user2 = "user2";

        for (int i = 0; i < 10; i++) {
            strategy.allow(user1, config, store);
        }

        assertThat(strategy.allow(user1, config, store).allow()).isFalse();

        for (int i = 0; i < 10; i++) {
            assertThat(strategy.allow(user2, config, store).allow()).isTrue();
        }
    }
}
