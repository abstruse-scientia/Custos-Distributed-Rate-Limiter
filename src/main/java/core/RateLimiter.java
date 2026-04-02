package core;

public interface RateLimiter {

    /**
     *
     * @param capacity : Maximum number of attempts allowed
     * @param refillRate : Number of request that can be restored
     * @param key : Unique id (Example: "user72" or "ip:12.1.02.1")
     * @return true if allowed, false if not.
     */
    boolean isAllowed(int capacity, int refillRate, String key);
}
