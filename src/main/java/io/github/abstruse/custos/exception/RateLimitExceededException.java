package com.abstruse.custos.exception;

import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {
    private final String key;
    private final long retryAfterSeconds;
    public RateLimitExceededException(String key, long retryAfterSeconds) {
        super(String.format("Rate limit for key %s exceeded try after %s", key, retryAfterSeconds));
        this.key = key;
        this.retryAfterSeconds = retryAfterSeconds;
    }

}
