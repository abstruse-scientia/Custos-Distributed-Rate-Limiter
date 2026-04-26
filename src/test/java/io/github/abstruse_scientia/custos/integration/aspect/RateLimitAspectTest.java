package io.github.abstruse_scientia.custos.integration.aspect;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import io.github.abstruse_scientia.custos.integration.TestRateLimitConfig;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestRateLimitConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RateLimitAspectTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testAnnotationProcessing() throws Exception {
        // Based on the capacity of 100 in MockTestController for /user-limited
        boolean gotTooManyRequests = false;
        for (int i = 0; i < 120; i++) {
            int status = mockMvc.perform(get("/api/test/v1/user-limited"))
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

    @Test
    public void testMultipleRateLimits() throws Exception {
        // Based on the capacity of 50 in MockTestController for /ip-limited
        boolean gotTooManyRequests = false;
        for (int i = 0; i < 60; i++) {
            int status = mockMvc.perform(get("/api/test/v1/ip-limited"))
                    .andReturn().getResponse().getStatus();
            if (status == 429) {
                gotTooManyRequests = true;
                break;
            }
        }
        
        org.assertj.core.api.Assertions.assertThat(gotTooManyRequests)
                .withFailMessage("Expected IP rate limit to be exceeded, but it wasn't")
                .isTrue();
    }
}
