package io.github.abstruse_scientia.custos.core.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Getter
@AllArgsConstructor
@EnableConfigurationProperties(CustosProperties.class)
public class RateLimitConfig {

    private final int capacity;
    private final double rate;
}
