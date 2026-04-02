package resolver;

import annotations.Ratelimit;
import org.aspectj.lang.JoinPoint;

public class DefaultKeyResolver implements KeyResolver {
    @Override
    public String resolve(JoinPoint joinPoint, Ratelimit ratelimit) {
        if (ratelimit.key() != null && !ratelimit.key().isEmpty()) {
            return ratelimit.key();
        }
        return joinPoint.getSignature().toShortString();
    }
}
