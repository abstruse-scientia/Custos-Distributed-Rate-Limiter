package io.github.abstruse_scientia.custos.core.strategy.redis;

import io.github.abstruse_scientia.custos.core.config.RateLimitConfig;
import io.github.abstruse_scientia.custos.core.model.Algorithm;
import io.github.abstruse_scientia.custos.core.model.RateLimitDecision;
import io.github.abstruse_scientia.custos.core.store.RateLimitStore;
import io.github.abstruse_scientia.custos.core.strategy.RateLimiterStrategy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.List;


public class RedisLeakyBucketStrategy implements RateLimiterStrategy {

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<List<Long>> redisScript;

    private static final String LUA_SCRIPT = "LUA_SCRIPT_LEAKY_BUCKET";

    @SuppressWarnings("unchecked")
    public RedisLeakyBucketStrategy(StringRedisTemplate redisTemplate) {
        this.stringRedisTemplate = redisTemplate;
        this.redisScript = new DefaultRedisScript<>();
        this.redisScript.setScriptText(LUA_SCRIPT);
        this.redisScript.setResultType((Class<List<Long>>) (Class<?>) List.class);
    }

    @Override
    public Algorithm getAlgorithm() {
        return Algorithm.LEAKY_BUCKET;
    }

    @Override
    public RateLimitDecision allow(String key, RateLimitConfig config, RateLimitStore store) {
        // Execute Lua script atomically
        List<Long> result = stringRedisTemplate.execute(
                redisScript,
                Collections.singletonList(buildKey(key)),
                String.valueOf(config.getCapacity()),
                String.valueOf(config.getRate()),
                String.valueOf(System.currentTimeMillis())
        );

        boolean allowed = result.get(0) == 1;
        long retryAfterSeconds = result.get(1);

        return new RateLimitDecision(allowed, retryAfterSeconds);
    }

    /**
     * Builds Redis key with namespace prefix
     * Format: custos:rlb:{originalKey}
     * rlb = redis leaky bucket
     */
    private String buildKey(String key) {
        return "custos:rlb:" + key;
    }
}

