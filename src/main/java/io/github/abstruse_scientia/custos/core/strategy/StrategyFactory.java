package io.github.abstruse_scientia.custos.core.strategy;

import io.github.abstruse_scientia.custos.core.model.Algorithm;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StrategyFactory {

    private final Map<Algorithm, RateLimiterStrategy> strategyMap;

    public StrategyFactory(List<RateLimiterStrategy> strategies) {
        this.strategyMap = strategies.stream().collect(Collectors.toMap(
                RateLimiterStrategy::getAlgorithm,
                strategy -> strategy

        ));
    }

    public RateLimiterStrategy get(Algorithm algorithm) {
        return strategyMap.get(algorithm);
    }
}
