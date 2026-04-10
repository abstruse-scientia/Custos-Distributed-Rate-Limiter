package com.abstruse.custos.utility;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

/**
 * Functional interface: Interface with only one abstract method to be implemented.
 * Tries to extract user id in different deployment scenarios:
 * 1. Through Spring Security context (Authentication object)
 * 2. Through custom request headers (X-User-Id, Authorization, etc.)
 */
public interface CustosUserResolver {

    /**
     * Resolves the user ID from HttpServletRequest and Authentication.
     *
     * Developers can implement custom logic to extract user ID from:
     * - Spring Security Authentication (JWT claims, UserDetails, etc.)
     * - Custom headers (X-User-Id, X-Api-Key, etc.)
     * - Request parameters or cookies
     * - Database lookups
     *
     * @param request : HttpServletRequest with headers and request data
     * @param authentication : Spring Security Authentication (can be null for unauthenticated requests)
     * @return A unique string representing user (e.g., userId, email, opaque string)
     */
    String resolverUserId(HttpServletRequest request, Authentication authentication);
}
