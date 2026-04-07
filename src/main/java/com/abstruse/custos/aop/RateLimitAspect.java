package com.abstruse.custos.aop;

import com.abstruse.custos.annotations.RateLimit;
import com.abstruse.custos.core.engine.RateLimiterEngine;
import com.abstruse.custos.core.model.RequestContext;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimiterEngine engine;



    @Around("@annotation(rateLimit)")
    public Object enforceRateLimit(ProceedingJoinPoint proceedingJoinPoint, RateLimit rateLimit) throws Throwable {

        RequestContext requestContext = new RequestContext("test-key");

        boolean allowed = engine.allow(requestContext, rateLimit);

        if (!allowed) {
            throw new RuntimeException("Rate limited exceeded for");
        }
        return proceedingJoinPoint.proceed();
    }



}
