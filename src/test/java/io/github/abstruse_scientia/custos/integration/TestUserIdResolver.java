package io.github.abstruse_scientia.custos.integration;

import io.github.abstruse_scientia.custos.utility.UserIdResolver;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Test implementation of UserIdResolver that extracts userId from X-User-Id header.
 * This ensures rate limiting tests work correctly by providing consistent user IDs.
 */
public class TestUserIdResolver implements UserIdResolver {

    @Override
    public String getUserId(HttpServletRequest request) {
        // Try to get userId from X-User-Id header
        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }

        // Fallback to a default test user if header not provided
        return "test-user";
    }
}

