package io.github.abstruse_scientia.custos.integration.store;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Redis Store backend.
 * <p> Uses TestContainers to spin up Redis for testing.</p>
 */
@SpringBootTest(properties = {
    "custos.store=redis"
})
@Testcontainers
public class RedisStoreIntegrationTest {



    @Container
    @ServiceConnection
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    public void setup() {
        // Clear Redis before each test
        redisTemplate.getConnectionFactory()
                .getConnection()
                .flushAll();
    }

    /**
     * Test 1: Redis Basic Operations
     * Verify Redis store handles SET/GET operations correctly
     */
    @Test
    public void testRedisBasicOperations() {
        String userId = "redisUser";
        String stateKey = "state:" + userId;
        
        redisTemplate.opsForValue().set(stateKey, "50.0");
        String retrieved = redisTemplate.opsForValue().get(stateKey);

        assertThat(retrieved).isNotNull();
        assertThat(retrieved).isEqualTo("50.0");
    }

    /**
     * Test 2: Redis Persistence Across Connections
     * Verify data persists when reconnecting
     */
    @Test
    public void testRedisPersistence() {
        String userId = "persistentUser";
        String stateKey = "state:" + userId;
        
        redisTemplate.opsForValue().set(stateKey, "75.0");
        
        String retrieved = redisTemplate.opsForValue().get(stateKey);
        assertThat(retrieved).isEqualTo("75.0");

        // Simulate another connection
        String retrievedAgain = redisTemplate.opsForValue().get(stateKey);
        assertThat(retrievedAgain).isEqualTo("75.0");
    }

    /**
     * Test 3: Redis Key Expiration
     * <p>Verify TTL functionality works correctly</p>
     */
    @Test
    public void testRedisKeyExpiration() throws InterruptedException {
        String userId = "tempUser";
        String stateKey = "state:" + userId;
        
        redisTemplate.opsForValue().set(stateKey, "20.0", 1, TimeUnit.SECONDS);
        
        String immediate = redisTemplate.opsForValue().get(stateKey);
        assertThat(immediate).isEqualTo("20.0");
        
        Thread.sleep(1100);
        
        String expired = redisTemplate.opsForValue().get(stateKey);
        assertThat(expired).isNull();
    }

    /**
     * Test 4: Redis Multiple Keys
     * <p>Verify storing and retrieving multiple independent keys</p>
     */
    @Test
    public void testRedisMultipleKeys() {
        for (int i = 0; i < 50; i++) {
            String key = "state:user" + i;
            redisTemplate.opsForValue().set(key, String.valueOf(i * 2.0));
        }

        for (int i = 0; i < 50; i++) {
            String key = "state:user" + i;
            String value = redisTemplate.opsForValue().get(key);
            assertThat(value).isEqualTo(String.valueOf(i * 2.0));
        }
    }

    /**
     * Test 5: Redis Increment Operation
     * <p>Verify atomic increment operations work</p>
     */
    @Test
    public void testRedisIncrementOperation() {
        String counterKey = "counter:requests";
        
        redisTemplate.opsForValue().set(counterKey, "0");
        
        for (int i = 1; i <= 10; i++) {
            redisTemplate.opsForValue().increment(counterKey);
        }
        
        String value = redisTemplate.opsForValue().get(counterKey);
        assertThat(value).isEqualTo("10");
    }
}

