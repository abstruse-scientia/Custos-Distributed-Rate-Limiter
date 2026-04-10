package com.abstruse.custos.utility;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Functional interface: Interface with only one abstract method to be implemented.
 * Used to extract the client IP address from an HttpServletRequest.
 * Different deployment environments may need different IP extraction strategies:
 * - Behind a proxy: X-Forwarded-For header
 * - Behind nginx: X-Real-IP header
 * - Behind Cloudflare: CF-Connecting-IP header
 * - Direct connection: request.getRemoteAddr()
 */
public interface CustosIPResolver {

    /**
     * Resolves the client IP address from the HTTP request.
     * 
     * @param request : HttpServletRequest object
     * @return A string representing the client IP address
     */
    String resolveClientIP(HttpServletRequest request);
}

