package annotations;


import java.lang.annotation.*;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface RateLimit {


    /**
     * Capacity of the bucket to hold tokens.
     */
    int capacity() default 10;

    /**
     *
     */
    int refillRate() default 5;

}
