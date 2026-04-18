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
     * Max request allowed
     */
    int capacity() default 10;

    /**
     * Request allowed per second
     */
    double rate() default 5;

}
