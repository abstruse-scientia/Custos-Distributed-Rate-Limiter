package io.github.abstruse_scientia.custos.concurrency;

import io.github.abstruse_scientia.custos.core.config.RateLimitConfig;
import io.github.abstruse_scientia.custos.core.model.RateLimitDecision;
import io.github.abstruse_scientia.custos.core.store.InMemoryStore;
import io.github.abstruse_scientia.custos.core.strategy.TokenBucketStrategy;
import io.github.abstruse_scientia.custos.core.strategy.SlidingWindowStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests: Concurrency & Thread Safety Tests
 */
class ConcurrencyTest {

    /**
     * Test 1: Concurrent Requests from Multiple Threads
     * <p>Setup: 10 threads, each making 10 requests, capacity = 50</p>
     * <p>Verify: Exactly around 50 requests pass, rest rejected; no race conditions</p>
     */
    @Test
    void testConcurrentRequests() throws InterruptedException {
        // Arrange
        TokenBucketStrategy strategy = new TokenBucketStrategy();
        InMemoryStore store = new InMemoryStore();
        // Capacity = 50, very low refill rate so time-based refill is negligible
        RateLimitConfig config = new RateLimitConfig(50, 0.001);

        String sharedKey = "concurrent-user";
        int threadCount = 10;
        int requestsPerThread = 10; // 100 total, but only 50 capacity
        AtomicInteger allowedCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Act: Launch threads that all start simultaneously
        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    for (int i = 0; i < requestsPerThread; i++) {
                        RateLimitDecision decision = strategy.allow(sharedKey, config, store);
                        if (decision.allow()) {
                            allowedCount.incrementAndGet();
                        } else {
                            rejectedCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Release all threads at once
        doneLatch.await(10, TimeUnit.SECONDS); // Wait for all to finish
        executor.shutdown();

        // Assert: Total requests = 100
        assertThat(allowedCount.get() + rejectedCount.get())
                .isEqualTo(threadCount * requestsPerThread);

        // Approximately 50 should be allowed (capacity = 50)
        // Due to refill rate, slightly more may pass, but we allow a small margin
        assertThat(allowedCount.get())
                .as("Allowed count should be approximately = capacity (50)")
                .isGreaterThanOrEqualTo(49) // Allow small margin for timing
                .isLessThanOrEqualTo(55);

        // At least some requests should be rejected
        assertThat(rejectedCount.get()).isGreaterThan(0);
    }

    /**
     * Test 2: Thread-Safe Token Bucket Updates
     * <p>Setup: Multiple threads modifying same bucket simultaneously</p>
     * <p>Verify: No lost updates, token count is accurate</p>
     */
    @Test
    void testThreadSafeBucketUpdates() throws InterruptedException {
        // Arrange
        TokenBucketStrategy strategy = new TokenBucketStrategy();
        InMemoryStore store = new InMemoryStore();
        // Very low refill so we don't have refill interference
        RateLimitConfig config = new RateLimitConfig(1000, 0.0001);

        String sharedKey = "threadsafe-user";
        int threadCount = 20;
        int requestsPerThread = 50; // 1000 total = capacity
        AtomicInteger totalAllowed = new AtomicInteger(0);
        AtomicInteger totalRejected = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < requestsPerThread; i++) {
                        RateLimitDecision decision = strategy.allow(sharedKey, config, store);
                        if (decision.allow()) {
                            totalAllowed.incrementAndGet();
                        } else {
                            totalRejected.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert: Total processed = threadCount * requestsPerThread
        int totalProcessed = totalAllowed.get() + totalRejected.get();
        assertThat(totalProcessed).isEqualTo(threadCount * requestsPerThread);

        // No lost updates: allowed + rejected = total
        // Allowed should be close to capacity (1000)
        assertThat(totalAllowed.get())
                .as("Allowed should be close to capacity (1000)")
                .isGreaterThanOrEqualTo(990)
                .isLessThanOrEqualTo(1010);
    }

    /**
     * Test 2.1: Concurrent sliding window requests
     */
    @Test
    void testConcurrentSlidingWindow() throws InterruptedException {
        SlidingWindowStrategy strategy = new SlidingWindowStrategy();
        InMemoryStore store = new InMemoryStore();
        RateLimitConfig config = new RateLimitConfig(100, 60); // 100 max, 60s window

        String sharedKey = "concurrent-sw";
        int threadCount = 10;
        int requestsPerThread = 20;
        AtomicInteger totalAllowed = new AtomicInteger(0);
        AtomicInteger totalRejected = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < requestsPerThread; i++) {
                        RateLimitDecision decision = strategy.allow(sharedKey, config, store);
                        if (decision.allow()) {
                            totalAllowed.incrementAndGet();
                        } else {
                            totalRejected.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Total processed
        assertThat(totalAllowed.get() + totalRejected.get())
                .isEqualTo(threadCount * requestsPerThread);

        // Allowed should be close to capacity (100)
        assertThat(totalAllowed.get())
                .as("Allowed count close to capacity for sliding window")
                .isGreaterThanOrEqualTo(99)
                .isLessThanOrEqualTo(105);
    }
}
