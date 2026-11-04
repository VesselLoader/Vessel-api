package dev.vessel.api.registry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification test suite for {@link VesselDeferredRegister} and {@link VesselRegistrySupplier}.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 */
public final class VesselDeferredRegisterTest {

    @Test
    @DisplayName("Verify lazy registration, namespaced key generation, and supplier resolution")
    void verifyDeferredRegistrationCycle() {
        VesselDeferredRegister<String> register = VesselDeferredRegister.create(
                VesselRegistryKey.of("test:strings"),
                "mymod"
        );

        VesselRegistrySupplier<String> supplier = register.register("hello", () -> "World 1.1.0");

        assertThat(supplier.getId()).isEqualTo("hello");
        assertThat(supplier.getNamespace()).isEqualTo("mymod");
        assertThat(supplier.getNamespacedId()).isEqualTo("mymod:hello");
        assertThat(supplier.isPresent()).isFalse();

        // Materialize
        assertThat(supplier.get()).isEqualTo("World 1.1.0");
        assertThat(supplier.isPresent()).isTrue();
        assertThat(supplier.asOptional()).contains("World 1.1.0");

        // Verify register entries
        assertThat(register.getEntries()).contains(supplier);
        register.register();
    }
}
