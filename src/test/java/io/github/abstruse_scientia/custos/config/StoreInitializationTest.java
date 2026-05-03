package io.github.abstruse_scientia.custos.config;

import io.github.abstruse_scientia.custos.core.store.InMemoryStore;
import io.github.abstruse_scientia.custos.core.store.RateLimitStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test: Configuration & Initialization Tests
 * <p>Verify stores initialize correctly and are ready for operations.</p>
 */
class StoreInitializationTest {

    /**
     * Test 1: In-Memory Store Initialization
     * <p>Setup: Initialize in-memory store</p>
     * <p>Verify: Store is ready for operations</p>
     */
    @Test
    void testInMemoryStoreInitialization() {
        // Arrange & Act
        RateLimitStore store = new InMemoryStore();

        // Assert: Store is created and ready
        assertThat(store).isNotNull();
        assertThat(store.get("any-key")).isNull();
    }

}
