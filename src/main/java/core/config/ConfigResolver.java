package core.config;

import annotations.RateLimit;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConfigResolver {

    private final CustosProperties custosProperties;

    public RateLimitConfig resolve(RateLimit annotation) {

        // if annotation contains any value use it, otherwise set default custos-properties value
        int capacity = annotation.capacity() > 0
                ? annotation.capacity() : custosProperties.getCapacity();

        //same for refill rate

        int refillRate = annotation.refillRate() > 0
                ? annotation.refillRate() : custosProperties.getRefillRate();

        return new RateLimitConfig(capacity, refillRate);

    }
}
