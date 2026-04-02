package resolver;

import annotations.Ratelimit;
import org.aspectj.lang.JoinPoint;

public interface KeyResolver {

    /**
     *
     * @param joinPoint: contains details about the method being called.
     * @param ratelimit: annotation containing instruction
     * @return A unique string identifier.
     */
    String resolve(JoinPoint joinPoint, Ratelimit ratelimit);
}
