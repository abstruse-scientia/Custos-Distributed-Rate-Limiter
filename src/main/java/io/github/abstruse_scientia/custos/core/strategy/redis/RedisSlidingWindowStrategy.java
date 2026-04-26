package io.github.abstruse_scientia.custos.core.strategy.redis;

import io.github.abstruse_scientia.custos.core.config.RateLimitConfig;
import io.github.abstruse_scientia.custos.core.model.Algorithm;
import io.github.abstruse_scientia.custos.core.model.RateLimitDecision;
import io.github.abstruse_scientia.custos.core.store.RateLimitStore;
import io.github.abstruse_scientia.custos.core.strategy.RateLimiterStrategy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class RedisSlidingWindowStrategy  implements RateLimiterStrategy {

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<List<Long>> redisScript;

    private static final String LUA_SCRIPT_PATH = "scripts/LUA_SCRIPT_SLIDING_WINDOW.lua";

    @SuppressWarnings("unchecked")
    public RedisSlidingWindowStrategy(StringRedisTemplate redisTemplate) {
        this.stringRedisTemplate = redisTemplate;
        this.redisScript = new DefaultRedisScript<>();
        this.redisScript.setLocation(new ClassPathResource(LUA_SCRIPT_PATH));
        this.redisScript.setResultType((Class<List<Long>>) (Class<?>) List.class);
    }

    @Override
    public Algorithm getAlgorithm() {
        return Algorithm.SLIDING_WINDOW;
    }

    @Override
    public RateLimitDecision allow(String key, RateLimitConfig config, RateLimitStore store) {
        // Calculate window duration in milliseconds
        long windowDurationMs = (long) ((config.getCapacity() / config.getRate()) * 1000);
        long now = System.currentTimeMillis();
        String uniqueMember = now + ":" + UUID.randomUUID();
        // Execute Lua script atomically
        List<Long> result = stringRedisTemplate.execute(
                redisScript,
                Collections.singletonList(buildKey(key)),
                String.valueOf(config.getCapacity()),
                String.valueOf(windowDurationMs),
                String.valueOf(now),
                uniqueMember
        );

        boolean allowed = result.get(0) == 1;
        long retryAfterSeconds = result.size() > 1 && result.get(1) != null ? result.get(1) : 0;

        return new RateLimitDecision(allowed, retryAfterSeconds);
    }

    /**
     * Builds Redis key with namespace prefix
     * Format: custos:rswl:{originalKey}
     * rswl = redis sliding window limit
     */
    private String buildKey(String key) {
        return "custos:rswl:" + key;
    }
}

