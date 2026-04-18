package io.github.abstruse_scientia.custos.core.strategy;

import io.github.abstruse_scientia.custos.core.config.RateLimitConfig;
import io.github.abstruse_scientia.custos.core.model.Algorithm;
import io.github.abstruse_scientia.custos.core.model.RateLimitDecision;
import io.github.abstruse_scientia.custos.core.model.SlidingWindowState;
import io.github.abstruse_scientia.custos.core.store.RateLimitStore;

import java.util.Queue;

import static java.lang.Math.max;


/**
 * Sliding Window Strategy: Implements sliding window log algorithm.Window keeps moving
 * after request.
 * Used to prevent bursts at last seconds of the window(Window Boundary)
 * Space Complexity: consumes O(N) memory per user.
 *
 */
public class SlidingWindowStrategy implements RateLimiterStrategy {

    @Override
    public Algorithm getAlgorithm() {
        return Algorithm.SLIDING_WINDOW;
    }

    @Override
    public RateLimitDecision allow(String key, RateLimitConfig config, RateLimitStore store) {


        long now = System.currentTimeMillis();

        final RateLimitDecision[] decisionHolder = new RateLimitDecision[1];
        // using key.intern 
        store.atomicCompute(key, (k, currentState) -> {


            // Total duration of window
            long windowDurationMs = (long) (config.getCapacity() / config.getRate()) * 1000;
            long windowStart = now - windowDurationMs;

            /*
             * Get or create SlidingWindowState from store
             * Uses Object storage to maintain compatibility with RateLimitStore interface
             */
            SlidingWindowState state = (currentState instanceof SlidingWindowState)  ?
                    (SlidingWindowState) currentState : new  SlidingWindowState();

            Queue<Long> window = state.getTimestamps();
            window.removeIf(timeStamp -> timeStamp < windowStart);
            //if the timestamp queue's size is within the limits of the capacity, allow
            if (window.size() < config.getCapacity() ) {
                window.add(now);
                decisionHolder[0] = new RateLimitDecision(true, 0);
            }else {

                // in case of it not being the above case
                long oldestRequestTime = window.peek();
                long timeUntilOldestRequestExpires = (oldestRequestTime + windowDurationMs) - now;

                /* Calculate retry time when oldest request expires. Use + 999 because division tends to
                truncate the value toward zero. So suppose the client has to wait for 1,001 milliseconds
                or 1.001 seconds , so regular division will lead to 1s of retry but at that point
                oldest request would not expire.
                 */
                long retryAfterSeconds =  max(1, (timeUntilOldestRequestExpires + 999) / 1000);
                decisionHolder[0] = new RateLimitDecision(false, retryAfterSeconds);
            }
            return state;
        });
        return decisionHolder[0];
    }



}
