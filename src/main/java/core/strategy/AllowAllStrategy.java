package core.strategy;



public class AllowAllStrategy implements RateLimiterStrategy {

    @Override
    public boolean allow(String key) {
        return false;
    }
}
