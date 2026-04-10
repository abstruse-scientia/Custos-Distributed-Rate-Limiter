package io.github.abstruse_scientia.custos.utility;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Default IP resolver implementation.
 * Tries to extract the client User id in this order:
 * 1. Try Spring Security (if logged in)
 * 2. Try for custom headers (for API calls, logged in or not)
 * This covers most common deployment scenarios without requiring custom implementation.
 */
public class DefaultCustosUserResolver implements  CustosUserResolver{
    @Override
    public String resolverUserId(HttpServletRequest request,  Authentication authentication) {

        // 1. Try spring security
        String userId = trySpring();
        if (userId != null)  {return userId;}

        // 2. Custom header
        userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isEmpty() ){
            return userId;
        }

        // FallBack to anonymous
        return "unknown";

    }

    private String trySpring(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        if (principal instanceof String s) {
            return s;
        }

        return authentication.getName();
    }
}
