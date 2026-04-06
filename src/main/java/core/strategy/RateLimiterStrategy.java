package core.strategy;

import core.config.RateLimitConfig;
import core.model.Algorithm;
import core.store.RateLimitStore;

public interface RateLimiterStrategy {
    Algorithm getAlgorithm();
    boolean allow(String key, RateLimitConfig config, RateLimitStore store);
}
