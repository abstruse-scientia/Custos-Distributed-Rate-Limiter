package io.github.abstruse_scientia.custos.core.strategy.config;

import io.github.abstruse_scientia.custos.core.store.InMemoryStore;
import io.github.abstruse_scientia.custos.core.store.RateLimitStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@TestConfiguration
@EnableAspectJAutoProxy
public class TestCustosConfig {

    @Bean
    public RateLimitStore testRateLimitStore(){
        return new InMemoryStore();
    }
}
