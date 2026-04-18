package io.github.abstruse_scientia.custos.core.strategy;

import io.github.abstruse_scientia.custos.core.config.RateLimitConfig;
import io.github.abstruse_scientia.custos.core.model.Algorithm;
import io.github.abstruse_scientia.custos.core.model.LeakyBucketState;
import io.github.abstruse_scientia.custos.core.model.RateLimitDecision;
import io.github.abstruse_scientia.custos.core.store.RateLimitStore;

import java.util.Objects;

import static java.lang.Math.max;

/**
 * Time Complexity: O(1) per request
 * Space Complexity: O(1) per key
 */
public class LeakyBucketStrategy implements RateLimiterStrategy {

    @Override
    public Algorithm getAlgorithm() {
        return Algorithm.LEAKY_BUCKET;
    }

    @Override
    public RateLimitDecision allow(String key, RateLimitConfig config, RateLimitStore store) {

        final RateLimitDecision[] decisionHolder =  new RateLimitDecision[1];
        store.atomicCompute(key, (k, currentState)-> {
            long now = System.currentTimeMillis();

            // Get or create a new bucket
            LeakyBucketState state = currentState instanceof LeakyBucketState ? (LeakyBucketState) currentState:
                    new LeakyBucketState();

            double elapsedTimeMs = Math.max(0, now - state.getLastLeakTime());
            // Calculate the amount of request that has leaked since last "check".
            double leakedAmount = (elapsedTimeMs / 1000.0) * config.getRate();

            // Updated bucket level, remove leaked requests
            // tokens should never go below 0
            double currentTokens = Math.max(0, state.getTokens() - leakedAmount);
            // Update last leak time
            state.setLastLeakTime(now);

            if (currentTokens < config.getCapacity()) {
                state.setTokens(currentTokens + 1);
                decisionHolder[0] = new RateLimitDecision(true, 0);
            }else {
                long leakTimePerRequestMs = (long) ((1.0 / config.getRate()) * 1000);
                long retryAfterSeconds = max(1, (leakTimePerRequestMs + 999) / 1000);
                decisionHolder[0] = new RateLimitDecision(false, retryAfterSeconds);
            }
            return state;
        });
        return decisionHolder[0];
    }



}

