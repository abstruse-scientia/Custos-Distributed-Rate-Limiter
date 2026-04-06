package core.strategy.redis;

import core.config.RateLimitConfig;
import core.model.Algorithm;
import core.store.RateLimitStore;
import core.strategy.RateLimiterStrategy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;


public class RedisTokenBucketStrategy implements RateLimiterStrategy {

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> redisScript;

    private static final String LUA_SCRIPT = "LUA_SCRIPT_TOKEN_BUCKET";

    public RedisTokenBucketStrategy(StringRedisTemplate redisTemplate) {
        this.stringRedisTemplate = redisTemplate;
        this.redisScript = new DefaultRedisScript<>();
        this.redisScript.setScriptText(LUA_SCRIPT);
        this.redisScript.setResultType(Long.class);
    }

    @Override
    public Algorithm getAlgorithm() {
        return Algorithm.TOKEN_BUCKET;

    }

    @Override
    public boolean allow(String key, RateLimitConfig config, RateLimitStore store) {
        Long result = stringRedisTemplate.execute(
                redisScript,
                Collections.singletonList(buildKey(key)),
                String.valueOf(config.getCapacity()),
                String.valueOf(config.getRefillRate()),
                String.valueOf(System.currentTimeMillis())
        );

        return result == 1;
    }

    private String buildKey(String key) {
        return "custos:rrl:" + key;
    }

}
