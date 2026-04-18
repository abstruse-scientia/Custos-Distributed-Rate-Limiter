package io.github.abstruse_scientia.custos.core.store;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;


/**
 * In-memory rate limit state store
 * Thread-safe storage using ConcurrentHashMap
 * Supports storing any type of state object for different strategies
 */
public class InMemoryStore implements RateLimitStore {

    private final ConcurrentHashMap<String, Object> store = new ConcurrentHashMap<>();

    @Override
    public Object get(String key) {
        return store.get(key);
    }

    @Override
    public void put(String key, Object state){
        store.put(key, state);
    }

    @Override
    public Object atomicCompute(String key, BiFunction<String, Object, Object> remappingFunction) {
        return store.compute(key, remappingFunction);
    }

}
