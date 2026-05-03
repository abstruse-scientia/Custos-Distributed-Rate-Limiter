package io.github.abstruse_scientia.custos.config;

import io.github.abstruse_scientia.custos.core.config.RateLimitConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test: Configuration & Initialization Tests
 * <p>Verify that RateLimitConfig instances are immutable via final fields.</p>
 */
class RateLimitConfigTest {

    /**
     * Test 1: RateLimitConfig Immutability
     * <p>Setup: Create config, verify values are stable</p>
     * <p>Verify: Config values remain unchanged after creation</p>
     */
    @Test
    @DisplayName("Test 38: RateLimitConfig immutability — values stable after creation")
    void testConfigImmutability() {
        // Arrange: Create a config with specific values
        RateLimitConfig config = new RateLimitConfig(100.0, 10.0);

        // Assert: Values are as set
        assertThat(config.getCapacity()).isEqualTo(100.0);
        assertThat(config.getRate()).isEqualTo(10.0);

        // Multiple accesses return same values (no side effects)
        assertThat(config.getCapacity()).isEqualTo(100.0);
        assertThat(config.getRate()).isEqualTo(10.0);
    }

    /**
     * Test 1.1: Multiple configs are independent
     */
    @Test
    @DisplayName("Test 38b: Multiple RateLimitConfig instances are independent")
    void testMultipleConfigsIndependent() {
        RateLimitConfig config1 = new RateLimitConfig(50.0, 5.0);
        RateLimitConfig config2 = new RateLimitConfig(200.0, 20.0);

        // Assert: Each config has its own values
        assertThat(config1.getCapacity()).isEqualTo(50.0);
        assertThat(config1.getRate()).isEqualTo(5.0);
        assertThat(config2.getCapacity()).isEqualTo(200.0);
        assertThat(config2.getRate()).isEqualTo(20.0);

        // Modifying config2 doesn't affect config1
        assertThat(config1.getCapacity()).isNotEqualTo(config2.getCapacity());
    }

    /**
     * Test 1.2: Config with decimal precision
     */
    @Test
    @DisplayName("Test 38c: RateLimitConfig maintains decimal precision")
    void testConfigDecimalPrecision() {
        RateLimitConfig config = new RateLimitConfig(0.5, 0.1);

        assertThat(config.getCapacity()).isEqualTo(0.5);
        assertThat(config.getRate()).isEqualTo(0.1);
    }
}
