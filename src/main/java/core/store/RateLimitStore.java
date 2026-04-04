package core.store;

import core.model.BucketState;

public interface RateLimitStore {
    BucketState get(String key);
    void put(String key, BucketState state);
}
