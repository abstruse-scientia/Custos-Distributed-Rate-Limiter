package io.github.abstruse_scientia.custos.resolver;

import io.github.abstruse_scientia.custos.core.model.RequestContext;
import io.github.abstruse_scientia.custos.utility.SpringSecurityUserIdResolver;
import io.github.abstruse_scientia.custos.utility.UserIdResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests 1-3: User key resolution from Spring Security, anonymous fallback,
 * and custom UserIdResolver.
 */
class UserKeyResolverTest {

    private UserKeyResolver userKeyResolver;

    @BeforeEach
    void setUp() {
        userKeyResolver = new UserKeyResolver();
        // Clear security context before each test
        SecurityContextHolder.clearContext();
    }

    /**
     * Test 1: Extract User from Spring Security
     * <p>Setup: Authenticated request with Spring Security principal</p>
     * <p>Verify: User principal extracted as key correctly</p>
     */
    @Test
    void testUserExtractionFromSecurity() {
        // Arrange: Set up Spring Security context with an authenticated user
        UserDetails userDetails = User.builder()
                .username("authenticated-user-123")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Use SpringSecurityUserIdResolver to extract the userId
        SpringSecurityUserIdResolver springResolver = new SpringSecurityUserIdResolver();
        MockHttpServletRequest request = new MockHttpServletRequest();
        String resolvedUserId = springResolver.getUserId(request);

        // Build a RequestContext with the resolved userId
        RequestContext context = new RequestContext(resolvedUserId, "192.168.1.1");

        // Act: Resolve the key using UserKeyResolver
        String key = userKeyResolver.resolve(context);

        // Assert
        assertThat(key).isEqualTo("authenticated-user-123");
        assertThat(userKeyResolver.getKeyType()).isEqualTo(KeyType.USER);
    }

    /**
     * Test 2: Handle Anonymous User
     * <p>Setup: Unauthenticated request (no principal)</p>
     * <p>Verify: Falls back to null (no user identified) - handled by NoOpUserIdResolver</p>
     */
    @Test
    @DisplayName("Test 28: Handle anonymous user fallback")
    void testAnonymousUserFallback() {
        // Arrange: No authentication set → SecurityContext is empty
        SpringSecurityUserIdResolver springResolver = new SpringSecurityUserIdResolver();
        MockHttpServletRequest request = new MockHttpServletRequest();

        // Act: Resolve userId   should return null for anonymous user
        String resolvedUserId = springResolver.getUserId(request);

        // Assert: No user identified for anonymous request
        assertThat(resolvedUserId).isNull();

        // Verify that when null userId is passed into RequestContext, UserKeyResolver returns null
        RequestContext context = new RequestContext(resolvedUserId, "192.168.1.1");
        String key = userKeyResolver.resolve(context);
        assertThat(key).isNull();
    }

    /**
     * Test 2.1: Handle anonymous user with X-User-Id header fallback
     * <p>Setup: Unauthenticated request but X-User-Id header present</p>
     * <p>Verify: Falls back to header based userId</p>
     */
    @Test
    @DisplayName("Test 28b: Handle anonymous user with X-User-Id header fallback")
    void testAnonymousUserFallbackWithHeader() {
        // Arrange: No Security authentication, but X-User-Id header present
        SpringSecurityUserIdResolver springResolver = new SpringSecurityUserIdResolver();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "header-user-456");

        // Act
        String resolvedUserId = springResolver.getUserId(request);

        // Assert: Should fall back to X-User-Id header
        assertThat(resolvedUserId).isEqualTo("header-user-456");
    }

    /**
     * Test 3: Custom UserIdResolver Implementation
     * <p>Setup: Custom UserIdResolver bean provided</p>
     * <p>Verify: Custom resolver called, returns expected user ID</p>
     */
    @Test
    void testCustomUserIdResolver() {
        // Arrange: Create a custom UserIdResolver that extracts from a custom header
        UserIdResolver customResolver = new UserIdResolver() {
            @Override
            public String getUserId(HttpServletRequest request) {
                String apiKey = request.getHeader("X-API-Key");
                if (apiKey != null && !apiKey.isEmpty()) {
                    return "api_user_" + apiKey;
                }
                return "default-api-user";
            }
        };

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-Key", "abc123");

        // Act: Use custom resolver
        String resolvedUserId = customResolver.getUserId(request);

        // Assert: Custom resolver returns expected transformed user ID
        assertThat(resolvedUserId).isEqualTo("api_user_abc123");

        // Verify it works through the UserKeyResolver chain
        RequestContext context = new RequestContext(resolvedUserId, "10.0.0.1");
        String key = userKeyResolver.resolve(context);
        assertThat(key).isEqualTo("api_user_abc123");
    }

    /**
     * Test 3.1: Custom UserIdResolver with no API key falls back to default
     */
    @Test
    void testCustomUserIdResolverFallback() {
        UserIdResolver customResolver = new UserIdResolver() {
            @Override
            public String getUserId(HttpServletRequest request) {
                String apiKey = request.getHeader("X-API-Key");
                if (apiKey != null && !apiKey.isEmpty()) {
                    return "api_user_" + apiKey;
                }
                return "default-api-user";
            }
        };

        MockHttpServletRequest request = new MockHttpServletRequest();
        // No API key header

        String resolvedUserId = customResolver.getUserId(request);
        assertThat(resolvedUserId).isEqualTo("default-api-user");
    }
}
