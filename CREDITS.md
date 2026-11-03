# Vessel-api — Third-Party Open-Source Credits, Attributions & License Compliance

- **Version**: `1.0.0`
- **Last Updated**: `2026-11-03`
- **Repository**: `VesselLoader/Vessel-api` (`Works/Vessel-api`)

`Vessel-api` (`dev.vessel.api.*`) is the public, clean-room VSTM Super-API and Bytecode Transmutation Receptor library for the **VESSEL** Sovereign Standalone Universal Minecraft Server Runtime, bidirectionally interconnected with `VesselLoader/Vessel` (`Works/VESSEL`).

---

## 1. Direct Compile & Test Dependencies

| Component | Authors / Copyright Holder | License | Official URL | Usage in `Vessel-api` |
| :--- | :--- | :--- | :--- | :--- |
| **OW2 ASM (`org.ow2.asm:asm`, `asm-tree` 9.7)** | INRIA, France Telecom | **BSD-3-Clause** | https://asm.ow2.io | Used in `dev.vessel.api.classification.*` and `dev.vessel.api.transmutation.*` for zero-allocation bytecode scanning and VSTM call-site transmutation. |
| **JUnit 5 (`org.junit.jupiter`) & AssertJ** | JUnit Team, AssertJ Team | **EPL-2.0 / Apache-2.0** | https://junit.org/junit5 | Used exclusively in `src/test/java/dev/vessel/api/**` (`157` unit and symmetry tests). |

---

## 2. Clean-Room Ecosystem Symmetry Receptors & Algorithmic Credits

All 135 classes in `src/main/java/dev/vessel/api/**` are **100% original clean-room Java 21 implementations** with zero `net.minecraft.*` or copyleft imports. We gratefully credit the following upstream ecosystems and performance projects whose public API contracts or optimization concepts are bridged or adapted by `Vessel-api`:

1. **Supported Mod & Plugin Loader Ecosystems (Clean-Room Symmetry Adapters):**
   - **FabricMC** (`fabric-loader`, `fabric-api`, `fabric-transfer-api-v1` — **Apache-2.0**, https://github.com/FabricMC/fabric): Bridged in `FabricRegistrySymmetryBridge`, `FabricStorageSymmetryAdapter`, `FabricTransactionContext`, `FabricEnergySymmetryAdapter`, `FabricResourceReloadSymmetryAdapter`, and `FabricBytecodeTransmutationAdapter`.
   - **QuiltMC** (`quilt-loader`, `quilt-standard-libraries` QSL — **Apache-2.0**, https://github.com/QuiltMC/quilt-standard-libraries): Bridged in `QuiltBytecodeTransmutationAdapter` and `ModTaxonomyClassifier`.
   - **MinecraftForge** (`MinecraftForge`, `EventBus`, `ForgeConfigSpec` — **LGPL-2.1**, https://github.com/MinecraftForge/MinecraftForge): Bridged clean-room in `ForgeBlockSymmetryAdapter`, `ForgeConfigSpecSymmetryAdapter`, `ForgeEnergySymmetryAdapter`, `ForgeEntitySymmetryAdapter`, `ForgeItemSymmetryAdapter`, `ForgeStorageSymmetryAdapter`, `DeferredRegisterSymmetryReplay`, and `ForgeBytecodeTransmutationAdapter`.
   - **NeoForged** (`NeoForge`, `Bus`, `AttachmentType`, `DataMap` — **LGPL-2.1**, https://github.com/neoforged/NeoForge): Bridged clean-room in `NeoForgeBytecodeTransmutationAdapter`, `AttachmentMemoryEvictor`, and `VesselUnifiedCapability`.
   - **Bukkit, SpigotMC, PaperMC & Folia** (Public Plugin API contracts — **GPL-3.0 / MIT-Licensed Contributors**, https://github.com/PaperMC/Paper, https://github.com/PaperMC/Folia): Bridged clean-room in `FoliaRegionExecutionBridge`, `BukkitSchedulerSymmetryBridge`, `BukkitBlockSymmetryAdapter`, `BukkitCommandSymmetryAdapter`, `BukkitToBrigadierTranspiler`, `BukkitYamlSymmetryAdapter`, `BukkitLivingEntitySymmetryAdapter`, `BukkitItemSymmetryAdapter`, `BukkitInventorySymmetryAdapter`, `BukkitWorldSymmetryAdapter`, `BukkitChunkSymmetryAdapter`, and `BukkitBytecodeTransmutationAdapter`.
   - **Mojang Studios** (`Brigadier` & `DataFixerUpper` — **MIT License**, https://github.com/Mojang/brigadier).

2. **Algorithmic & Performance Architecture Inspirations:**
   - **PaperMC / Folia** (`RegionalActorStrand`, `GlobalRegionSyncBarrier`, `LockFreeMailboxRingBuffer`, `ActorMailboxPaddedRingQueue`).
   - **Lithium / Sodium / FerriteCore / C2ME / Krypton / MemoryLeakFix / ModernFix** (`WorldEntitySpatialGrid`, `ExplosionResistanceCache`, `CoordinateFastPacker`, `ChunkTicketGovernor`, `WeakCapabilitySentinel`, `AttachmentMemoryEvictor`, `PacketBandwidthOptimizer`, `CustomPayloadBypassFilter`, `SyntheticClientAckFactory`).
   - **GeyserMC & Floodgate** (MIT License, https://geysermc.org — `BedrockDisplayEntityTranspiler`).
