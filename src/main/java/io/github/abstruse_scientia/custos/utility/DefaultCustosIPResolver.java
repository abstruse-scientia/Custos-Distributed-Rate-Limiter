package io.github.abstruse_scientia.custos.utility;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Default IP resolver implementation.
 * Tries to extract the client IP address in this order:
 * 1. X-Forwarded-For header (for reverse proxy/load balancer scenarios)
 * 2. X-Real-IP header (for nginx scenarios)
 * 3. request.getRemoteAddr() (direct connection)
 * This covers most common deployment scenarios without requiring custom implementation.
 */
public class DefaultCustosIPResolver implements CustosIPResolver {

    @Override
    public String resolveClientIP(HttpServletRequest request) {
        // 1. Try X-Forwarded-For (most common for proxies/load balancers)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can be comma-separated, take the first one (original client IP)
            return xForwardedFor.split(",")[0].trim();
        }

        // 2. Try X-Real-IP (nginx)
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }

        // 3. Fall back to direct connection IP
        return request.getRemoteAddr();
    }
}

