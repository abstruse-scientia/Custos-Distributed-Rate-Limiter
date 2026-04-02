package aspect;

import core.RateLimiter;
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




}
