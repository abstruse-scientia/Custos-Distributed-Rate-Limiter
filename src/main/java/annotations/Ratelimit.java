package annotations;


import java.lang.annotation.*;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Ratelimit {

    /**
     * Max number of requests allowed in window
     */
    int capacity() default 10;

    /**
     * How many tokens allowed to added back per second
     */
    int refillRate() default 1;

    /**
     * SpEL : Spring Expression Language expression for the key.
     * If left blank, Custos Library will use default key.
     */
    String key()  default "";

}
