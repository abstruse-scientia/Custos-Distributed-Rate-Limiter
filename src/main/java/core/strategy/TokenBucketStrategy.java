package core.strategy;

import core.config.RateLimitConfig;
import core.model.Algorithm;
import core.model.BucketState;
import core.store.RateLimitStore;

import java.util.Objects;

import static java.lang.Math.min;


public class TokenBucketStrategy implements RateLimiterStrategy {


    @Override
    public Algorithm getAlgorithm() {
        return Algorithm.TOKEN_BUCKET;
    }

    @Override
    public boolean allow(String key, RateLimitConfig config, RateLimitStore store) {

        synchronized (key.intern()) {
            BucketState state = store.get(key);
            long now = System.currentTimeMillis();
            if (state == null) {
                store.put(key, new BucketState(config.getCapacity(), now));
            }

            //refill the bucket based on time elapsed
            //Calculate the elapsed time by calling last Refill time

            long elapsed = (now - Objects.requireNonNull(state).getLastRefillTime() ) / 1000;
            double tokensToAdd =  elapsed * config.getRefillRate();

            double tokens = min(config.getCapacity(), config.getCapacity() + tokensToAdd);
            state.setTokens(tokens);
            state.setLastRefillTime(now);

            if (state.getTokens() >= 1) {
                state.setTokens(state.getTokens() - 1); // decrease token count
                store.put(key, state); // update the store , to keep the most recent state with key
                return true;
            }
            store.put(key, state);
            return false;

        }
    }
}
