package dev.vessel.api.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests verifying Domain 1 Storage Core micro-classes (VesselStorageView,
 * StorageTransferBridge, CapabilityTransactionSimulator) in accordance with VMSD v1.0.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
class StorageSymmetryCoreTest {

    @Test
    @DisplayName("VesselStorageView: Slot and tank read/write operations and boundaries")
    void testVesselStorageViewOperations() {
        Object[] arrayStorage = new Object[]{"minecraft:diamond", null, "minecraft:iron_ingot"};
        List<Object> listStorage = new ArrayList<>(List.of("create:gear", "mekanism:circuit"));
        Map<Integer, Object> mapStorage = new HashMap<>();
        mapStorage.put(0, "irons_spellbooks:scroll");

        // slotCount
        assertEquals(3, VesselStorageView.slotCount(arrayStorage));
        assertEquals(2, VesselStorageView.slotCount(listStorage));
        assertEquals(1, VesselStorageView.slotCount(mapStorage));
        assertEquals(0, VesselStorageView.slotCount(null));

        // readSlot
        assertEquals("minecraft:diamond", VesselStorageView.readSlot(arrayStorage, 0));
        assertNull(VesselStorageView.readSlot(arrayStorage, 1));
        assertEquals("minecraft:iron_ingot", VesselStorageView.readSlot(arrayStorage, 2));
        assertNull(VesselStorageView.readSlot(arrayStorage, 3)); // boundary out of bounds
        assertNull(VesselStorageView.readSlot(arrayStorage, -1));

        // writeSlot
        VesselStorageView.writeSlot(arrayStorage, 1, "minecraft:gold_ingot");
        assertEquals("minecraft:gold_ingot", VesselStorageView.readSlot(arrayStorage, 1));

        VesselStorageView.writeSlot(listStorage, 0, "create:shaft");
        assertEquals("create:shaft", VesselStorageView.readSlot(listStorage, 0));

        // slotLimit and validity
        assertEquals(64, VesselStorageView.slotLimit(arrayStorage, 0));
        assertTrue(VesselStorageView.isItemValid(arrayStorage, 0, "minecraft:stick"));
        assertFalse(VesselStorageView.isItemValid(arrayStorage, 5, "minecraft:stick")); // out of bounds
        assertFalse(VesselStorageView.isItemValid(arrayStorage, 0, null));

        // Tanks
        assertEquals(3, VesselStorageView.tankCount(arrayStorage));
        assertEquals("minecraft:diamond", VesselStorageView.readFluidTank(arrayStorage, 0));

        // viewIterator
        Iterator<Object> iter = VesselStorageView.viewIterator(arrayStorage);
        assertNotNull(iter);
        assertTrue(iter.hasNext());
        assertEquals("minecraft:diamond", iter.next());

        // supports
        assertTrue(VesselStorageView.supportsInsertion(arrayStorage));
        assertTrue(VesselStorageView.supportsExtraction(arrayStorage));

        // clearAll
        VesselStorageView.clearAll(arrayStorage);
        assertNull(VesselStorageView.readSlot(arrayStorage, 0));
        assertNull(VesselStorageView.readSlot(arrayStorage, 1));

        VesselStorageView.clearAll(listStorage);
        assertEquals(0, listStorage.size());
    }

    @Test
    @DisplayName("CapabilityTransactionSimulator: Nested rollback journal and zero-allocation tracking")
    void testCapabilityTransactionSimulatorRollback() {
        CapabilityTransactionSimulator sim = new CapabilityTransactionSimulator(16, 32, 8);

        sim.setSlotAmount(0, 100L);
        sim.setSlotAmount(1, 50L);

        assertEquals(100L, sim.getSlotAmount(0));
        assertEquals(50L, sim.getSlotAmount(1));
        assertFalse(sim.isInTransaction());
        assertEquals(0, sim.getCurrentDepth());

        // Root transaction begin (depth 1)
        int depth1 = sim.beginNestedTransaction();
        assertEquals(1, depth1);
        assertTrue(sim.isInTransaction());

        sim.recordMutation(0, 120L); // slot 0: 100 -> 120
        assertEquals(120L, sim.getSlotAmount(0));

        // Nested transaction begin (depth 2)
        int depth2 = sim.beginNestedTransaction();
        assertEquals(2, depth2);

        sim.recordMutation(1, 80L);  // slot 1: 50 -> 80
        sim.recordMutation(0, 150L); // slot 0: 120 -> 150
        assertEquals(150L, sim.getSlotAmount(0));
        assertEquals(80L, sim.getSlotAmount(1));

        // Abort nested transaction at depth 2 -> should restore slot 1 to 50, slot 0 to 120
        int restored = sim.abortAndRollbackTransaction();
        assertEquals(2, restored);
        assertEquals(1, sim.getCurrentDepth());
        assertEquals(120L, sim.getSlotAmount(0));
        assertEquals(50L, sim.getSlotAmount(1));

        // Commit depth 1
        int committed = sim.commitTransaction();
        assertEquals(1, committed);
        assertEquals(0, sim.getCurrentDepth());
        assertFalse(sim.isInTransaction());
        assertEquals(120L, sim.getSlotAmount(0)); // mutation remains committed

        // Reset
        sim.reset();
        assertEquals(0L, sim.getSlotAmount(0));
        assertEquals(0, sim.getSnapshotCount());

        // Exception contracts
        assertThrows(IllegalStateException.class, sim::commitTransaction);
        assertThrows(IllegalStateException.class, sim::abortAndRollbackTransaction);
    }

    @Test
    @DisplayName("StorageTransferBridge: Item insertion, extraction, and Bukkit bulk transfers")
    void testStorageTransferBridgeItemOperations() {
        Object[] inv = new Object[3];

        // insertItem simulate vs execute
        Object remainingSim = StorageTransferBridge.insertItem(inv, 0, "itemA", true);
        assertNull(remainingSim);
        assertNull(inv[0]); // simulate did not mutate

        Object remainingExec = StorageTransferBridge.insertItem(inv, 0, "itemA", false);
        assertNull(remainingExec);
        assertEquals("itemA", inv[0]); // executed mutation

        // extractItem simulate vs execute
        Object extractedSim = StorageTransferBridge.extractItem(inv, 0, 1, true);
        assertEquals("itemA", extractedSim);
        assertEquals("itemA", inv[0]); // simulate did not delete

        Object extractedExec = StorageTransferBridge.extractItem(inv, 0, 1, false);
        assertEquals("itemA", extractedExec);
        assertNull(inv[0]); // executed mutation removed item

        // Bukkit bulkInsertBukkit
        Object[] bulkItems = new Object[]{"item1", "item2", "item3", "item4"};
        Map<Integer, Object> rejected = new HashMap<>();
        StorageTransferBridge.bulkInsertBukkit(inv, bulkItems, rejected);

        assertEquals("item1", inv[0]);
        assertEquals("item2", inv[1]);
        assertEquals("item3", inv[2]);
        assertEquals(1, rejected.size()); // item4 could not fit in 3-slot inventory
        assertEquals("item4", rejected.get(3));
    }

    @Test
    @DisplayName("StorageTransferBridge: Fabric transfer with TransactionContext integration")
    void testStorageTransferBridgeFabricTransactions() {
        CapabilityTransactionSimulator sim = new CapabilityTransactionSimulator();
        sim.setSlotAmount(0, 10L);

        // Begin transaction
        sim.beginNestedTransaction();

        // insertFabric using simulator as transactionContext
        long accepted = StorageTransferBridge.insertFabric(sim, "minecraft:stone", 20L, sim);
        assertEquals(20L, accepted);
        assertEquals(30L, sim.getSlotAmount(0));

        // extractFabric
        long extracted = StorageTransferBridge.extractFabric(sim, "minecraft:stone", 5L, sim);
        assertEquals(5L, extracted);
        assertEquals(25L, sim.getSlotAmount(0));

        // Rollback transaction
        sim.abortAndRollbackTransaction();
        assertEquals(10L, sim.getSlotAmount(0)); // safely restored to original balance!
    }

    @Test
    @DisplayName("StorageTransferBridge: Fluid math and conversions")
    void testStorageTransferBridgeFluidMath() {
        // 1 Bucket = 1,000 mB = 81,000 Droplets
        assertEquals(1_000L, StorageTransferBridge.dropletsToMillibuckets(81_000L));
        assertEquals(81_000L, StorageTransferBridge.millibucketsToDroplets(1_000L));

        // 1 mB = 81 Droplets
        assertEquals(1L, StorageTransferBridge.dropletsToMillibuckets(81L));
        assertEquals(81L, StorageTransferBridge.millibucketsToDroplets(1L));

        // fill and drain exact
        int filled = StorageTransferBridge.fillFluid("tank", 500, StorageTransferBridge.FLUID_ACTION_EXECUTE);
        assertEquals(500, filled);

        Object drained = StorageTransferBridge.drainFluidExact("tank", "water_500", StorageTransferBridge.FLUID_ACTION_EXECUTE);
        assertEquals("water_500", drained);
    }
}
