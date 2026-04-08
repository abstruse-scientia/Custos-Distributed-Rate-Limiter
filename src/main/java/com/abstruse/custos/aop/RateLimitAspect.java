package com.abstruse.custos.aop;

import com.abstruse.custos.annotations.RateLimit;
import com.abstruse.custos.core.engine.RateLimiterEngine;
import com.abstruse.custos.core.model.RequestContext;
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
            return "unknown";
        }
        RequestContext requestContext = getRequestContext(attributes);


        boolean allowed = engine.allow(requestContext, rateLimit);

        if (!allowed) {
            throw new RuntimeException("Rate limited exceeded for");
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
