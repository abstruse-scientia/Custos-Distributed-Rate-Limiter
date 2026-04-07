package com.abstruse.custos.core.store;

import com.abstruse.custos.core.model.BucketState;

public interface RateLimitStore {
    BucketState get(String key);
    void put(String key, BucketState state);
}
