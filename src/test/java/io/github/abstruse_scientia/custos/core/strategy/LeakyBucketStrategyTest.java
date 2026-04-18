package io.github.abstruse_scientia.custos.core.strategy;

import io.github.abstruse_scientia.custos.core.config.RateLimitConfig;
import io.github.abstruse_scientia.custos.core.model.RateLimitDecision;
import io.github.abstruse_scientia.custos.core.store.InMemoryStore;
import io.github.abstruse_scientia.custos.core.store.RateLimitStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Leaky Bucket Rate Limiting Strategy
 *
 * Test scenarios:
 * 1. testBasicLeakyBucket: Verifies basic leaky bucket functionality
 * 2. testBucketLeaking: Verifies bucket leaks over time and allows new requests
 * 3. testMultipleUsersInIsolation: Confirms rate limiting is per-user, not global
 * 4. testLeakingBehavior: Verifies that over time, bucket leaks and allows new requests
 */
public class LeakyBucketStrategyTest {

    private LeakyBucketStrategy strategy;
    private RateLimitStore store;
    private RateLimitConfig config;

    @BeforeEach
    public void setup() {
        strategy = new LeakyBucketStrategy();
        store = new InMemoryStore();
        // capacity = 5 requests, rate = 1 request/second (leaks 1 per second)
        config = new RateLimitConfig(5, 1.0);
    }

    @Test
    public void testBasicLeakyBucket() {
        // Fill bucket with 5 requests
        for (int i = 0; i < 5; i++) {
            RateLimitDecision decision = strategy.allow("user1", config, store);
            Assertions.assertTrue(decision.allow(), "Request " + (i+1) + " should be allowed");
        }

        // 6th request should be denied (bucket full)
        RateLimitDecision decision = strategy.allow("user1", config, store);
        Assertions.assertFalse(decision.allow(), "6th request should be denied (bucket full)");
        Assertions.assertTrue(decision.retryAfterSeconds() > 0, "Retry-after should be positive");
    }

    @Test
    public void testBucketLeaking() throws InterruptedException {
        // Fill bucket
        for (int i = 0; i < 5; i++) {
            strategy.allow("user1", config, store);
        }

        // Deny 6th request
        RateLimitDecision decision = strategy.allow("user1", config, store);
        Assertions.assertFalse(decision.allow());

        long retryAfterSeconds = decision.retryAfterSeconds();
        System.out.println("Retry after: " + retryAfterSeconds + " seconds (leak rate: 1 request/sec)");

        // Wait for bucket to leak one request
        Thread.sleep((retryAfterSeconds + 1) * 1000);

        // Now 7th request should be allowed (one request leaked)
        RateLimitDecision decision2 = strategy.allow("user1", config, store);
        Assertions.assertTrue(decision2.allow(), "Request should be allowed after bucket leaks");
    }

    @Test
    public void testLeakingBehavior() throws InterruptedException {
        // Add 3 requests
        for (int i = 0; i < 3; i++) {
            RateLimitDecision decision = strategy.allow("user1", config, store);
            Assertions.assertTrue(decision.allow());
        }

        // Wait 2 seconds (should leak ~2 requests at 1 request/sec)
        Thread.sleep(2000);

        // Should be able to add 2-3 new requests
        int allowedCount = 0;
        for (int i = 0; i < 3; i++) {
            RateLimitDecision decision = strategy.allow("user1", config, store);
            if (decision.allow()) {
                allowedCount++;
            } else {
                break;
            }
        }
        
        Assertions.assertTrue(allowedCount >= 1, "Should allow at least 1 request after leak");
    }

    @Test
    public void testMultipleUsersInIsolation() {
        // User 1: Fill bucket
        for (int i = 0; i < 5; i++) {
            RateLimitDecision decision = strategy.allow("user1", config, store);
            Assertions.assertTrue(decision.allow());
        }

        // User 1: Deny 6th request
        RateLimitDecision decision = strategy.allow("user1", config, store);
        Assertions.assertFalse(decision.allow());

        // User 2: Should still allow 5 requests (isolated from user1)
        for (int i = 0; i < 5; i++) {
            RateLimitDecision decision2 = strategy.allow("user2", config, store);
            Assertions.assertTrue(decision2.allow(), "User 2 should not be affected by User 1's rate limit");
        }

        // User 2: Deny 6th request
        RateLimitDecision decision2 = strategy.allow("user2", config, store);
        Assertions.assertFalse(decision2.allow());
    }

    @Test
    public void testEmptyBucketAllowsRequests() throws InterruptedException {
        // Fill bucket
        for (int i = 0; i < 5; i++) {
            strategy.allow("user1", config, store);
        }

        // Wait for bucket to completely empty (5 seconds at 1 request/sec)
        Thread.sleep(5500);

        // Should allow new requests (bucket empty)
        for (int i = 0; i < 5; i++) {
            RateLimitDecision decision = strategy.allow("user1", config, store);
            Assertions.assertTrue(decision.allow(), "Request " + (i+1) + " should be allowed (bucket empty)");
        }
    }
}




