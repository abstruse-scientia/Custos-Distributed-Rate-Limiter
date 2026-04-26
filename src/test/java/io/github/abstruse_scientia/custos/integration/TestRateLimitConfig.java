package io.github.abstruse_scientia.custos.integration;

import io.github.abstruse_scientia.custos.utility.UserIdResolver;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration that provides a custom UserIdResolver for integration tests.
 * This ensures user based rate limiting tests work correctly with consistent user IDs.
 */
@TestConfiguration
public class TestRateLimitConfig {

    /**
     * Provide a custom UserIdResolver that extracts userId from headers during tests.
     * This overrides the default NoOpUserIdResolver to enable proper testing.
     */
    @Bean
    @Primary
    public UserIdResolver testUserIdResolver() {
        return new TestUserIdResolver();
    }
}

