package com.abstruse.custos.core.strategy;

import com.abstruse.custos.core.config.RateLimitConfig;
import com.abstruse.custos.core.model.Algorithm;
import com.abstruse.custos.core.store.RateLimitStore;

public interface RateLimiterStrategy {
    Algorithm getAlgorithm();
    boolean allow(String key, RateLimitConfig config, RateLimitStore store);
}
