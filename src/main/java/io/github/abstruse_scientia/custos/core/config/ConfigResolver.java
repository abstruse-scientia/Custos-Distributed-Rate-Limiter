package io.github.abstruse_scientia.custos.core.config;

import io.github.abstruse_scientia.custos.annotations.RateLimit;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConfigResolver {

    private final CustosProperties custosProperties;

    public RateLimitConfig resolve(RateLimit annotation) {

        // if annotation contains any value use it, otherwise set default custos-properties value
        int capacity = annotation.capacity() > 0
                ? annotation.capacity() : custosProperties.getCapacity();

        //same for refill rate

        double refillRate = annotation.rate() > 0
                ? annotation.rate() : custosProperties.getRate();

        return new RateLimitConfig(capacity, refillRate);

    }
}
