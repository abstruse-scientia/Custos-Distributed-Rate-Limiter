package com.abstruse.custos.exception;

import com.abstruse.custos.annotations.RateLimit;

/**
 *  Thrown when configuration is invalid.
 */
public class RateLimitConfigurationException extends RuntimeException {

    public RateLimitConfigurationException(String message) {super(message);}

    public RateLimitConfigurationException(String message, Throwable cause) {super(message, cause);}
}
