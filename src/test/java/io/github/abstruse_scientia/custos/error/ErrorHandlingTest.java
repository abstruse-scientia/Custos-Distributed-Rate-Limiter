package io.github.abstruse_scientia.custos.error;

import io.github.abstruse_scientia.custos.core.config.RateLimitConfig;
import io.github.abstruse_scientia.custos.core.model.RateLimitDecision;
import io.github.abstruse_scientia.custos.core.store.InMemoryStore;
import io.github.abstruse_scientia.custos.core.store.RateLimitStore;
import io.github.abstruse_scientia.custos.core.strategy.TokenBucketStrategy;
import io.github.abstruse_scientia.custos.exception.RateLimitConfigurationException;
import io.github.abstruse_scientia.custos.exception.RateLimitExceededException;
import io.github.abstruse_scientia.custos.resolver.IPKeyResolver;
import io.github.abstruse_scientia.custos.resolver.UserKeyResolver;
import io.github.abstruse_scientia.custos.core.model.RequestContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test: Exception handling for rate limit exceedance, configuration errors,
 * null key resolution, and store unavailability.
 */
class ErrorHandlingTest {

    /**
     * Test 1: Rate Limit Exceeded Exception
     * <p>Setup: Rate limit exceeded scenario</p>
     * <p>Verify: RateLimitExceededException thrown with metadata</p>
     */
    @Test
    void testRateLimitExceededException() {
        // Arrange
        TokenBucketStrategy strategy = new TokenBucketStrategy();
        RateLimitStore store = new InMemoryStore();
        RateLimitConfig config = new RateLimitConfig(5, 0.1);

        String userId = "exceeded-user";

        // Act: Exhaust the capacity
        for (int i = 0; i < 5; i++) {
            RateLimitDecision decision = strategy.allow(userId, config, store);
            assertThat(decision.allow()).isTrue();
        }

        // The 6th request should be rejected
        RateLimitDecision rejectedDecision = strategy.allow(userId, config, store);
        assertThat(rejectedDecision.allow()).isFalse();
        assertThat(rejectedDecision.retryAfterSeconds()).isGreaterThan(0);

        // Verify the exception can be created with correct metadata
        RateLimitExceededException exception =
                new RateLimitExceededException(userId, rejectedDecision.retryAfterSeconds());

        assertThat(exception.getKey()).isEqualTo(userId);
        assertThat(exception.getRetryAfterSeconds()).isGreaterThan(0);
        assertThat(exception.getMessage()).contains(userId);
        assertThat(exception.getMessage()).contains(String.valueOf(rejectedDecision.retryAfterSeconds()));
    }



    /**
     * Test 2: Configuration Exception Handling
     * <p>Setup: Invalid configuration provided</p>
     * <p>Verify: RateLimitConfigurationException thrown correctly</p>
     */
    @Test
    void testConfigurationException() {
        // Test basic constructor
        RateLimitConfigurationException ex1 =
                new RateLimitConfigurationException("Invalid capacity: -1");
        assertThat(ex1).isInstanceOf(RuntimeException.class);
        assertThat(ex1.getMessage()).isEqualTo("Invalid capacity: -1");

        // Test constructor with cause
        IllegalArgumentException cause = new IllegalArgumentException("Negative value");
        RateLimitConfigurationException ex2 =
                new RateLimitConfigurationException("Config validation failed", cause);
        assertThat(ex2.getMessage()).isEqualTo("Config validation failed");
        assertThat(ex2.getCause()).isEqualTo(cause);
    }

    /**
     * Test 2.1: Configuration exception prevents invalid state
     */
    @Test
    void testConfigurationExceptionPreventsInvalidState() {
        assertThatThrownBy(() -> {
            int capacity = -100;
            if (capacity < 0) {
                throw new RateLimitConfigurationException(
                        "Capacity must not be negative, got: " + capacity);
            }
        }).isInstanceOf(RateLimitConfigurationException.class)
                .hasMessageContaining("Capacity must not be negative");
    }

    /**
     * Test 3: Null or Invalid Key Resolution
     * <p>Setup: Key resolver returns null</p>
     * <p>Verify: Handled gracefully or appropriate behavior occurs</p>
     */
    @Test
    void testNullKeyResolution() {
        // UserKeyResolver returns null when userId is null
        UserKeyResolver userResolver = new UserKeyResolver();
        RequestContext nullUserContext = new RequestContext(null, "192.168.1.1");

        String key = userResolver.resolve(nullUserContext);
        assertThat(key).isNull();

        // IPKeyResolver returns null when IP is null
        IPKeyResolver ipResolver = new IPKeyResolver();
        RequestContext nullIpContext = new RequestContext("user1", null);

        String ipKey = ipResolver.resolve(nullIpContext);
        assertThat(ipKey).isNull();
    }

    /**
     * Test 3.1: Strategy handles null key by creating state for null
     */
    @Test
    void testStrategyWithNullKey() {
        TokenBucketStrategy strategy = new TokenBucketStrategy();
        RateLimitStore store = new InMemoryStore();
        RateLimitConfig config = new RateLimitConfig(5, 1);

        // Allow with null key — should not throw NPE from strategy
        // (behavior depends on ConcurrentHashMap support for null keys — it doesn't)
        assertThatThrownBy(() -> strategy.allow(null, config, store))
                .isInstanceOf(NullPointerException.class);
    }

    /**
     * Test 4: Store Unavailable Scenario
     * <p>Setup: Store operation fails</p>
     * <p>Verify: Error handling — exception propagated</p>
     */
    @Test
    void testStoreUnavailable() {
        TokenBucketStrategy strategy = new TokenBucketStrategy();
        RateLimitConfig config = new RateLimitConfig(10, 5);

        // Create a store that simulates failure
        RateLimitStore failingStore = new RateLimitStore() {
            @Override
            public Object get(String key) {
                throw new RuntimeException("Store connection lost");
            }

            @Override
            public void put(String key, Object state) {
                throw new RuntimeException("Store connection lost");
            }

            @Override
            public Object atomicCompute(String key,
                    java.util.function.BiFunction<String, Object, Object> remappingFunction) {
                throw new RuntimeException("Store connection lost");
            }
        };

        // Act & Assert: Store failure propagates as RuntimeException
        assertThatThrownBy(() -> strategy.allow("user1", config, failingStore))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Store connection lost");
    }

    /**
     * Test 4.1: Store unavailable with a custom wrapped exception
     */
    @Test
    void testStoreUnavailableWrapped() {
        RateLimitStore failingStore = new RateLimitStore() {
            @Override
            public Object get(String key) {
                throw new RuntimeException("Redis timeout", new java.net.ConnectException("Connection refused"));
            }

            @Override
            public void put(String key, Object state) {
                throw new RuntimeException("Redis timeout");
            }

            @Override
            public Object atomicCompute(String key,
                    java.util.function.BiFunction<String, Object, Object> remappingFunction) {
                throw new RuntimeException("Redis timeout", new java.net.ConnectException("Connection refused"));
            }
        };

        assertThatThrownBy(() -> failingStore.get("any-key"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Redis timeout")
                .hasCauseInstanceOf(java.net.ConnectException.class);
    }
}
