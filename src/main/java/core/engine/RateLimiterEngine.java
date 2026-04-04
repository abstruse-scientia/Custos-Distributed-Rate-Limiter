package core.engine;

import annotations.RateLimit;
import core.config.RateLimitConfig;
import core.model.RequestContext;
import core.store.RateLimitStore;
import core.strategy.RateLimiterStrategy;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RateLimiterEngine {

    private final RateLimiterStrategy strategy;
    private final RateLimitStore store;

    public boolean allow(RequestContext requestContext, RateLimit rateLimit) {
        String key = requestContext.getDummyKey();
        RateLimitConfig config = new RateLimitConfig(
                rateLimit.capacity(),
                rateLimit.refillRate()
        );
        return strategy.allow(key, config, store);
    }
}
