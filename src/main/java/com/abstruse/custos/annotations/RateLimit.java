package com.abstruse.custos.annotations;


import com.abstruse.custos.core.model.Algorithm;
import com.abstruse.custos.resolver.KeyType;

import java.lang.annotation.*;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface RateLimit {

    /**
     * Type of rate limiting. By default, set to user.
     */
    KeyType keytype() default KeyType.USER;

    /**
     * Type of algorithm defined by user. By default token bucket.
     */
    Algorithm algorithm() default Algorithm.TOKEN_BUCKET;

    /**
     * Capacity of the bucket to hold tokens.
     */
    int capacity() default 10;

    /**
     * Rate by which token should be refilled.
     */
    int refillRate() default 5;

}
