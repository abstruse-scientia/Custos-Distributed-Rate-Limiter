package io.github.abstruse_scientia.custos.core.autoconfiguration;

import io.github.abstruse_scientia.custos.core.config.ConfigResolver;
import io.github.abstruse_scientia.custos.core.config.CustosMainProperties;
import io.github.abstruse_scientia.custos.core.config.CustosProperties;
import io.github.abstruse_scientia.custos.core.engine.RateLimiterEngine;
import io.github.abstruse_scientia.custos.core.store.InMemoryStore;
import io.github.abstruse_scientia.custos.core.store.RateLimitStore;
import io.github.abstruse_scientia.custos.core.strategy.RateLimiterStrategy;
import io.github.abstruse_scientia.custos.core.strategy.StrategyFactory;
import io.github.abstruse_scientia.custos.core.strategy.TokenBucketStrategy;
import io.github.abstruse_scientia.custos.core.strategy.SlidingWindowStrategy;
import io.github.abstruse_scientia.custos.core.strategy.LeakyBucketStrategy;
import io.github.abstruse_scientia.custos.core.strategy.redis.RedisTokenBucketStrategy;
import io.github.abstruse_scientia.custos.core.strategy.redis.RedisSlidingWindowStrategy;
import io.github.abstruse_scientia.custos.core.strategy.redis.RedisLeakyBucketStrategy;
import io.github.abstruse_scientia.custos.resolver.IPKeyResolver;
import io.github.abstruse_scientia.custos.resolver.KeyResolver;
import io.github.abstruse_scientia.custos.resolver.KeyResolverFactory;
import io.github.abstruse_scientia.custos.resolver.UserKeyResolver;
import io.github.abstruse_scientia.custos.utility.*;
import io.github.abstruse_scientia.custos.utility.UserIdResolver;
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

        // ---------- USER ID PROVIDER (Framework Agnostic) ----

        /**
         * Spring Security implementation of UserIdResolver.
         * Runs only if Spring Security is on the classpath.
         */
        @Bean
        @ConditionalOnClass(name = "org.springframework.security.core.context.SecurityContextHolder")
        @ConditionalOnMissingBean(UserIdResolver.class)
        public UserIdResolver springSecurityUserIdProvider() {
            return new SpringSecurityUserIdResolver();
        }

        /**
         * Fallback no-op UserIdResolver.
         * Runs only if:
         * - Spring Security is NOT on the classpath
         * - User has not provided their own UserIdResolver bean
         * - User only wants IP-based rate limiting
         */
        @Bean
        @ConditionalOnMissingBean(UserIdResolver.class)
        public UserIdResolver noOpUserIdProvider() {
            return new NoOpUserIdResolver();
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

        // ---------- SLIDING WINDOW STRATEGIES ----------

        @Bean
        @ConditionalOnProperty(
                name = "custos.store",
                havingValue = "memory",
                matchIfMissing = true
        )
        @ConditionalOnMissingBean
        public RateLimiterStrategy inMemorySlidingWindow() {
            return new SlidingWindowStrategy();
        }

        @Bean
        @ConditionalOnProperty(name = "custos.store", havingValue = "redis")
        @ConditionalOnClass(StringRedisTemplate.class)
        @ConditionalOnMissingBean
        public RateLimiterStrategy redisSlidingWindow(StringRedisTemplate redisTemplate) {
            return new RedisSlidingWindowStrategy(redisTemplate);
        }

        // ---------- LEAKY BUCKET STRATEGIES ----------

        @Bean
        @ConditionalOnProperty(
                name = "custos.store",
                havingValue = "memory",
                matchIfMissing = true
        )
        @ConditionalOnMissingBean
        public RateLimiterStrategy inMemoryLeakyBucket() {
            return new LeakyBucketStrategy();
        }

        @Bean
        @ConditionalOnProperty(name = "custos.store", havingValue = "redis")
        @ConditionalOnClass(StringRedisTemplate.class)
        @ConditionalOnMissingBean
        public RateLimiterStrategy redisLeakyBucket(StringRedisTemplate redisTemplate) {
            return new RedisLeakyBucketStrategy(redisTemplate);
        }

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
        @ConditionalOnProperty(name = "custos.store", havingValue = "memory",  matchIfMissing = true)
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

