package io.github.abstruse_scientia.custos.resolver;

import io.github.abstruse_scientia.custos.core.model.RequestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Tests 1-2: Custom resolver implementation and failure handling.
 */
class CustomKeyResolverUnitTest {

    /**
     * Test 1: Custom Resolver Implementation
     * <p>Setup: Implement KeyResolver returning custom key (e.g., account_id)</p>
     * <p>Verify: Custom resolver invoked, returns expected key</p>
     */
    @Test
    void testCustomResolverImplementation() {
        // Arrange: Create a custom resolver that returns account_id as key
        KeyResolver accountIdResolver = new KeyResolver() {
            @Override
            public KeyType getKeyType() {
                return KeyType.USER; // Using USER as placeholder
            }

            @Override
            public String resolve(RequestContext context) {
                return "account_" + context.getUserId();
            }
        };

        RequestContext context = new RequestContext("user789", "192.168.1.50");

        // Act
        String resolvedKey = accountIdResolver.resolve(context);

        // Assert
        assertThat(resolvedKey).isEqualTo("account_user789");
        assertThat(accountIdResolver.getKeyType()).isEqualTo(KeyType.USER);
    }

    /**
     * Test 1.1: Custom resolver based on IP with tenant prefix
     */
    @Test
    void testCustomResolverWithTenantPrefix() {
        KeyResolver tenantResolver = new KeyResolver() {
            @Override
            public KeyType getKeyType() {
                return KeyType.IP;
            }

            @Override
            public String resolve(RequestContext context) {
                String ip = context.getIpAddress();
                if (ip.startsWith("10.")) {
                    return "internal_" + ip;
                }
                return "external_" + ip;
            }
        };

        // Internal IP
        RequestContext internalContext = new RequestContext("user1", "10.0.0.50");
        assertThat(tenantResolver.resolve(internalContext)).isEqualTo("internal_10.0.0.50");

        // External IP
        RequestContext externalContext = new RequestContext("user2", "203.0.113.5");
        assertThat(tenantResolver.resolve(externalContext)).isEqualTo("external_203.0.113.5");
    }

    /**
     * Test 1.3: Custom resolver using composite key (user + IP)
     */
    @Test
    void testCustomResolverCompositeKey() {
        KeyResolver compositeResolver = new KeyResolver() {
            @Override
            public KeyType getKeyType() {
                return KeyType.USER;
            }

            @Override
            public String resolve(RequestContext context) {
                return context.getUserId() + ":" + context.getIpAddress();
            }
        };

        RequestContext context = new RequestContext("admin", "192.168.1.100");
        String key = compositeResolver.resolve(context);
        assertThat(key).isEqualTo("admin:192.168.1.100");
    }

    /**
     * Test 2: Custom Resolver Failure Handling
     * <p>Setup: Custom resolver throws exception</p>
     * <p>Verify: Exception is properly propagated (caller should handle)</p>
     */
    @Test
    void testCustomResolverFailureFallback() {
        // Arrange: Create a resolver that throws an exception
        KeyResolver failingResolver = new KeyResolver() {
            @Override
            public KeyType getKeyType() {
                return KeyType.USER;
            }

            @Override
            public String resolve(RequestContext context) {
                if (context.getUserId() == null) {
                    throw new IllegalArgumentException("User ID cannot be null");
                }
                return context.getUserId();
            }
        };

        RequestContext nullUserContext = new RequestContext(null, "192.168.1.1");

        // Act & Assert: Exception is thrown and propagated
        assertThatThrownBy(() -> failingResolver.resolve(nullUserContext))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User ID cannot be null");
    }

    /**
     * Test 2.1: Custom resolver failure with graceful fallback pattern
     */
    @Test
    void testCustomResolverGracefulFallback() {
        // Arrange: Create a resolver with try-catch and fallback
        KeyResolver resilientResolver = new KeyResolver() {
            @Override
            public KeyType getKeyType() {
                return KeyType.USER;
            }

            @Override
            public String resolve(RequestContext context) {
                try {
                    String userId = context.getUserId();
                    if (userId == null || userId.isEmpty()) {
                        throw new RuntimeException("Failed to resolve user");
                    }
                    return "resolved_" + userId;
                } catch (Exception e) {
                    // Graceful fallback to IP-based key
                    return "fallback_" + context.getIpAddress();
                }
            }
        };

        // Test normal case
        RequestContext normalContext = new RequestContext("user123", "192.168.1.1");
        assertThat(resilientResolver.resolve(normalContext)).isEqualTo("resolved_user123");

        // Test failure case — falls back to IP
        RequestContext failContext = new RequestContext(null, "10.0.0.1");
        assertThat(resilientResolver.resolve(failContext)).isEqualTo("fallback_10.0.0.1");
    }

    /**
     * Test 2.3: KeyResolverFactory throws for unknown KeyType
     */
    @Test
    @DisplayName("Test 34c: KeyResolverFactory throws for unregistered KeyType")
    void testKeyResolverFactoryThrowsForUnknown() {
        // Arrange: Factory with only USER resolver registered
        KeyResolverFactory factory = new KeyResolverFactory(
                java.util.List.of(new UserKeyResolver())
        );

        // Act: Request IP resolver which is not registered
        Throwable thrown = catchThrowable(() -> factory.getKeyResolver(KeyType.IP));

        // Assert: Should throw IllegalArgumentException
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No KeyResolver found for KeyType: IP");
    }
}
