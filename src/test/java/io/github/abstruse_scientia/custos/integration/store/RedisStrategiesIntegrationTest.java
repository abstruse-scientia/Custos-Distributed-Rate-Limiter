package io.github.abstruse_scientia.custos.integration.store;
import io.github.abstruse_scientia.custos.core.config.RateLimitConfig;
import io.github.abstruse_scientia.custos.core.model.RateLimitDecision;
import io.github.abstruse_scientia.custos.core.strategy.redis.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest(properties = {
    "custos.store=redis"
})
@Testcontainers
public class RedisStrategiesIntegrationTest {
    @Container
    @ServiceConnection
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);
    @Autowired
    private StringRedisTemplate redisTemplate;
    @BeforeEach
    public void setup() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }
    @Test
    public void testTokenBucketStrategy() {
        RedisTokenBucketStrategy strategy = new RedisTokenBucketStrategy(redisTemplate);
        RateLimitConfig config = new RateLimitConfig(2.0, 1.0);
        RateLimitDecision d1 = strategy.allow("test_tb", config, null);
        assertThat(d1.allow()).isTrue();
        RateLimitDecision d2 = strategy.allow("test_tb", config, null);
        assertThat(d2.allow()).isTrue();
        RateLimitDecision d3 = strategy.allow("test_tb", config, null);
        assertThat(d3.allow()).isFalse();
        assertThat(d3.retryAfterSeconds()).isGreaterThan(0);
    }
    @Test
    public void testLeakyBucketStrategy() {
        RedisLeakyBucketStrategy strategy = new RedisLeakyBucketStrategy(redisTemplate);
        RateLimitConfig config = new RateLimitConfig(2.0, 1.0);
        RateLimitDecision d1 = strategy.allow("test_lb", config, null);
        assertThat(d1.allow()).isTrue();
        RateLimitDecision d2 = strategy.allow("test_lb", config, null);
        assertThat(d2.allow()).isTrue();
        RateLimitDecision d3 = strategy.allow("test_lb", config, null);
        assertThat(d3.allow()).isFalse();
        assertThat(d3.retryAfterSeconds()).isGreaterThan(0);
    }
    @Test
    public void testSlidingWindowStrategy() {
        RedisSlidingWindowStrategy strategy = new RedisSlidingWindowStrategy(redisTemplate);
        RateLimitConfig config = new RateLimitConfig(2.0, 1.0);
        RateLimitDecision d1 = strategy.allow("test_sw", config, null);
        assertThat(d1.allow()).isTrue();
        RateLimitDecision d2 = strategy.allow("test_sw", config, null);
        assertThat(d2.allow()).isTrue();
        RateLimitDecision d3 = strategy.allow("test_sw", config, null);
        assertThat(d3.allow()).isFalse();
    }
    @Test
    public void testSlidingWindowCounterStrategy() {
        RedisSlidingWindowCounterStrategy strategy = new RedisSlidingWindowCounterStrategy(redisTemplate);
        RateLimitConfig config = new RateLimitConfig(2.0, 1.0);
        RateLimitDecision d1 = strategy.allow("test_swc", config, null);
        assertThat(d1.allow()).isTrue();
        RateLimitDecision d2 = strategy.allow("test_swc", config, null);
        assertThat(d2.allow()).isTrue();
        RateLimitDecision d3 = strategy.allow("test_swc", config, null);
        assertThat(d3.allow()).isFalse();
    }
}
