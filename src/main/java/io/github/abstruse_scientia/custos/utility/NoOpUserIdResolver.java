package io.github.abstruse_scientia.custos.utility;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Default no operation UserIdResolver.
 * Always returns null, indicating no user is identified.
 * Used as fallback when:
 * - Spring Security is not on the classpath
 * - No custom UserIdResolver bean is registered
 * - User only wants IP-based rate limiting
 * If an application needs user based rate limiting, it must provide
 * a bean implementing UserIdResolver.
 */
public class NoOpUserIdResolver implements UserIdResolver {

    @Override
    public String getUserId(HttpServletRequest request) {
        return null; // No user identified
    }
}

