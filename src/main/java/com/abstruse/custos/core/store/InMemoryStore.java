package com.abstruse.custos.core.store;

import com.abstruse.custos.core.model.BucketState;

import java.util.concurrent.ConcurrentHashMap;

public class InMemoryStore implements RateLimitStore {

    private final ConcurrentHashMap<String, BucketState> store = new ConcurrentHashMap<>();

    @Override
    public BucketState get(String key) {
        return store.get(key);
    }

    @Override
    public void put(String key, BucketState state){
        store.put(key, state);
    }
}
