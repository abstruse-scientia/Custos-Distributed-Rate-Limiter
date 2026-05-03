package io.github.abstruse_scientia.custos.concurrency;

import io.github.abstruse_scientia.custos.core.config.RateLimitConfig;
import io.github.abstruse_scientia.custos.core.model.RateLimitDecision;
import io.github.abstruse_scientia.custos.core.store.InMemoryStore;
import io.github.abstruse_scientia.custos.core.strategy.TokenBucketStrategy;
import io.github.abstruse_scientia.custos.core.strategy.LeakyBucketStrategy;
import io.github.abstruse_scientia.custos.core.strategy.SlidingWindowCounterStrategy;
import io.github.abstruse_scientia.custos.core.strategy.SlidingWindowStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Category 6: Concurrency & Thread Safety Tests
 * Test 49: Lock-Free vs Lock-Based Store Performance
 * Verify performance is acceptable under concurrent load (100+ requests/sec).
 */
class PerformanceTest {

    /**
     * Test 49: Store Performance Under Load
     * Setup: High concurrent load
     * Verify: Performance acceptable under concurrent load (100+ requests/sec)
     */
    @Test
    @DisplayName("Test 49: Store performance under concurrent load — 100+ req/sec")
    void testStorePerformanceUnderLoad() throws InterruptedException {
        // Arrange
        TokenBucketStrategy strategy = new TokenBucketStrategy();
        InMemoryStore store = new InMemoryStore();
        RateLimitConfig config = new RateLimitConfig(100000, 1000); // Large capacity

        int threadCount = 10;
        int requestsPerThread = 100; // 1000 total requests
        AtomicInteger completedRequests = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        // Act: Measure time for 1000 concurrent requests
        long startTime = System.nanoTime();

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < requestsPerThread; i++) {
                        // Use unique keys per thread to avoid contention bottleneck for this perf test
                        strategy.allow("perf-user-" + threadId, config, store);
                        completedRequests.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        long endTime = System.nanoTime();
        executor.shutdown();

        // Assert: All requests completed
        assertThat(completed).isTrue();
        assertThat(completedRequests.get()).isEqualTo(threadCount * requestsPerThread);

        // Assert: Performance (100+ requests per second)
        double durationSeconds = (endTime - startTime) / 1_000_000_000.0;
        double requestsPerSecond = completedRequests.get() / durationSeconds;

        assertThat(requestsPerSecond)
                .as("Should handle 100+ requests per second, actual: %.2f req/sec", requestsPerSecond)
                .isGreaterThan(100);
    }

    /**
     * Test 49b: Performance with shared key under contention
     */
    @Test
    @DisplayName("Test 49b: Performance under contention — shared key")
    void testPerformanceUnderContention() throws InterruptedException {
        TokenBucketStrategy strategy = new TokenBucketStrategy();
        InMemoryStore store = new InMemoryStore();
        RateLimitConfig config = new RateLimitConfig(100000, 10000); // Very large capacity

        String sharedKey = "contention-user"; // All threads write to same key
        int threadCount = 10;
        int requestsPerThread = 100;
        AtomicInteger completedRequests = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        long startTime = System.nanoTime();

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < requestsPerThread; i++) {
                        strategy.allow(sharedKey, config, store);
                        completedRequests.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        long endTime = System.nanoTime();
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(completedRequests.get()).isEqualTo(threadCount * requestsPerThread);

        double durationSeconds = (endTime - startTime) / 1_000_000_000.0;
        double requestsPerSecond = completedRequests.get() / durationSeconds;

        // Even under contention, should handle 100+ req/sec
        assertThat(requestsPerSecond)
                .as("Should handle 100+ req/sec under contention, actual: %.2f req/sec", requestsPerSecond)
                .isGreaterThan(100);
    }

    /**
     * Test 49c: All algorithms perform under concurrent load
     */
    @Test
    @DisplayName("Test 49c: All algorithms handle concurrent load")
    void testAllAlgorithmsPerformance() throws InterruptedException {
        InMemoryStore store = new InMemoryStore();
        RateLimitConfig config = new RateLimitConfig(100000, 1000);

        TokenBucketStrategy tb = new TokenBucketStrategy();
        SlidingWindowStrategy sw = new SlidingWindowStrategy();
        LeakyBucketStrategy lb = new LeakyBucketStrategy();
        SlidingWindowCounterStrategy swc = new SlidingWindowCounterStrategy();

        int requestsPerAlgorithm = 200;
        int threadCount = 4;

        // Run each algorithm concurrently
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger completed = new AtomicInteger(0);

        long startTime = System.nanoTime();

        executor.submit(() -> {
            try {
                for (int i = 0; i < requestsPerAlgorithm; i++) {
                    tb.allow("perf-tb", config, store);
                    completed.incrementAndGet();
                }
            } finally { doneLatch.countDown(); }
        });

        executor.submit(() -> {
            try {
                for (int i = 0; i < requestsPerAlgorithm; i++) {
                    sw.allow("perf-sw", config, store);
                    completed.incrementAndGet();
                }
            } finally { doneLatch.countDown(); }
        });

        executor.submit(() -> {
            try {
                for (int i = 0; i < requestsPerAlgorithm; i++) {
                    lb.allow("perf-lb", config, store);
                    completed.incrementAndGet();
                }
            } finally { doneLatch.countDown(); }
        });

        executor.submit(() -> {
            try {
                for (int i = 0; i < requestsPerAlgorithm; i++) {
                    swc.allow("perf-swc", config, store);
                    completed.incrementAndGet();
                }
            } finally { doneLatch.countDown(); }
        });

        boolean done = doneLatch.await(30, TimeUnit.SECONDS);
        long endTime = System.nanoTime();
        executor.shutdown();

        assertThat(done).isTrue();
        assertThat(completed.get()).isEqualTo(requestsPerAlgorithm * threadCount);

        double durationSeconds = (endTime - startTime) / 1_000_000_000.0;
        double requestsPerSecond = completed.get() / durationSeconds;

        assertThat(requestsPerSecond)
                .as("All algorithms combined: %.2f req/sec", requestsPerSecond)
                .isGreaterThan(100);
    }
}
