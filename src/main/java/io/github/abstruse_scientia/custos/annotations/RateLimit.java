package io.github.abstruse_scientia.custos.annotations;


import io.github.abstruse_scientia.custos.core.model.Algorithm;
import io.github.abstruse_scientia.custos.resolver.KeyType;

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
    double refillRate() default 5;

}
