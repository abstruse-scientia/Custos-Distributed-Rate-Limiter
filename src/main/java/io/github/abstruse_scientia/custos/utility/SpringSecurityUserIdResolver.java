package io.github.abstruse_scientia.custos.utility;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Spring Security implementation of UserIdResolver.
 * This adapter extracts user id  from Spring Security's SecurityContext.
 * Users with Spring Security enabled can use this immediately.
 * Extraction order:
 * 1. Spring Security Authentication principal (UserDetails.username or principal string)
 * 2. Custom X-User-Id header
 * 3. Returns null if no user is identified
 */
public class SpringSecurityUserIdResolver implements UserIdResolver {

    @Override
    public String getUserId(HttpServletRequest request) {
        // Try Spring Security context first
        String userId = extractFromSecurityContext();
        if (userId != null) {
            return userId;
        }

        // Fallback to custom header
        userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }

        return null; // No user identified
    }

    private String extractFromSecurityContext() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }

            Object principal = authentication.getPrincipal();

            // Extract from UserDetails (standard Spring Security)
            if (principal instanceof UserDetails) {
                return ((UserDetails) principal).getUsername();
            }

            // If principal is a string (simple case)
            if (principal instanceof String) {
                return (String) principal;
            }

            // Fallback to authentication name
            return authentication.getName();
        } catch (Exception e) {
            // Spring Security not available or other error
            return null;
        }
    }
}

