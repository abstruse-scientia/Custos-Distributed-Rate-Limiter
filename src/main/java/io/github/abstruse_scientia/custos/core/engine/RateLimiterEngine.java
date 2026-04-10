package io.github.abstruse_scientia.custos.core.engine;

import io.github.abstruse_scientia.custos.annotations.RateLimit;
import io.github.abstruse_scientia.custos.core.config.ConfigResolver;
import io.github.abstruse_scientia.custos.core.config.RateLimitConfig;
import io.github.abstruse_scientia.custos.core.model.RateLimitDecision;
import io.github.abstruse_scientia.custos.core.model.RequestContext;
import io.github.abstruse_scientia.custos.core.store.RateLimitStore;
import io.github.abstruse_scientia.custos.core.strategy.RateLimiterStrategy;
import io.github.abstruse_scientia.custos.core.strategy.StrategyFactory;
import lombok.RequiredArgsConstructor;
import io.github.abstruse_scientia.custos.resolver.KeyResolverFactory;

@RequiredArgsConstructor
public class RateLimiterEngine {

    private final KeyResolverFactory keyResolver;
    private final ConfigResolver configResolver;
    private final StrategyFactory strategyFactory;
    private final RateLimitStore store;

    public RateLimitDecision allow(RequestContext requestContext, RateLimit rateLimit) {
        String key = keyResolver.getKeyResolver(rateLimit.keytype()).resolve(requestContext);
        RateLimitConfig config = configResolver.resolve(rateLimit);
        RateLimiterStrategy strategy = strategyFactory.get(rateLimit.algorithm());
        return strategy.allow(key, config, this.store);
    }
}
