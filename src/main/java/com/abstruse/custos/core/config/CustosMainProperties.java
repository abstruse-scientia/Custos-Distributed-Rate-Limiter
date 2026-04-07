package com.abstruse.custos.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "custos")
@Getter
@Setter
public class CustosMainProperties {
    private String store = "memory";
}
