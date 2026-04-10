package io.github.abstruse_scientia.custos.core.model;

public record RateLimitDecision(boolean allow, long retryAfterSeconds) {
}
