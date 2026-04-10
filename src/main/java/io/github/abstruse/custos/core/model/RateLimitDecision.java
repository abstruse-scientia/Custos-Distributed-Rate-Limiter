package com.abstruse.custos.core.model;

public record RateLimitDecision(boolean allow, long retryAfterSeconds) {
}
