package io.github.abstruse_scientia.custos.core.store;

import io.github.abstruse_scientia.custos.core.model.BucketState;

public interface RateLimitStore {
    BucketState get(String key);
    void put(String key, BucketState state);
}
