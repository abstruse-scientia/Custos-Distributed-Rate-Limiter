package aspect;

import annotations.Ratelimit;
import core.RateLimiter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import resolver.KeyResolver;

@Aspect
@Component
public class RateLimitAspect {

    private final RateLimiter rateLimiter;
    private final KeyResolver keyResolver;

    public RateLimitAspect(RateLimiter rateLimiter, KeyResolver keyResolver) {
        this.rateLimiter = rateLimiter;
        this.keyResolver = keyResolver;
    }

    @Around("@annotation(ratelimit)")
    public Object enforceRateLimit(ProceedingJoinPoint proceedingJoinPoint, Ratelimit ratelimit) throws Throwable {

        String key = keyResolver.resolve(proceedingJoinPoint, ratelimit);

        boolean allowed = rateLimiter.isAllowed(ratelimit.capacity(),ratelimit.refillRate(), key);

        if (!allowed) {
            throw new RuntimeException("Rate limited exceeded for: " + key);
        }
        return proceedingJoinPoint.proceed();
    }



}
