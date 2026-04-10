package com.abstruse.custos.core.autoconfiguration;

import com.abstruse.custos.core.config.ConfigResolver;
import com.abstruse.custos.core.config.CustosMainProperties;
import com.abstruse.custos.core.config.CustosProperties;
import com.abstruse.custos.core.engine.RateLimiterEngine;
import com.abstruse.custos.core.store.InMemoryStore;
import com.abstruse.custos.core.store.RateLimitStore;
import com.abstruse.custos.core.strategy.RateLimiterStrategy;
import com.abstruse.custos.core.strategy.StrategyFactory;
import com.abstruse.custos.core.strategy.TokenBucketStrategy;
import com.abstruse.custos.core.strategy.redis.RedisTokenBucketStrategy;
import com.abstruse.custos.resolver.IPKeyResolver;
import com.abstruse.custos.resolver.KeyResolver;
import com.abstruse.custos.resolver.KeyResolverFactory;
import com.abstruse.custos.resolver.UserKeyResolver;
import com.abstruse.custos.utility.CustosUserResolver;
import com.abstruse.custos.utility.CustosIPResolver;
import com.abstruse.custos.utility.DefaultCustosIPResolver;
import com.abstruse.custos.utility.DefaultCustosUserResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties({CustosProperties.class, CustosMainProperties.class})

public class CustosAutoConfiguration {

        // ---------- USER RESOLVER ----


        /**
         * Runs only if user does not provide their own resolver.
         * Provides defaults for common deployment scenarios
         * - Spring Security Context holder holding User
         * - Reading header request
         */
        @Bean
        @ConditionalOnMissingBean(CustosUserResolver.class)
        public CustosUserResolver  custosUserResolver() {
            return new DefaultCustosUserResolver();
        }

        // ---------- IP RESOLVER ----

        /**
         * Runs only if user does not provide their own resolver.
         * Provides sensible defaults for common deployment scenarios:
         * - Proxy/Load Balancer: X-Forwarded-For
         * - Nginx: X-Real-IP
         * - Direct Connection: request.getRemoteAddr()
         */
        @Bean
        @ConditionalOnMissingBean(CustosIPResolver.class)
        public CustosIPResolver custosIPResolver() {
            return new DefaultCustosIPResolver();
        }

        // ---------- KEY RESOLVERS ----------

        @Bean
        @ConditionalOnMissingBean
        public KeyResolver userResolver() {
            return new UserKeyResolver();
        }

        @Bean
        @ConditionalOnMissingBean
        public KeyResolver ipResolver() {
            return new IPKeyResolver();
        }

        @Bean
        @ConditionalOnMissingBean
        public KeyResolverFactory resolverFactory(List<KeyResolver> resolvers) {
            return new KeyResolverFactory(resolvers);
        }

        // ---------- STRATEGY (CONDITIONAL) ----------

        @Bean
        @ConditionalOnProperty(
                name = "custos.store",
                havingValue = "memory",
                matchIfMissing = true
        )
        @ConditionalOnMissingBean
        public RateLimiterStrategy inMemoryTokenBucket() {
            return new TokenBucketStrategy();
        }

        @Bean
        @ConditionalOnProperty(name = "custos.store", havingValue = "redis")
        @ConditionalOnClass(StringRedisTemplate.class)
        @ConditionalOnMissingBean
        public RateLimiterStrategy redisTokenBucket(StringRedisTemplate redisTemplate) {
            return new RedisTokenBucketStrategy(redisTemplate);
        }

        // ---------- STRATEGY FACTORY ----------

        @Bean
        @ConditionalOnMissingBean
        public StrategyFactory strategyFactory(List<RateLimiterStrategy> strategies) {
            return new StrategyFactory(strategies);
        }

        // ---------- CONFIG ----------

        @Bean
        @ConditionalOnMissingBean
        public ConfigResolver configResolver(CustosProperties props) {
            return new ConfigResolver(props);
        }

        //  ---------- STORE ----------
        @Bean
        @ConditionalOnMissingBean
        public RateLimitStore store() {return new InMemoryStore();}

        // ---------- ENGINE ----------

        @Bean
        @ConditionalOnMissingBean
        public RateLimiterEngine engine(
                KeyResolverFactory resolverFactory,
                ConfigResolver configResolver,
                StrategyFactory strategyFactory,
                RateLimitStore store
        ) {
            return new RateLimiterEngine(

                    resolverFactory,
                    configResolver,
                    strategyFactory,
                    store
            );
        }
}

