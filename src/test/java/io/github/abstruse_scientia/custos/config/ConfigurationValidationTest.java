package io.github.abstruse_scientia.custos.config;

import io.github.abstruse_scientia.custos.annotations.RateLimit;
import io.github.abstruse_scientia.custos.core.config.ConfigResolver;
import io.github.abstruse_scientia.custos.core.config.CustosProperties;
import io.github.abstruse_scientia.custos.core.config.RateLimitConfig;
import io.github.abstruse_scientia.custos.core.model.Algorithm;
import io.github.abstruse_scientia.custos.exception.RateLimitConfigurationException;
import io.github.abstruse_scientia.custos.resolver.KeyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test: Configuration & Initialization Tests
 * <p>Verify that invalid configurations are rejected with clear error messages.</p>
 */
class ConfigurationValidationTest {

    /**
     * Test 1: Invalid Configuration Validation
     * <p>Setup: Negative capacity, invalid refill rate</p>
     * <p>Verify: RateLimitConfigurationException thrown with clear message</p>
     */
    @Test
    void testInvalidConfigurationRejected() {
        // Assert: Creating a config with negative capacity throws exception
        assertThatThrownBy(() -> new RateLimitConfig(-10, 5))
                .isInstanceOf(RateLimitConfigurationException.class)
                .hasMessageContaining("Capacity must be greater than zero");
    }

    /**
     * Test 1.1: Invalid configuration — zero rate
     */
    @Test
    void testInvalidConfigurationZeroRate() {
        assertThatThrownBy(() -> new RateLimitConfig(10, 0))
                .isInstanceOf(RateLimitConfigurationException.class)
                .hasMessageContaining("Rate must be greater than zero");
    }

    /**
     * Test 1.2: RateLimitConfigurationException with cause
     */
    @Test
    @DisplayName("Test 37c: RateLimitConfigurationException carries cause")
    void testConfigurationExceptionWithCause() {
        Exception cause = new NumberFormatException("Not a number");
        RateLimitConfigurationException ex =
                new RateLimitConfigurationException("Configuration parsing failed", cause);

        assertThat(ex.getMessage()).isEqualTo("Configuration parsing failed");
        assertThat(ex.getCause()).isEqualTo(cause);
        assertThat(ex.getCause()).isInstanceOf(NumberFormatException.class);
    }

    /**
     * Test 1.3: ConfigResolver resolves annotation values correctly
     */
    @Test
    @DisplayName("Test 37d: ConfigResolver resolves annotation values to RateLimitConfig")
    void testConfigResolverResolvesAnnotation() {
        CustosProperties props = new CustosProperties();
        props.setCapacity(100);
        props.setRate(10.0);

        ConfigResolver resolver = new ConfigResolver(props);

        // Create a simulated annotation with explicit values
        RateLimit annotation = createRateLimit(50, 5.0, Algorithm.TOKEN_BUCKET, KeyType.USER);
        RateLimitConfig config = resolver.resolve(annotation);

        // Annotation values should override properties defaults
        assertThat(config.getCapacity()).isEqualTo(50);
        assertThat(config.getRate()).isEqualTo(5.0);
    }

    /**
     * Test 1.4: ConfigResolver uses defaults when annotation has zero values
     */
    @Test
    void testConfigResolverUsesDefaults() {
        CustosProperties props = new CustosProperties();
        props.setCapacity(100);
        props.setRate(10.0);

        ConfigResolver resolver = new ConfigResolver(props);

        // Annotation with 0 or negative values  should fall back to properties
        RateLimit annotation = createRateLimit(0, 0, Algorithm.TOKEN_BUCKET, KeyType.USER);
        RateLimitConfig config = resolver.resolve(annotation);

        // Should fall back to CustosProperties defaults
        assertThat(config.getCapacity()).isEqualTo(100);
        assertThat(config.getRate()).isEqualTo(10.0);
    }

    /**
     * Helper method to create a mock RateLimit annotation
     */
    private RateLimit createRateLimit(int capacity, double rate, Algorithm algorithm, KeyType keyType) {
        return new RateLimit() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return RateLimit.class;
            }

            @Override
            public KeyType keytype() {
                return keyType;
            }

            @Override
            public Algorithm algorithm() {
                return algorithm;
            }

            @Override
            public int capacity() {
                return capacity;
            }

            @Override
            public double rate() {
                return rate;
            }
        };
    }
}
