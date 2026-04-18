package io.github.abstruse_scientia.custos.core.store;

import java.util.function.BiFunction;

/**
 * Generic rate limit state store interface
 * Implementation can store any type of state object as long as
 * the strategy implementation can cast it appropriately.
 */
public interface RateLimitStore {
    /* Earlier implementation focused only on BucketState
    which was useful only for TokenBucketBased Algorithm, Now it's more inclined towards
    being a general store which can be cast to appropriate storage strategy based on use case.
     */
    Object get(String key);
    void put(String key, Object state);

    /**
     * Atomically computes new value for the key.
     * While computing the key is locked.
     * @param key: The rate limit key
     * @param remappingFunction: Function to compute new value for given current value
     * @return The computed value
     */
    Object atomicCompute(String key, BiFunction<String, Object, Object> remappingFunction);
}
