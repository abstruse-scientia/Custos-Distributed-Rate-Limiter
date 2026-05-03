package io.github.abstruse_scientia.custos.resolver;

import io.github.abstruse_scientia.custos.core.model.RequestContext;
import io.github.abstruse_scientia.custos.utility.DefaultCustosIPResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>Tests 1-3: IP extraction from HttpServletRequest, X-Forwarded-For handling,
 * and IPv6 support.</p>
 */
class IPKeyResolverTest {

    private IPKeyResolver ipKeyResolver;
    private DefaultCustosIPResolver ipResolver;

    @BeforeEach
    void setUp() {
        ipKeyResolver = new IPKeyResolver();
        ipResolver = new DefaultCustosIPResolver();
    }

    /**
     * Test 1: Extract IP from HttpServletRequest
     * <p>Setup: HTTP request with direct IP (no proxy headers)</p>
     * <p>Verify: IP correctly extracted as key</p>
     */
    @Test
    @DisplayName("Test 30: Extract IP from direct connection")
    void testIPExtraction() {
        // Arrange: Direct connection request with remoteAddr
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.1");

        // Act: Resolve IP using DefaultCustosIPResolver
        String resolvedIP = ipResolver.resolveClientIP(request);

        // Create RequestContext with the resolved IP
        RequestContext context = new RequestContext("user123", resolvedIP);
        String key = ipKeyResolver.resolve(context);

        // Assert: IP extracted correctly from direct connection
        assertThat(key).isEqualTo("203.0.113.1");
        assertThat(ipKeyResolver.getKeyType()).isEqualTo(KeyType.IP);
    }

    /**
     * Test 1.1: Extract IP from X-Real-IP header (nginx)
     */
    @Test
    void testIPExtractionFromXRealIP() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1"); // proxy localhost
        request.addHeader("X-Real-IP", "198.51.100.42");

        String resolvedIP = ipResolver.resolveClientIP(request);

        assertThat(resolvedIP).isEqualTo("198.51.100.42");
    }

    /**
     * Test 2: Handle X-Forwarded-For Header
     * <p>Setup: Request with X-Forwarded-For: 192.168.1.1, 10.0.0.1</p>
     * <p>Verify: First IP (client) is extracted (not proxy IPs)</p>
     */
    @Test
    void testXForwardedForHandling() {
        // Arrange: Request behind a proxy with multiple forwarded IPs
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1"); // proxy IP
        request.addHeader("X-Forwarded-For", "192.168.1.1, 10.0.0.1");

        // Act: Resolve should extract first IP (original client)
        String resolvedIP = ipResolver.resolveClientIP(request);

        // Assert: First IP from X-Forwarded-For chain is the client IP
        assertThat(resolvedIP).isEqualTo("192.168.1.1");

        // Verify through IPKeyResolver chain
        RequestContext context = new RequestContext("user1", resolvedIP);
        String key = ipKeyResolver.resolve(context);
        assertThat(key).isEqualTo("192.168.1.1");
    }

    /**
     * Test 2.1: Handle X-Forwarded-For with single IP
     */
    @Test
    void testXForwardedForSingleIP() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "10.20.30.40");

        String resolvedIP = ipResolver.resolveClientIP(request);
        assertThat(resolvedIP).isEqualTo("10.20.30.40");
    }

    /**
     * Test 2.2: X-Forwarded-For takes priority over X-Real-IP
     */
    @Test
    void testXForwardedForPriority() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "192.168.1.1, 10.0.0.1");
        request.addHeader("X-Real-IP", "172.16.0.1");

        String resolvedIP = ipResolver.resolveClientIP(request);

        // X-Forwarded-For should take priority
        assertThat(resolvedIP).isEqualTo("192.168.1.1");
    }

    /**
     * Test 3: IPv6 Support
     * <p>Setup: Request with IPv6 address 2001:db8::1</p>
     * <p>Verify: IPv6 correctly parsed and used as key</p>
     */
    @Test
    void testIPv6Support() {
        // Arrange: Request with IPv6 address
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("2001:db8::1");

        // Act
        String resolvedIP = ipResolver.resolveClientIP(request);
        RequestContext context = new RequestContext("user1", resolvedIP);
        String key = ipKeyResolver.resolve(context);

        // Assert: IPv6 address correctly extracted
        assertThat(key).isEqualTo("2001:db8::1");
    }

    /**
     * Test 3.1: IPv6 in X-Forwarded-For
     */
    @Test
    @DisplayName("Test 32b: IPv6 in X-Forwarded-For header")
    void testIPv6InXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "2001:db8::1, ::ffff:192.168.1.1");

        String resolvedIP = ipResolver.resolveClientIP(request);
        assertThat(resolvedIP).isEqualTo("2001:db8::1");
    }

    /**
     * Test 3.2: IPv6 loopback address
     */
    @Test
    @DisplayName("Test 32c: IPv6 loopback address")
    void testIPv6Loopback() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("::1");

        String resolvedIP = ipResolver.resolveClientIP(request);
        assertThat(resolvedIP).isEqualTo("::1");
    }
}
