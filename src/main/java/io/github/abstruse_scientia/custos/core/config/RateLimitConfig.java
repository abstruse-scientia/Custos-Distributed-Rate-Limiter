package io.github.abstruse_scientia.custos.core.config;

import io.github.abstruse_scientia.custos.exception.RateLimitConfigurationException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Getter
@EnableConfigurationProperties(CustosProperties.class)
public class RateLimitConfig {

    private final double capacity;
    private final double rate;

    public RateLimitConfig(double capacity, double rate) {
        if (capacity <= 0) {
            throw new RateLimitConfigurationException("Capacity must be greater than zero");
        }
        if (rate <= 0) {
            throw new RateLimitConfigurationException("Rate must be greater than zero");
        }

        this.capacity = capacity;
        this.rate = rate;
    }
}
