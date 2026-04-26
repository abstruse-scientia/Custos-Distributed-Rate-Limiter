package io.github.abstruse_scientia.custos.integration.resolver;

import io.github.abstruse_scientia.custos.core.model.RequestContext;
import io.github.abstruse_scientia.custos.resolver.KeyResolver;
import io.github.abstruse_scientia.custos.resolver.KeyResolverFactory;
import io.github.abstruse_scientia.custos.resolver.KeyType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Key Resolver Factory.
 * <p>Verifies factory selects correct resolver based on KeyType.</p>
 */
@SpringBootTest
public class KeyResolverFactoryIntegrationTest {

    @Autowired
    private KeyResolverFactory factory;

    /**
     * Test Key: Resolver Selection
     * <p>Verify factory correctly selects resolver based on KeyType</p>
     */
    @Test
    public void testKeyResolverSelection() {
        KeyResolver userResolver = factory.getKeyResolver(KeyType.USER);
        assertThat(userResolver).isNotNull();
        assertThat(userResolver.getKeyType()).isEqualTo(KeyType.USER);

        KeyResolver ipResolver = factory.getKeyResolver(KeyType.IP);
        assertThat(ipResolver).isNotNull();
        assertThat(ipResolver.getKeyType()).isEqualTo(KeyType.IP);
    }

    /**
     * Test: User Key Resolver Integration
     * <p>Verify USER KeyType resolver works correctly</p>
     */
    @Test
    public void testUserKeyResolverIntegration() {
        KeyResolver userResolver = factory.getKeyResolver(KeyType.USER);
        
        RequestContext context = new RequestContext("user123", "192.168.1.1");
        String resolvedKey = userResolver.resolve(context);

        assertThat(resolvedKey).isEqualTo("user123");
    }

    /**
     * Test: IP Key Resolver Integration
     * <p>Verify IP KeyType resolver works correctly</p>
     */
    @Test
    public void testIPKeyResolverIntegration() {
        KeyResolver ipResolver = factory.getKeyResolver(KeyType.IP);
        
        RequestContext context = new RequestContext("user123", "192.168.1.1");
        String resolvedKey = ipResolver.resolve(context);

        assertThat(resolvedKey).isEqualTo("192.168.1.1");
    }

    /**
     * Test: Factory with Valid Keys
     * <p>Verify factory can resolve both key types</p>
     */
    @Test
    public void testFactoryWithValidKeys() {
        KeyResolver userResolver = factory.getKeyResolver(KeyType.USER);
        KeyResolver ipResolver = factory.getKeyResolver(KeyType.IP);

        assertThat(userResolver).isNotNull();
        assertThat(ipResolver).isNotNull();
    }

    /**
     * Test: Multiple Resolver Instances
     * <p>Verify factory returns same resolver instances consistently</p>
     *
     */
    @Test
    public void testConsistentResolverInstances() {
        KeyResolver userResolver1 = factory.getKeyResolver(KeyType.USER);
        KeyResolver userResolver2 = factory.getKeyResolver(KeyType.USER);

        assertThat(userResolver1).isSameAs(userResolver2);

        KeyResolver ipResolver1 = factory.getKeyResolver(KeyType.IP);
        KeyResolver ipResolver2 = factory.getKeyResolver(KeyType.IP);

        assertThat(ipResolver1).isSameAs(ipResolver2);
    }

    /**
     * Test: Different Resolvers Return Different Keys
     * <p>Verify resolvers return different keys for same context</p>
     *
     */
    @Test
    public void testResolversDifferentiation() {
        KeyResolver userResolver = factory.getKeyResolver(KeyType.USER);
        KeyResolver ipResolver = factory.getKeyResolver(KeyType.IP);

        RequestContext context = new RequestContext("john", "10.0.0.1");

        String userKey = userResolver.resolve(context);
        String ipKey = ipResolver.resolve(context);

        assertThat(userKey).isNotEqualTo(ipKey);
        assertThat(userKey).isEqualTo("john");
        assertThat(ipKey).isEqualTo("10.0.0.1");
    }
}


