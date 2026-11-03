package dev.vessel.api.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.api.storage.CapabilityTransactionSimulator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite verifying the Universal Energy Substrate bridging Forge Energy (FE)
 * and Fabric Energy (TR E), testing atomic concurrency, transaction simulation, and Rule 17 compliance.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
class UniversalEnergySymmetryTest {

    @Test
    @DisplayName("VesselEnergyView: Primitive read inspections across various storage representations")
    void testVesselEnergyViewInspections() {
        ZeroAllocationEnergyStorage storage = new ZeroAllocationEnergyStorage(50_000L, 1_000L, 1_000L, 25_000L);
        SimpleEnergyContainer container = new SimpleEnergyContainer(10_000L, 500L, 500L, 5_000L);
        AtomicLong atomic = new AtomicLong(1_234L);
        Long scalar = 9_999L;

        // ZeroAllocationEnergyStorage
        assertEquals(25_000L, VesselEnergyView.getEnergyStored(storage));
        assertEquals(50_000L, VesselEnergyView.getMaxEnergyStored(storage));
        assertTrue(VesselEnergyView.canReceive(storage));
        assertTrue(VesselEnergyView.canExtract(storage));

        // SimpleEnergyContainer
        assertEquals(5_000L, VesselEnergyView.getEnergyStored(container));
        assertEquals(10_000L, VesselEnergyView.getMaxEnergyStored(container));
        assertTrue(VesselEnergyView.canReceive(container));
        assertTrue(VesselEnergyView.canExtract(container));

        // AtomicLong
        assertEquals(1_234L, VesselEnergyView.getEnergyStored(atomic));
        assertTrue(VesselEnergyView.canExtract(atomic));
        assertTrue(VesselEnergyView.canReceive(atomic));

        // Number / Scalar
        assertEquals(9_999L, VesselEnergyView.getEnergyStored(scalar));
        assertEquals(9_999L, VesselEnergyView.getMaxEnergyStored(scalar));

        // Null handling
        assertEquals(0L, VesselEnergyView.getEnergyStored(null));
        assertEquals(0L, VesselEnergyView.getMaxEnergyStored(null));
        assertFalse(VesselEnergyView.canReceive(null));
        assertFalse(VesselEnergyView.canExtract(null));
    }

    @Test
    @DisplayName("ZeroAllocationEnergyStorage: Atomic CAS receive/extract, limits, and zero-allocation semantics")
    void testZeroAllocationEnergyStorageOperations() {
        ZeroAllocationEnergyStorage storage = new ZeroAllocationEnergyStorage(10_000L, 1_000L, 500L, 0L);

        assertEquals(10_000L, storage.getMaxEnergyStored());
        assertEquals(1_000L, storage.getMaxReceive());
        assertEquals(500L, storage.getMaxExtract());
        assertEquals(0L, storage.getEnergyStored());
        assertTrue(storage.canReceive());
        assertFalse(storage.canExtract());

        // Simulate receive
        long simReceived = storage.receiveEnergy(800L, true);
        assertEquals(800L, simReceived);
        assertEquals(0L, storage.getEnergyStored()); // no mutation

        // Execute receive
        long actualReceived = storage.receiveEnergy(800L, false);
        assertEquals(800L, actualReceived);
        assertEquals(800L, storage.getEnergyStored());
        assertTrue(storage.canExtract());

        // Rate limit clamp on receive: maxReceive is 1,000
        long rateClampedReceive = storage.receiveEnergy(5_000L, false);
        assertEquals(1_000L, rateClampedReceive);
        assertEquals(1_800L, storage.getEnergyStored());

        // Simulate extract
        long simExtracted = storage.extractEnergy(400L, true);
        assertEquals(400L, simExtracted);
        assertEquals(1_800L, storage.getEnergyStored());

        // Execute extract with maxExtract rate clamp (500)
        long actualExtracted = storage.extractEnergy(800L, false);
        assertEquals(500L, actualExtracted);
        assertEquals(1_300L, storage.getEnergyStored());

        // Direct set and reset
        storage.setEnergyStored(7_777L);
        assertEquals(7_777L, storage.getEnergyStored());
        storage.reset();
        assertEquals(0L, storage.getEnergyStored());

        // Constructor validation
        assertThrows(IllegalArgumentException.class, () -> new ZeroAllocationEnergyStorage(-1L));
        assertThrows(IllegalArgumentException.class, () -> new ZeroAllocationEnergyStorage(100L, -5L, 10L));
    }

    @Test
    @DisplayName("SimpleEnergyContainer: Synchronized machine buffer state management")
    void testSimpleEnergyContainerOperations() {
        SimpleEnergyContainer container = new SimpleEnergyContainer(20_000L, 2_000L, 2_000L, 10_000L);

        assertEquals(20_000L, container.getMaxEnergyStored());
        assertEquals(10_000L, container.getEnergyStored());
        assertTrue(container.canReceive());
        assertTrue(container.canExtract());

        long received = container.receiveEnergy(1_500L, false);
        assertEquals(1_500L, received);
        assertEquals(11_500L, container.getEnergyStored());

        long extracted = container.extractEnergy(500L, false);
        assertEquals(500L, extracted);
        assertEquals(11_000L, container.getEnergyStored());

        container.reset();
        assertEquals(0L, container.getEnergyStored());
    }

    @Test
    @DisplayName("EnergyTransferBridge: Point-to-point transfer and symmetrical 1:1 unit conversion")
    void testEnergyTransferBridgeDirectTransfer() {
        ZeroAllocationEnergyStorage source = new ZeroAllocationEnergyStorage(10_000L, 1_000L, 1_000L, 5_000L);
        ZeroAllocationEnergyStorage destination = new ZeroAllocationEnergyStorage(10_000L, 1_000L, 1_000L, 0L);

        long moved = EnergyTransferBridge.transferEnergy(source, destination, 800L);
        assertEquals(800L, moved);
        assertEquals(4_200L, source.getEnergyStored());
        assertEquals(800L, destination.getEnergyStored());

        // Conversion verification (1 FE == 1 TR E)
        assertEquals(1.0, EnergyTransferBridge.FORGE_ENERGY_TO_FABRIC_ENERGY_RATIO);
        assertEquals(1_500L, EnergyTransferBridge.forgeToFabricEnergy(1_500L));
        assertEquals(1_500L, EnergyTransferBridge.fabricToForgeEnergy(1_500L));
    }

    @Test
    @DisplayName("ForgeEnergySymmetryAdapter: 32-bit signed int opcode receptor and boundary clamping")
    void testForgeEnergySymmetryAdapter() {
        ZeroAllocationEnergyStorage storage = new ZeroAllocationEnergyStorage(50_000L, 5_000L, 5_000L, 10_000L);

        int received = ForgeEnergySymmetryAdapter.receiveEnergy(storage, 2_000, false);
        assertEquals(2_000, received);
        assertEquals(12_000, ForgeEnergySymmetryAdapter.getEnergyStored(storage));

        int extracted = ForgeEnergySymmetryAdapter.extractEnergy(storage, 1_000, false);
        assertEquals(1_000, extracted);
        assertEquals(11_000, ForgeEnergySymmetryAdapter.getEnergyStored(storage));

        assertEquals(50_000, ForgeEnergySymmetryAdapter.getMaxEnergyStored(storage));
        assertTrue(ForgeEnergySymmetryAdapter.canExtract(storage));
        assertTrue(ForgeEnergySymmetryAdapter.canReceive(storage));
    }

    @Test
    @DisplayName("FabricEnergySymmetryAdapter: 64-bit long operations and TransactionContext integration")
    void testFabricEnergySymmetryAdapterWithTransaction() {
        ZeroAllocationEnergyStorage storage = new ZeroAllocationEnergyStorage(100_000L, 10_000L, 10_000L, 20_000L);
        CapabilityTransactionSimulator sim = new CapabilityTransactionSimulator();

        sim.beginNestedTransaction();

        // Insert with transaction context
        long inserted = FabricEnergySymmetryAdapter.insert(storage, 5_000L, sim);
        assertEquals(5_000L, inserted);
        assertEquals(25_000L, FabricEnergySymmetryAdapter.getAmount(storage));
        assertEquals(100_000L, FabricEnergySymmetryAdapter.getCapacity(storage));
        assertTrue(FabricEnergySymmetryAdapter.supportsInsertion(storage));
        assertTrue(FabricEnergySymmetryAdapter.supportsExtraction(storage));

        // Verify snapshot was captured in transaction simulator journal
        assertTrue(sim.getSnapshotCount() > 0);

        // Commit transaction
        sim.commitTransaction();
        assertEquals(25_000L, storage.getEnergyStored());
    }

    @Test
    @DisplayName("Concurrency: Multi-threaded parallel transfer conservation test")
    void testConcurrentEnergyTransferConservation() throws InterruptedException {
        int threadCount = 8;
        int transfersPerThread = 1_000;
        long initialSourceEnergy = 100_000L;

        ZeroAllocationEnergyStorage source = new ZeroAllocationEnergyStorage(initialSourceEnergy, initialSourceEnergy, initialSourceEnergy, initialSourceEnergy);
        ZeroAllocationEnergyStorage destination = new ZeroAllocationEnergyStorage(initialSourceEnergy, initialSourceEnergy, initialSourceEnergy, 0L);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < transfersPerThread; j++) {
                        EnergyTransferBridge.transferEnergy(source, destination, 10L);
                    }
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Multi-threaded transfers timed out");

        // Energy Conservation Law: Total Energy must be conserved exactly
        long totalEnergy = source.getEnergyStored() + destination.getEnergyStored();
        assertEquals(initialSourceEnergy, totalEnergy, "Energy conservation violated under concurrent transfer!");
        assertTrue(destination.getEnergyStored() > 0L);
    }

    @Test
    @DisplayName("Rule 17 Compliance: 100% of classes are top-level with zero inner/nested classes")
    void testRule17ZeroInnerClassesCompliance() {
        Class<?>[] classes = new Class<?>[]{
                VesselEnergyView.class,
                ZeroAllocationEnergyStorage.class,
                SimpleEnergyContainer.class,
                EnergyTransferBridge.class,
                ForgeEnergySymmetryAdapter.class,
                FabricEnergySymmetryAdapter.class
        };

        for (Class<?> clazz : classes) {
            assertNotNull(clazz);
            assertFalse(clazz.isMemberClass(), clazz.getSimpleName() + " must not be a member/inner class");
            assertFalse(clazz.isAnonymousClass(), clazz.getSimpleName() + " must not be an anonymous class");
            assertFalse(clazz.isLocalClass(), clazz.getSimpleName() + " must not be a local class");
            assertEquals(0, clazz.getDeclaredClasses().length, clazz.getSimpleName() + " contains forbidden nested classes!");
        }
    }
}
