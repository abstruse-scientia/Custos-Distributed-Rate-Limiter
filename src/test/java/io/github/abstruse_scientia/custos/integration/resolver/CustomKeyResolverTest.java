package io.github.abstruse_scientia.custos.integration.resolver;

import io.github.abstruse_scientia.custos.core.model.RequestContext;
import io.github.abstruse_scientia.custos.resolver.KeyResolver;
import io.github.abstruse_scientia.custos.resolver.KeyType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Primary;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Custom Key Resolver.
 *<p>Verifies custom KeyResolver is properly picked from Spring context.</p>
 */
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
public class CustomKeyResolverTest {

    /**
     * Custom test configuration that defines a custom resolver
     */
    @TestConfiguration
    static class CustomResolverConfig {
        
        @Bean("userKeyResolver")
        @Primary
        public KeyResolver accountIdResolver() {
            return new KeyResolver() {
                @Override
                public KeyType getKeyType() {
                    return KeyType.USER; // Using existing enum value as placeholder
                }

                @Override
                public String resolve(RequestContext context) {
                    // Custom resolution logic based on a hypothetical account ID
                    return "account_" + context.getUserId();
                }
            };
        }
    }

    @Autowired(required = false)
    private KeyResolver accountIdResolver;

    /**
     * Test: Custom Resolver in Spring Context
     * <p>Verify custom resolver is properly injected by Spring</p>
     */
    @Test
    public void testCustomResolverInjection() {
        assertThat(accountIdResolver).isNotNull();
    }

    /**
     * Test: Custom Resolver Resolution Logic
     * <p>Verify custom resolver produces expected output</p>

     */
    @Test
    public void testCustomResolverResolution() {
        RequestContext context = new RequestContext("user456", "192.168.0.1");
        String resolvedKey = accountIdResolver.resolve(context);

        assertThat(resolvedKey).startsWith("account_");
        assertThat(resolvedKey).isEqualTo("account_user456");
    }

    /**
     * Test: Custom Resolver Key Type
     * <p>Verify custom resolver defines correct KeyType</p>
     */
    @Test
    public void testCustomResolverKeyType() {
        KeyType keyType = accountIdResolver.getKeyType();
        
        assertThat(keyType).isNotNull();
        assertThat(keyType).isEqualTo(KeyType.USER);
    }

    /**
     * Test: Custom Resolver with Complex Logic
     * <p>Verify custom resolver can implement complex resolution logic</p>
     */
    @Test
    public void testComplexCustomResolverLogic() {
        RequestContext context = new RequestContext("premium_user_789", "203.0.113.5");
        
        KeyResolver complexResolver = new KeyResolver() {
            @Override
            public KeyType getKeyType() {
                return KeyType.IP;
            }

            @Override
            public String resolve(RequestContext context) {
                String userId = context.getUserId();
                if (userId.startsWith("premium_")) {
                    return "tier_premium";
                } else if (userId.startsWith("free_")) {
                    return "tier_free";
                }
                return "tier_standard";
            }
        };

        String resolvedKey = complexResolver.resolve(context);
        assertThat(resolvedKey).isEqualTo("tier_premium");
    }

    /**
     * Test: Custom Resolver Handles Null Context Gracefully
     * <p>Verify custom resolver is robust to edge cases</p>
     */
    @Test
    public void testCustomResolverNullHandling() {
        RequestContext nullUserContext = new RequestContext(null, "192.168.0.1");
        
        // Custom resolver should handle null userId gracefully
        String resolvedKey = accountIdResolver.resolve(nullUserContext);
        assertThat(resolvedKey).isEqualTo("account_null");
    }
}


