package io.github.abstruse_scientia.custos.utility;

import jakarta.servlet.http.HttpServletRequest;

/*
 * This interface allows users to provide their own user ID extraction logic,
 * regardless of which security framework they use (Spring Security or Custom framework).
 *
 * Implementations should extract the user ID from:
 * - HTTP headers (X-User-Id, Authorization, etc.)
 * - Request attributes or cookies
 * - Session data
 * - Custom authentication context
 * - Database lookups
 */
public interface UserIdResolver {

    /**
     * Extract user ID from the current HTTP request.
     *
     * @param request The HTTP servlet request
     * @return The user ID (unique identifier), or null if no user is identified
     */
    String getUserId(HttpServletRequest request);
}

