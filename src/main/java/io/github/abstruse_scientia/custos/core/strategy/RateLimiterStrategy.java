package io.github.abstruse_scientia.custos.core.strategy;

import io.github.abstruse_scientia.custos.core.config.RateLimitConfig;
import io.github.abstruse_scientia.custos.core.model.Algorithm;
import io.github.abstruse_scientia.custos.core.model.RateLimitDecision;
import io.github.abstruse_scientia.custos.core.store.RateLimitStore;

public interface RateLimiterStrategy {
    Algorithm getAlgorithm();
    RateLimitDecision allow(String key, RateLimitConfig config, RateLimitStore store);
}
