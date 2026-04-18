package io.github.abstruse_scientia.custos.core.strategy;

import io.github.abstruse_scientia.custos.core.config.RateLimitConfig;
import io.github.abstruse_scientia.custos.core.model.BucketState;
import io.github.abstruse_scientia.custos.core.model.RateLimitDecision;
import io.github.abstruse_scientia.custos.core.store.RateLimitStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test to check basic rate limiting : Testing Token Bucket Strategy
 * 1. testTokenBucketStrategy: tests basic rate limiting
 * 2. checkBucketRefill: as the name suggests, checks if the refill rate is working properly when time lapses.
 * 3. testMultipleUsersInIsolation: rate limiter should be per user based not, global
 */

public class TokenBucketStrategyTest {

    public TokenBucketStrategy strategy;
    public RateLimitStore store;
    public RateLimitConfig config;


    @BeforeEach
    public void setup() {
        strategy = new TokenBucketStrategy();
        store = mock(RateLimitStore.class);
        config = mock(RateLimitConfig.class);
    }


    @Test
    public void testTokenBucketStrategy() {

        when(config.getCapacity()).thenReturn(10);
        when(config.getRefillRate()).thenReturn(5.0);

        // Check basic rate limiting, whether user is allowed up until the capacity(10)
        for(int i = 0; i < 10; i++){
            Object obj = store.get("user1");
            BucketState userState1 = (obj instanceof BucketState) ? (BucketState) obj : null;
            if (userState1 == null) {
                userState1 = new BucketState(config.getCapacity(), System.currentTimeMillis());
            }
            when(store.get("user1")).thenReturn(userState1);

            RateLimitDecision decision = strategy.allow("user1", config, store);
            Assertions.assertTrue(decision.allow());
        }
        Object obj = store.get("user1");
        BucketState userState1 = (obj instanceof BucketState) ? (BucketState) obj : null;
        when(store.get("user1")).thenReturn(userState1);
        RateLimitDecision decision =  strategy.allow("user1", config, store);
        Assertions.assertFalse(decision.allow());
    }


    @Test
    //Checking Partial Refill
    public void checkBucketRefill() throws InterruptedException {


        when(config.getCapacity()).thenReturn(10);
        when(config.getRefillRate()).thenReturn(0.1);

        BucketState userState1 = new BucketState(0,  System.currentTimeMillis());
        when(store.get("user1")).thenReturn(userState1);

        RateLimitDecision decision = strategy.allow("user1", config, store);
        Assertions.assertFalse(decision.allow());

        Thread.sleep(5000);

        //Refill rate is 0.1 per second . so after 5 second it should be 0.5. Therefore Reject
        RateLimitDecision decision2 = strategy.allow("user1", config, store);
        Assertions.assertFalse(decision2.allow());

        Thread.sleep(5000);

        Object obj = store.get("user1");
        BucketState updatedSTate = (obj instanceof BucketState) ? (BucketState) obj : null;
        when(store.get("user1")).thenReturn(updatedSTate);

        RateLimitDecision decision3 = strategy.allow("user1", config, store);
        Assertions.assertTrue(decision3.allow());

    }


    /** Check that RateLimiter is not global , but per user */
    @Test
    void testMultipleUsersInIsolation() {

        when(config.getCapacity()).thenReturn(10);
        when(config.getRefillRate()).thenReturn(0.1);

        // Check for user 1
        for (int i = 0; i < 10; i++) {
            //Get Bucket State
            Object obj = store.get("user1");
            BucketState userState1 = (obj instanceof BucketState) ? (BucketState) obj : null;
            //If Bucket Sate not initialized
            if (userState1 == null) {
                userState1 = new BucketState(config.getCapacity(), System.currentTimeMillis());
            }
            // After initialization in each iteration will get userState1 bucket
            when(store.get("user1")).thenReturn(userState1);
            RateLimitDecision decision = strategy.allow("user1", config, store);
            Assertions.assertTrue(decision.allow());
        }

        RateLimitDecision decision = strategy.allow("user1", config, store);
        Assertions.assertFalse(decision.allow());

        BucketState userState2 = new BucketState(config.getCapacity(),  System.currentTimeMillis());
        when(store.get("user2")).thenReturn(userState2);

        for (int i = 0; i < 9; i++) {
            RateLimitDecision decision2 = strategy.allow("user2", config, store);
            Assertions.assertTrue(decision2.allow());
        }
    }


}
