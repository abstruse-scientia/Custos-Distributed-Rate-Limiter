package com.abstruse.custos.aop;

import com.abstruse.custos.annotations.RateLimit;
import com.abstruse.custos.core.engine.RateLimiterEngine;
import com.abstruse.custos.core.model.RateLimitDecision;
import com.abstruse.custos.core.model.RequestContext;
import com.abstruse.custos.exception.RateLimitExceededException;
import com.abstruse.custos.resolver.KeyType;
import com.abstruse.custos.utility.CustosIPResolver;
import com.abstruse.custos.utility.CustosUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimiterEngine engine;
    private final CustosUserResolver userResolver;
    private final CustosIPResolver ipResolver;


    @Around("@annotation(rateLimit)")
    public Object enforceRateLimit(ProceedingJoinPoint proceedingJoinPoint, RateLimit rateLimit) throws Throwable {

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("Active ServletRequestAttributes required");
        }
        RequestContext requestContext = getRequestContext(attributes);


        RateLimitDecision decision = engine.allow(requestContext, rateLimit);

        if (!decision.allow()) {

            String key = rateLimit.keytype() == KeyType.USER ?
                    requestContext.getUserId() : requestContext.getIpAddress();
            throw new RateLimitExceededException(
                    key,
                    decision.retryAfterSeconds()
            );
        }
        return proceedingJoinPoint.proceed();
    }

    private RequestContext getRequestContext(ServletRequestAttributes attributes) {
        HttpServletRequest request = attributes.getRequest();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = userResolver.resolverUserId(request, authentication);
        String userIP = ipResolver.resolveClientIP(request);
        return new  RequestContext(userId, userIP);

    }



}
