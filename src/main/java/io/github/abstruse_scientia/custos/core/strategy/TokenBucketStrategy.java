package io.github.abstruse_scientia.custos.core.strategy;

import io.github.abstruse_scientia.custos.core.config.RateLimitConfig;
import io.github.abstruse_scientia.custos.core.model.Algorithm;
import io.github.abstruse_scientia.custos.core.model.BucketState;
import io.github.abstruse_scientia.custos.core.model.RateLimitDecision;
import io.github.abstruse_scientia.custos.core.store.RateLimitStore;

import java.util.Objects;

import static java.lang.Math.max;
import static java.lang.Math.min;



public class TokenBucketStrategy implements RateLimiterStrategy {


    @Override
    public Algorithm getAlgorithm() {
        return Algorithm.TOKEN_BUCKET;
    }

    @Override
    public RateLimitDecision allow(String key, RateLimitConfig config, RateLimitStore store) {

        synchronized (key.intern()) {
            BucketState state = store.get(key);
            long now = System.currentTimeMillis();
            if (state == null) {
                store.put(key, new BucketState(config.getCapacity(), now));
            }

            //refill the bucket based on time elapsed
            //Calculate the elapsed time by calling last Refill time

            double elapsed = (now - Objects.requireNonNull(state).getLastRefillTime() ) / 1000.0;
            double tokensToAdd =  elapsed * config.getRefillRate();

            double tokens = min(config.getCapacity(), state.getTokens() + tokensToAdd);
            state.setTokens(tokens);
            state.setLastRefillTime(now);

            if (state.getTokens() >= 1) {
                state.setTokens(state.getTokens() - 1); // decrease token count
                store.put(key, state); // update the store , to keep the most recent state with key
                return new RateLimitDecision(true, 0);
            }
            long retryAfterSeconds = max(1, Math.round((float) 1 / config.getRefillRate()));
            store.put(key, state);
            return new  RateLimitDecision(false, retryAfterSeconds);

        }
    }
}
