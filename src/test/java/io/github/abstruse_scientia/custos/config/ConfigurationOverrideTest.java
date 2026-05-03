package io.github.abstruse_scientia.custos.config;

import io.github.abstruse_scientia.custos.core.config.CustosProperties;
import io.github.abstruse_scientia.custos.core.config.CustosMainProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test: Custom Configuration Override
 * Verify custom values from application properties override defaults.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "custos.store=memory",
        "custos.token-bucket.capacity=50",
        "custos.token-bucket.rate=10"
})
class ConfigurationOverrideTest {

    @Autowired
    private CustosProperties custosProperties;

    @Autowired
    private CustosMainProperties custosMainProperties;

    /**
     * Test 1: Custom Configuration Override
     * <p>Setup: application properties with custom values</p>
     * <p>Verify: Custom values override defaults</p>
     */
    @Test
    void testConfigurationOverride() {
        // Assert: Custom capacity is applied
        assertThat(custosProperties.getCapacity()).isEqualTo(50);

        // Assert: Custom rate is applied
        assertThat(custosProperties.getRate()).isEqualTo(10.0);

        // Assert: Custom store type is applied
        assertThat(custosMainProperties.getStore()).isEqualTo("memory");
    }

    /**
     * Test 1.1: Verify overridden values are different from class defaults
     */
    @Test
    void testOverriddenValuesAreDifferent() {
        CustosProperties defaultProps = new CustosProperties();

        // Overridden capacity (50) should differ from default (10)
        assertThat(custosProperties.getCapacity()).isNotEqualTo(defaultProps.getCapacity());
        assertThat(custosProperties.getCapacity()).isEqualTo(50);
    }
}
