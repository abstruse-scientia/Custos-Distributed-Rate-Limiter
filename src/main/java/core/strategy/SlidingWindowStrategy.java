package core.strategy;

import core.config.RateLimitConfig;
import core.model.Algorithm;
import core.store.RateLimitStore;

public class SlidingWindowStrategy implements RateLimiterStrategy {

    @Override
    public Algorithm getAlgorithm() {
        return Algorithm.SLIDING_WINDOW;
    }

    @Override
    public boolean allow(String key, RateLimitConfig config, RateLimitStore store) {
        return false;
    }
}
