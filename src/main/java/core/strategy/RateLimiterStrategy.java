package core.strategy;

import core.config.RateLimitConfig;
import core.store.RateLimitStore;

public interface RateLimiterStrategy {
    boolean allow(String key, RateLimitConfig config, RateLimitStore store);
}
