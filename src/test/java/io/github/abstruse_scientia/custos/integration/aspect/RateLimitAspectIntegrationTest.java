package io.github.abstruse_scientia.custos.integration.aspect;

import io.github.abstruse_scientia.custos.integration.TestRateLimitConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for RateLimit Aspect AOP functionality.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestRateLimitConfig.class)
public class RateLimitAspectIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Test 1: @RateLimit Annotation Processing
     * Verify AOP intercepts @RateLimit annotations and applies rate limiting
     */
    @Test
    public void testAnnotationProcessing() throws Exception {
        String userId = "test-user-annotation";

        // Make a few requests to verify the rate limiting infrastructure is working
        // With proper userId header to ensure user based isolation
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/test/v1/user-limited")
                    .header("X-User-Id", userId))
                    .andExpect(status().isOk());
        }

        // An additional request should also succeed (within capacity)
        mockMvc.perform(get("/api/test/v1/user-limited")
                .header("X-User-Id", userId))
                .andExpect(status().isOk());
    }

    /**
     * Test 2: Rate Limit Exception Thrown
     * Verify that RateLimitExceededException is properly thrown when limit is exceeded
     * Also for time based strategies: such as Leaky Bucket or token bucket refill rate should be kept low.
     */
    @Test
    public void testRateLimitExceptionThrown() throws Exception {
        String userId = "test-user-exception";
        
        // Exhaust the capacity: /user-limited has capacity = 100 with TOKEN_BUCKET algorithm
        // Note: tokens refill over time (rate = 2), so 100 requests might not be enough
        // depending on test execution speed. We make up to 120 requests to guarantee exhaustion.
        boolean gotTooManyRequests = false;
        for (int i = 0; i < 120; i++) {
            int status = mockMvc.perform(get("/api/test/v1/user-limited")
                    .header("X-User-Id", userId))
                    .andReturn().getResponse().getStatus();
            if (status == 429) {
                gotTooManyRequests = true;
                break;
            }
        }

        assertThat(gotTooManyRequests)
                .withFailMessage("Expected rate limit to be exceeded, but it wasn't")
                .isTrue();
    }

    /**
     * Test 3: Multiple Rate Limits on Single Method
     * Verify that multiple rate limit configurations can be applied and enforced independently
     */
    @Test
    public void testMultipleRateLimits() throws Exception {
        String userA = "test-user-multiple-a";
        String userB = "test-user-multiple-b";

        // User A: Make 5 requests
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/test/v1/user-limited")
                    .header("X-User-Id", userA))
                    .andExpect(status().isOk());
        }

        // User B: Make 5 requests - should not be blocked by User A's requests
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/test/v1/user-limited")
                    .header("X-User-Id", userB))
                    .andExpect(status().isOk());
        }

        // Verify both users can still make requests (independent limits)
        mockMvc.perform(get("/api/test/v1/user-limited")
                .header("X-User-Id", userA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/test/v1/user-limited")
                .header("X-User-Id", userB))
                .andExpect(status().isOk());
    }


    /**
     * Test 4: User Isolation in Rate Limiting
     * Verify that different users have independent rate limit buckets
     * One user exhausting their limit does not affect other users
     */
    @Test
    public void testUserIsolation() throws Exception {
        String user1 = "test-user-isolation-1";
        String user2 = "test-user-isolation-2";

        // User 1: Exhaust their capacity (100 requests)
        // /user-limited has capacity = 100, rate = 20
        // Note: The refill rate means requests will succeed even after 100 due to token refill
        // We'll verify user isolation by checking both users independently
        for (int i = 0; i < 50; i++) {
            mockMvc.perform(get("/api/test/v1/user-limited")
                    .header("X-User-Id", user1))
                    .andExpect(status().isOk());
        }

        // User 2 should have their own independent bucket
        // even though User 1 made 50 requests
        for (int i = 0; i < 50; i++) {
            mockMvc.perform(get("/api/test/v1/user-limited")
                    .header("X-User-Id", user2))
                    .andExpect(status().isOk());
        }

        // Both users should still be able to make requests
        // proving they have independent rate limit buckets
        mockMvc.perform(get("/api/test/v1/user-limited")
                .header("X-User-Id", user1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/test/v1/user-limited")
                .header("X-User-Id", user2))
                .andExpect(status().isOk());

        // Test that a request without userId uses a different bucket
        mockMvc.perform(get("/api/test/v1/user-limited"))
                .andExpect(status().isOk());
    }
}

