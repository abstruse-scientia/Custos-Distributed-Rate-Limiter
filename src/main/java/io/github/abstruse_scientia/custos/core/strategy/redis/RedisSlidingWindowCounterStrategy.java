package io.github.abstruse_scientia.custos.core.strategy.redis;

import io.github.abstruse_scientia.custos.core.config.RateLimitConfig;
import io.github.abstruse_scientia.custos.core.model.Algorithm;
import io.github.abstruse_scientia.custos.core.model.RateLimitDecision;
import io.github.abstruse_scientia.custos.core.store.RateLimitStore;
import io.github.abstruse_scientia.custos.core.strategy.RateLimiterStrategy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.ArrayList;
import java.util.List;

public class RedisSlidingWindowCounterStrategy implements RateLimiterStrategy {

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<List<Long>> redisScript;

    private static final String LUA_SCRIPT_PATH = "scripts/LUA_SCRIPT_SLIDING_WINDOW_COUNTER.lua";

    @SuppressWarnings("unchecked")
    public RedisSlidingWindowCounterStrategy(StringRedisTemplate redisTemplate) {
        this.stringRedisTemplate = redisTemplate;
        this.redisScript = new DefaultRedisScript<>();
        this.redisScript.setLocation(new ClassPathResource(LUA_SCRIPT_PATH));
        this.redisScript.setResultType((Class<List<Long>>) (Class<?>) List.class);
    }

    @Override
    public Algorithm getAlgorithm() {
        return Algorithm.SLIDING_WINDOW_COUNTER;
    }

    @Override
    public RateLimitDecision allow(String key, RateLimitConfig config, RateLimitStore store) {

        long currentMs = System.currentTimeMillis();
        long currentSecond = (currentMs / 1000) % 60;
        double previousWeight = (60 - currentSecond) / 60.0;
        List<Long> result = stringRedisTemplate.execute(
                redisScript,
                buildKeys(key),
                String.valueOf(config.getCapacity()),
                String.valueOf(previousWeight),
                String.valueOf(currentSecond)
        );

        boolean allowed = result.get(0) == 1;
        long retryAfterSeconds = result.size() > 1 && result.get(1) != null ? result.get(1) : 0;

        return new RateLimitDecision(allowed, retryAfterSeconds);
    }

    private List<String> buildKeys(String key) {
        List<String> keys = new ArrayList<>();
        long currentMs = System.currentTimeMillis();
        long currentMinute = currentMs / 60000;
        long previousMinute = currentMinute - 1;

        keys.add("custos:rswc:" + key + ":" + currentMinute);
        keys.add("custos:rswc:" + key + ":" + previousMinute);
        return keys;
    }


}
