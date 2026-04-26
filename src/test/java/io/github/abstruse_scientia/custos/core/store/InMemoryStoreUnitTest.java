package io.github.abstruse_scientia.custos.core.store;

import io.github.abstruse_scientia.custos.core.model.BucketState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class InMemoryStoreUnitTest {

    private InMemoryStore store;

    @BeforeEach
    public void setup() {
        store = new InMemoryStore();
    }

    /**
     * Test: In Memory Store Persistence
     * <p>Verify in memory store handles SET/GET operations correctly</p>
     */
    @Test
    public void testInMemoryPersistence() {
        String userId = "user123";
        BucketState state = new BucketState(10.0, System.currentTimeMillis());

        store.put(userId, state);
        BucketState retrieved = (BucketState) store.get(userId);

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getTokens()).isEqualTo(10.0);
    }

    /**
     * Test: Multiple Insertions in Store
     * <p>Verify store can handle multiple concurrent keys</p>
     */
    @Test
    public void testMultipleInsertions() {
        for (int i = 0; i < 100; i++) {
            BucketState state = new BucketState(i * 1.0, System.currentTimeMillis());
            store.put("user" + i, state);
        }

        for (int i = 0; i < 100; i++) {
            BucketState retrieved = (BucketState) store.get("user" + i);
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getTokens()).isEqualTo(i * 1.0);
        }
    }

    /**
     * Test: Atomic Updates in Store
     * <p>Verify atomicCompute performs updates safely</p>
     */
    @Test
    public void testAtomicOperations() {
        String userId = "atomicUser";
        BucketState initialState = new BucketState(100.0, System.currentTimeMillis());
        store.put(userId, initialState);

        BucketState updated = (BucketState) store.atomicCompute(userId, (key, oldState) -> {
            BucketState state = (BucketState) oldState;
            state.setTokens(state.getTokens() - 10);
            return state;
        });

        assertThat(updated.getTokens()).isEqualTo(90.0);

        BucketState retrieved = (BucketState) store.get(userId);
        assertThat(retrieved.getTokens()).isEqualTo(90.0);
    }

    /**
     * Test: Store Overwrite Operation
     * <p>Verify that putting a new value overwrites the old one</p>
     */
    @Test
    public void testOverwrite() {
        String userId = "overwriteUser";
        BucketState state1 = new BucketState(50.0, System.currentTimeMillis());
        store.put(userId, state1);

        BucketState state2 = new BucketState(75.0, System.currentTimeMillis());
        store.put(userId, state2);

        BucketState retrieved = (BucketState) store.get(userId);
        assertThat(retrieved.getTokens()).isEqualTo(75.0);
    }

    /**
     * Test: Null Key Handling
     * <p>Verify store handles null keys gracefully</p>
     */
    @Test
    public void testNullRetrieval() {
        BucketState retrieved = (BucketState) store.get("nonexistent");
        assertThat(retrieved).isNull();
    }
}

