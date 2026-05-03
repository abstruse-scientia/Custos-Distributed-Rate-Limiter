package io.github.abstruse_scientia.custos.config;

import io.github.abstruse_scientia.custos.core.config.CustosMainProperties;
import io.github.abstruse_scientia.custos.core.config.CustosProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test: Default Configuration Loading
 * <p>Verify default values are applied when no custom application.yml is provided.</p>
 */
@SpringBootTest
class ConfigurationDefaultsTest {

    @Autowired
    private CustosProperties custosProperties;

    @Autowired
    private CustosMainProperties custosMainProperties;

    /**
     * Test 1: Default Configuration Loading
     * <p>Setup: No custom application.yml overrides for Custos</p>
     * <p>Verify: Default values applied (in-memory store, Token Bucket defaults)</p>
     */
    @Test
    void testDefaultConfigurationLoading() {
        // Assert: CustosProperties defaults
        assertThat(custosProperties).isNotNull();
        assertThat(custosProperties.getCapacity()).isGreaterThan(0);
        assertThat(custosProperties.getRate()).isGreaterThan(0);

        // Assert: CustosMainProperties defaults
        assertThat(custosMainProperties).isNotNull();
        assertThat(custosMainProperties.getStore()).isNotNull();
        assertThat(custosMainProperties.getStore()).isNotEmpty();
    }

    /**
     * Test 1.1: Verify in memory is the default store type
     */
    @Test
    void testDefaultStoreIsMemory() {
        assertThat(custosMainProperties.getStore()).isEqualTo("memory");
    }

    /**
     * Test 1.2: Verify CustosProperties have the class-level defaults
     */
    @Test
    void testClassLevelDefaults() {
        // Create fresh instance to check the hardcoded defaults in the class
        CustosProperties fresh = new CustosProperties();
        assertThat(fresh.getCapacity()).isEqualTo(10);
        assertThat(fresh.getRate()).isEqualTo(5.0);

        CustosMainProperties freshMain = new CustosMainProperties();
        assertThat(freshMain.getStore()).isEqualTo("memory");
    }
}
