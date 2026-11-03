package dev.vessel.api.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests verifying cross-loader storage symmetry adapters across Fabric Transfer API v1,
 * Forge/NeoForge IItemHandler and IFluidHandler, and Bukkit Inventory.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class UniversalStorageSymmetryTest {

    @Test
    @DisplayName("Fabric Transfer API: Should insert, extract, and support transactional rollback")
    public void testFabricStorageOperationsAndRollback() {
        SimpleFabricStorageContainer container = new SimpleFabricStorageContainer("minecraft:water", 810_000L, 81_000L);

        // Verify initial state
        assertThat(FabricStorageSymmetryAdapter.supportsInsertion(container)).isTrue();
        assertThat(FabricStorageSymmetryAdapter.supportsExtraction(container)).isTrue();
        assertThat(FabricStorageSymmetryAdapter.getVersion(container)).isEqualTo(0L);

        // Test non-transactional insertion
        long inserted = FabricStorageSymmetryAdapter.insertFabric(container, "minecraft:water", 162_000L, null);
        assertThat(inserted).isEqualTo(162_000L);
        assertThat(container.getCurrentAmount()).isEqualTo(243_000L);
        assertThat(FabricStorageSymmetryAdapter.getVersion(container)).isEqualTo(1L);

        // Test transactional insertion with rollback
        FabricTransactionContext tx = new FabricTransactionContext();
        long txInserted = FabricStorageSymmetryAdapter.insertFabric(container, "minecraft:water", 81_000L, tx);
        assertThat(txInserted).isEqualTo(81_000L);
        assertThat(container.getCurrentAmount()).isEqualTo(324_000L);

        // Rollback transaction
        tx.abort();
        assertThat(container.getCurrentAmount()).isEqualTo(243_000L);

        // Test transactional insertion with commit
        FabricTransactionContext commitTx = new FabricTransactionContext();
        long committedInsert = FabricStorageSymmetryAdapter.insertFabric(container, "minecraft:water", 81_000L, commitTx);
        assertThat(committedInsert).isEqualTo(81_000L);
        commitTx.commit();
        assertThat(container.getCurrentAmount()).isEqualTo(324_000L);

        // Test extraction
        long extracted = FabricStorageSymmetryAdapter.extractFabric(container, "minecraft:water", 81_000L, null);
        assertThat(extracted).isEqualTo(81_000L);
        assertThat(container.getCurrentAmount()).isEqualTo(243_000L);

        // Wrong resource is rejected
        long wrongResource = FabricStorageSymmetryAdapter.insertFabric(container, "minecraft:lava", 81_000L, null);
        assertThat(wrongResource).isEqualTo(0L);
    }

    @Test
    @DisplayName("Fabric Fluid Droplet Conversion: 81 droplets per millibucket")
    public void testFabricDropletsConversion() {
        long oneBucketDroplets = FabricStorageSymmetryAdapter.FABRIC_DROPLETS_PER_FORGE_BUCKET;
        assertThat(FabricStorageSymmetryAdapter.dropletsToMillibuckets(oneBucketDroplets)).isEqualTo(1_000L);
        assertThat(FabricStorageSymmetryAdapter.millibucketsToDroplets(1_000L)).isEqualTo(81_000L);
        assertThat(FabricStorageSymmetryAdapter.dropletsToMillibuckets(81L)).isEqualTo(1L);
    }

    @Test
    @DisplayName("Forge ItemHandler: Should handle slot counts, stack queries, simulation and execution")
    public void testForgeItemHandlerOperations() {
        SimpleForgeInventoryContainer container = new SimpleForgeInventoryContainer(27, 64);

        assertThat(ForgeItemHandlerSymmetryAdapter.getSlots(container)).isEqualTo(27);
        assertThat(ForgeItemHandlerSymmetryAdapter.getSlotLimit(container, 0)).isEqualTo(64);

        // Empty slot inspection
        Object initialSlot = ForgeItemHandlerSymmetryAdapter.getStackInSlot(container, 0);
        assertThat(initialSlot).isEqualTo(SimpleSlotItem.EMPTY);

        SimpleSlotItem diamondStack = SimpleSlotItem.of("minecraft:diamond", 10);
        assertThat(ForgeItemHandlerSymmetryAdapter.isItemValid(container, 0, diamondStack)).isTrue();

        // Simulate insertion (should not mutate slot)
        Object simResult = ForgeItemHandlerSymmetryAdapter.insertItem(container, 0, diamondStack, true);
        assertThat(simResult).isEqualTo(SimpleSlotItem.EMPTY); // Empty remainder means all would be accepted
        assertThat(ForgeItemHandlerSymmetryAdapter.getStackInSlot(container, 0)).isEqualTo(SimpleSlotItem.EMPTY);

        // Execute insertion
        Object execResult = ForgeItemHandlerSymmetryAdapter.insertItem(container, 0, diamondStack, false);
        assertThat(execResult).isEqualTo(SimpleSlotItem.EMPTY);
        assertThat(ForgeItemHandlerSymmetryAdapter.getStackInSlot(container, 0)).isEqualTo(diamondStack);

        // Partial extraction simulation
        Object simExtract = ForgeItemHandlerSymmetryAdapter.extractItem(container, 0, 4, true);
        assertThat(((SimpleSlotItem) simExtract).count()).isEqualTo(4);
        assertThat(((SimpleSlotItem) ForgeItemHandlerSymmetryAdapter.getStackInSlot(container, 0)).count()).isEqualTo(10);

        // Partial extraction execution
        Object execExtract = ForgeItemHandlerSymmetryAdapter.extractItem(container, 0, 4, false);
        assertThat(((SimpleSlotItem) execExtract).count()).isEqualTo(4);
        assertThat(((SimpleSlotItem) ForgeItemHandlerSymmetryAdapter.getStackInSlot(container, 0)).count()).isEqualTo(6);
    }

    @Test
    @DisplayName("Forge FluidHandler: Should handle tanks, fill, and drain with simulation support")
    public void testForgeFluidHandlerOperations() {
        SimpleForgeInventoryContainer container = new SimpleForgeInventoryContainer(9);
        container.addTank(new SimpleFluidTank("minecraft:empty", 0L, 4_000L));

        assertThat(ForgeItemHandlerSymmetryAdapter.getTanks(container)).isEqualTo(1);

        SimpleFluidTank lava1000 = new SimpleFluidTank("minecraft:lava", 1_000L, 1_000L);

        // Simulate filling lava
        int simFilled = ForgeItemHandlerSymmetryAdapter.fillFluid(container, lava1000, ForgeItemHandlerSymmetryAdapter.ACTION_SIMULATE);
        assertThat(simFilled).isEqualTo(1_000);
        assertThat(((SimpleFluidTank) ForgeItemHandlerSymmetryAdapter.getFluidInTank(container, 0)).isEmpty()).isTrue();

        // Execute filling lava
        int execFilled = ForgeItemHandlerSymmetryAdapter.fillFluid(container, lava1000, ForgeItemHandlerSymmetryAdapter.ACTION_EXECUTE);
        assertThat(execFilled).isEqualTo(1_000);
        SimpleFluidTank tankState = (SimpleFluidTank) ForgeItemHandlerSymmetryAdapter.getFluidInTank(container, 0);
        assertThat(tankState.fluidId()).isEqualTo("minecraft:lava");
        assertThat(tankState.millibuckets()).isEqualTo(1_000L);

        // Drain fluid execution
        Object drained = ForgeItemHandlerSymmetryAdapter.drainFluid(container, 500, ForgeItemHandlerSymmetryAdapter.ACTION_EXECUTE);
        assertThat(((SimpleFluidTank) drained).millibuckets()).isEqualTo(500L);
        SimpleFluidTank remainingTank = (SimpleFluidTank) ForgeItemHandlerSymmetryAdapter.getFluidInTank(container, 0);
        assertThat(remainingTank.millibuckets()).isEqualTo(500L);
    }

    @Test
    @DisplayName("Bukkit Inventory: Should support addItem, removeItem, clear, and firstEmpty")
    public void testBukkitInventoryOperations() {
        SimpleBukkitInventoryContainer inventory = new SimpleBukkitInventoryContainer(27);

        assertThat(BukkitInventorySymmetryAdapter.getSize(inventory)).isEqualTo(27);
        assertThat(BukkitInventorySymmetryAdapter.firstEmpty(inventory)).isEqualTo(0);

        // Direct setItem
        SimpleSlotItem gold = SimpleSlotItem.of("minecraft:gold_ingot", 32);
        BukkitInventorySymmetryAdapter.setItem(inventory, 0, gold);
        assertThat(BukkitInventorySymmetryAdapter.getItem(inventory, 0)).isEqualTo(gold);
        assertThat(BukkitInventorySymmetryAdapter.firstEmpty(inventory)).isEqualTo(1);

        // Add items: should top up slot 0 to 64, then place remaining 16 in slot 1
        SimpleSlotItem extraGold = SimpleSlotItem.of("minecraft:gold_ingot", 48);
        Map<?, ?> leftovers = BukkitInventorySymmetryAdapter.addItem(inventory, new Object[]{extraGold});
        assertThat(leftovers).isEmpty();

        assertThat(((SimpleSlotItem) BukkitInventorySymmetryAdapter.getItem(inventory, 0)).count()).isEqualTo(64);
        assertThat(((SimpleSlotItem) BukkitInventorySymmetryAdapter.getItem(inventory, 1)).count()).isEqualTo(16);

        // Remove item: slot 0 (64) is decremented by 20 to 44, leaving slot 1 at 16
        SimpleSlotItem toRemove = SimpleSlotItem.of("minecraft:gold_ingot", 20);
        Map<?, ?> removeLeftovers = BukkitInventorySymmetryAdapter.removeItem(inventory, new Object[]{toRemove});
        assertThat(removeLeftovers).isEmpty();
        assertThat(((SimpleSlotItem) BukkitInventorySymmetryAdapter.getItem(inventory, 0)).count()).isEqualTo(44);
        assertThat(((SimpleSlotItem) BukkitInventorySymmetryAdapter.getItem(inventory, 1)).count()).isEqualTo(16);

        // Clear specific slot and all
        BukkitInventorySymmetryAdapter.clear(inventory, 0);
        assertThat(BukkitInventorySymmetryAdapter.getItem(inventory, 0)).isEqualTo(SimpleSlotItem.EMPTY);

        BukkitInventorySymmetryAdapter.clear(inventory);
        assertThat(BukkitInventorySymmetryAdapter.firstEmpty(inventory)).isEqualTo(0);
    }

    @Test
    @DisplayName("Cross-Loader Transmutation Bridge: Bridge Bukkit items to Forge and Fabric fluids to Forge")
    public void testCrossLoaderTransmutationBridge() {
        // 1. Cross-Loader Item Bridge: Transfer items from Bukkit Inventory to Forge Container
        SimpleBukkitInventoryContainer bukkitSource = new SimpleBukkitInventoryContainer(9);
        BukkitInventorySymmetryAdapter.setItem(bukkitSource, 0, SimpleSlotItem.of("minecraft:iron_ingot", 30));

        SimpleForgeInventoryContainer forgeTarget = new SimpleForgeInventoryContainer(9);

        // Extract from Bukkit
        SimpleSlotItem bukkitItem = (SimpleSlotItem) BukkitInventorySymmetryAdapter.getItem(bukkitSource, 0);
        BukkitInventorySymmetryAdapter.clear(bukkitSource, 0);

        // Insert into Forge
        Object remainder = ForgeItemHandlerSymmetryAdapter.insertItem(forgeTarget, 0, bukkitItem, false);
        assertThat(remainder).isEqualTo(SimpleSlotItem.EMPTY);
        assertThat(ForgeItemHandlerSymmetryAdapter.getStackInSlot(forgeTarget, 0)).isEqualTo(bukkitItem);

        // 2. Cross-Loader Fluid Bridge: Fabric droplets to Forge tank
        SimpleFabricStorageContainer fabricTank = new SimpleFabricStorageContainer("minecraft:water", 162_000L, 162_000L); // 2 buckets (162,000 droplets)
        SimpleForgeInventoryContainer forgeFluidReceiver = new SimpleForgeInventoryContainer(1);
        forgeFluidReceiver.addTank(new SimpleFluidTank("minecraft:empty", 0L, 5_000L));

        // Extract 81,000 droplets (1 bucket) from Fabric
        long extractedDroplets = FabricStorageSymmetryAdapter.extractFabric(fabricTank, "minecraft:water", 81_000L, null);
        assertThat(extractedDroplets).isEqualTo(81_000L);

        // Convert droplets to Forge millibuckets
        long millibuckets = FabricStorageSymmetryAdapter.dropletsToMillibuckets(extractedDroplets);
        assertThat(millibuckets).isEqualTo(1_000L);

        // Fill Forge fluid tank
        int acceptedMb = ForgeItemHandlerSymmetryAdapter.fillFluid(
                forgeFluidReceiver,
                new SimpleFluidTank("minecraft:water", millibuckets, millibuckets),
                ForgeItemHandlerSymmetryAdapter.ACTION_EXECUTE
        );
        assertThat(acceptedMb).isEqualTo(1_000);

        SimpleFluidTank resultingTank = (SimpleFluidTank) ForgeItemHandlerSymmetryAdapter.getFluidInTank(forgeFluidReceiver, 0);
        assertThat(resultingTank.fluidId()).isEqualTo("minecraft:water");
        assertThat(resultingTank.millibuckets()).isEqualTo(1_000L);
    }
}
