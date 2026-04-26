package io.github.abstruse_scientia.custos.integration.store;

import io.github.abstruse_scientia.custos.core.model.BucketState;
import io.github.abstruse_scientia.custos.core.store.InMemoryStore;
import io.github.abstruse_scientia.custos.core.store.RateLimitStore;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(properties = {"custos.store=memory"})
public class InMemoryStoreIntegrationTest {

    @Autowired
    private RateLimitStore store;

    @Test
    public void testInMemoryStoreProvided() {
        assertThat(store).isInstanceOf(InMemoryStore.class);
    }

    @Test
    public void testInMemoryPersistence() {
        for (int i = 0; i < 100; i++) {
            BucketState state = new BucketState(i * 1.0, System.currentTimeMillis());
            store.put("user" + i, state);
        }

        for (int i = 0; i < 100; i++) {
            BucketState retrieved = (BucketState) store.get("user" + i);
            Assertions.assertThat(retrieved).isNotNull();
            Assertions.assertThat(retrieved.getTokens()).isEqualTo(i * 1.0);
        }
    }
}
