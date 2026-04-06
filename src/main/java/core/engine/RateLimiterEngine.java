package core.engine;

import annotations.RateLimit;
import core.config.ConfigResolver;
import core.config.RateLimitConfig;
import core.model.RequestContext;
import core.store.RateLimitStore;
import core.strategy.RateLimiterStrategy;
import core.strategy.StrategyFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RateLimiterEngine {

    private final RateLimitStore store;
    private final ConfigResolver configResolver;
    private final StrategyFactory strategyFactory;

    public boolean allow(RequestContext requestContext, RateLimit rateLimit) {
        String key = requestContext.getDummyKey();
        RateLimitConfig config = configResolver.resolve(rateLimit);
        RateLimiterStrategy strategy = strategyFactory.get(rateLimit.algorithm());
        return strategy.allow(key, config, store);
    }
}
