package io.github.abstruse_scientia.custos.integration.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.abstruse_scientia.custos.aop.RateLimitAspect;
import io.github.abstruse_scientia.custos.core.config.CustosProperties;
import io.github.abstruse_scientia.custos.core.config.CustosMainProperties;
import io.github.abstruse_scientia.custos.core.engine.RateLimiterEngine;
import io.github.abstruse_scientia.custos.core.store.RateLimitStore;
import io.github.abstruse_scientia.custos.resolver.KeyResolverFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Spring Boot autoconfiguration.
 *
 */
@SpringBootTest
public class CustosAutoConfigurationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private CustosProperties custosProperties;

    @Autowired
    private CustosMainProperties custosMainProperties;

    /**
     * Test 1: AutoConfiguration Loads Beans
     * Verify Spring Boot autoconfiguration creates all required beans
     */
    @Test
    public void testAutoConfigurationBeansCreated() {
        // Verify key beans are created and wired
        assertThat(applicationContext.getBeanNamesForType(RateLimiterEngine.class)).isNotEmpty();
        assertThat(applicationContext.getBeanNamesForType(RateLimitStore.class)).isNotEmpty();
        assertThat(applicationContext.getBeanNamesForType(RateLimitAspect.class)).isNotEmpty();

        // Get beans and verify they're not null
        RateLimiterEngine engine = applicationContext.getBean(RateLimiterEngine.class);
        RateLimitAspect aspect = applicationContext.getBean(RateLimitAspect.class);
        RateLimitStore store = applicationContext.getBean(RateLimitStore.class);
        KeyResolverFactory factory = applicationContext.getBean(KeyResolverFactory.class);

        assertThat(engine).isNotNull();
        assertThat(aspect).isNotNull();
        assertThat(store).isNotNull();
        assertThat(factory).isNotNull();
    }

    /**
     * Test 2: Configuration Properties Loaded
     * Verify that configuration properties are correctly loaded from application.yml
     */
    @Test
    public void testConfigurationPropertiesLoaded() {
        assertThat(custosProperties).isNotNull();
        assertThat(custosMainProperties).isNotNull();
        
        // Verify token bucket configuration
        assertThat(custosProperties.getCapacity()).isGreaterThan(0);
        assertThat(custosProperties.getRate()).isGreaterThan(0);
        
        // Verify store type is configured
        assertThat(custosMainProperties.getStore()).isNotNull();
    }
}


