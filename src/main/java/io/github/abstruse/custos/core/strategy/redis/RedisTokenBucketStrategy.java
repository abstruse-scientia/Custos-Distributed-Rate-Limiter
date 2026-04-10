package com.abstruse.custos.core.strategy.redis;

import com.abstruse.custos.core.config.RateLimitConfig;
import com.abstruse.custos.core.model.Algorithm;
import com.abstruse.custos.core.model.RateLimitDecision;
import com.abstruse.custos.core.store.RateLimitStore;
import com.abstruse.custos.core.strategy.RateLimiterStrategy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.List;


public class RedisTokenBucketStrategy implements RateLimiterStrategy {

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<List<Long>> redisScript;

    private static final String LUA_SCRIPT = "LUA_SCRIPT_TOKEN_BUCKET";

    @SuppressWarnings("unchecked")
    public RedisTokenBucketStrategy(StringRedisTemplate redisTemplate) {
        this.stringRedisTemplate = redisTemplate;
        this.redisScript = new DefaultRedisScript<>();
        this.redisScript.setScriptText(LUA_SCRIPT);
        this.redisScript.setResultType((Class<List<Long>>) (Class<?>) List.class); // Type Erasure: Leads to
        // IntelliJ warnings. Therefore @SuppressWarnings
    }

    @Override
    public Algorithm getAlgorithm() {
        return Algorithm.TOKEN_BUCKET;

    }

    @Override
    public RateLimitDecision allow(String key, RateLimitConfig config, RateLimitStore store) {
        List<Long> result =  stringRedisTemplate.execute(
                redisScript,
                Collections.singletonList(buildKey(key)),
                String.valueOf(config.getCapacity()),
                String.valueOf(config.getRefillRate()),
                String.valueOf(System.currentTimeMillis())
        );
        boolean allow = result.get(0) == 1;
        long retryAfterSeconds = result.get(1);
        return new RateLimitDecision(allow, retryAfterSeconds);
    }

    private String buildKey(String key) {
        return "custos:rrl:" + key;
    }

}
