package com.abstruse.custos.core.strategy;

import com.abstruse.custos.core.config.RateLimitConfig;
import com.abstruse.custos.core.model.Algorithm;
import com.abstruse.custos.core.model.RateLimitDecision;
import com.abstruse.custos.core.store.RateLimitStore;

public class SlidingWindowStrategy implements RateLimiterStrategy {

    @Override
    public Algorithm getAlgorithm() {
        return Algorithm.SLIDING_WINDOW;
    }

    @Override
    public RateLimitDecision allow(String key, RateLimitConfig config, RateLimitStore store) {

        return new RateLimitDecision(false, 0);
    }
}
