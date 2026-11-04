package dev.vessel.api.capability;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification test suite for universal capability contracts and {@link VesselCapabilityLookup}.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 */
public final class VesselCapabilityLookupTest {

    @Test
    @DisplayName("Verify universal energy contract conversions and simulation semantics")
    void verifyUniversalEnergyCapability() {
        VesselEnergy energy = new VesselEnergy() {
            private long stored = 5000L;

            @Override
            public long getEnergyStored() {
                return stored;
            }

            @Override
            public long getMaxEnergyStored() {
                return 10000L;
            }

            @Override
            public long receiveEnergy(long maxReceive, boolean simulate) {
                long accepted = Math.min(maxReceive, getMaxEnergyStored() - stored);
                if (!simulate) {
                    stored += accepted;
                }
                return accepted;
            }

            @Override
            public long extractEnergy(long maxExtract, boolean simulate) {
                long extracted = Math.min(maxExtract, stored);
                if (!simulate) {
                    stored -= extracted;
                }
                return extracted;
            }
        };

        assertThat(energy.getEnergyStored()).isEqualTo(5000L);
        assertThat(energy.receiveEnergy(2000L, true)).isEqualTo(2000L);
        assertThat(energy.getEnergyStored()).isEqualTo(5000L); // Unchanged after simulate

        assertThat(energy.receiveEnergy(2000L, false)).isEqualTo(2000L);
        assertThat(energy.getEnergyStored()).isEqualTo(7000L);
    }

    @Test
    @DisplayName("Verify fluid handler milli-Bucket and droplet conversions")
    void verifyFluidDropletConversion() {
        VesselFluidHandler fluidHandler = new VesselFluidHandler() {
            @Override
            public String getFluidType() {
                return "minecraft:water";
            }

            @Override
            public long getFluidAmount() {
                return 2500L; // 2.5 buckets
            }

            @Override
            public long getCapacity() {
                return 10000L; // 10 buckets
            }

            @Override
            public long fill(String fluidType, long amount, boolean simulate) {
                return amount;
            }

            @Override
            public long drain(String fluidType, long maxDrain, boolean simulate) {
                return maxDrain;
            }
        };

        assertThat(fluidHandler.getFluidAmount()).isEqualTo(2500L);
        // 2500 * 81 = 202,500 droplets
        assertThat(fluidHandler.getFluidAmountDroplets()).isEqualTo(202500L);
        assertThat(fluidHandler.getCapacityDroplets()).isEqualTo(810000L);
    }
}
