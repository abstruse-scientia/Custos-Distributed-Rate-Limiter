package com.abstruse.custos.core.engine;

import com.abstruse.custos.annotations.RateLimit;
import com.abstruse.custos.core.config.ConfigResolver;
import com.abstruse.custos.core.config.RateLimitConfig;
import com.abstruse.custos.core.model.RequestContext;
import com.abstruse.custos.core.strategy.RateLimiterStrategy;
import com.abstruse.custos.core.strategy.StrategyFactory;
import lombok.RequiredArgsConstructor;
import com.abstruse.custos.resolver.KeyResolverFactory;

@RequiredArgsConstructor
public class RateLimiterEngine {

    private final KeyResolverFactory keyResolver;
    private final ConfigResolver configResolver;
    private final StrategyFactory strategyFactory;

    public boolean allow(RequestContext requestContext, RateLimit rateLimit) {
        String key = keyResolver.getKeyResolver(rateLimit.keytype()).resolve(requestContext);
        RateLimitConfig config = configResolver.resolve(rateLimit);
        RateLimiterStrategy strategy = strategyFactory.get(rateLimit.algorithm());
        return strategy.allow(key, config, null);
    }
}
