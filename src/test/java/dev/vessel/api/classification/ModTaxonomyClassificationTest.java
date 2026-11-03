package dev.vessel.api.classification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Verification test suite for Chapter 2 Mod Taxonomy Classification.
 * Tests 3-Tier classification, bytecode scanning, and Folia thread-safety gate.
 */
class ModTaxonomyClassificationTest {

    private PreflightBytecodeTaxonomyScanner scanner;
    private PureVesselComplianceValidator pureValidator;
    private MixedVesselReferenceDetector mixedDetector;
    private FoliaThreadSafetyAuditScanner foliaScanner;

    @BeforeEach
    void setUp() {
        scanner = new PreflightBytecodeTaxonomyScanner();
        pureValidator = new PureVesselComplianceValidator();
        mixedDetector = new MixedVesselReferenceDetector();
        foliaScanner = new FoliaThreadSafetyAuditScanner();
    }

    @Test
    @DisplayName("Tier 1: Pure Vessel Mod with Vessel Super-API and Vanilla Mojmap is certified")
    void testPureVesselCertification() {
        Set<String> references = Set.of(
            "dev.vessel.api.event.SovereignEventBus",
            "dev.vessel.api.capability.VesselUnifiedCapability",
            "net.minecraft.world.level.Level",
            "net.minecraft.world.entity.player.Player",
            "java.lang.Object",
            "java.lang.String"
        );
        Set<String> methods = Set.of(
            "dev.vessel.api.event.SovereignEventBus.dispatch",
            "net.minecraft.world.level.Level.getBlockState"
        );

        ModClassificationReport report = scanner.classifyArtifact(
            "pure-vessel-sample",
            references,
            methods,
            false,
            pureValidator,
            mixedDetector,
            foliaScanner
        );

        assertThat(report.tier()).isEqualTo(ModTaxonomyTier.VESSEL);
        assertThat(report.isVesselNative()).isTrue();
        assertThat(report.classifiedLoader()).isEqualTo("VESSEL");
        assertThat(report.foliaThreadSafe()).isTrue();
        assertThat(report.demotedFromFolia()).isFalse();
        assertThat(report.auditViolations()).isEmpty();
        assertThat(report.detectedExternalLoaders()).isEmpty();
    }

    @Test
    @DisplayName("Tier 1: Pure Minecraft mod with zero external loader imports is certified as Vessel")
    void testPureMinecraftDependencyRule() {
        Set<String> references = Set.of(
            "net.minecraft.server.MinecraftServer",
            "net.minecraft.world.item.ItemStack",
            "java.lang.Object"
        );
        Set<String> methods = Set.of(
            "net.minecraft.server.MinecraftServer.tickChildren"
        );

        ModClassificationReport report = scanner.classifyArtifact(
            "pure-mojmap-mod",
            references,
            methods,
            false,
            pureValidator,
            mixedDetector,
            foliaScanner
        );

        assertThat(report.tier()).isEqualTo(ModTaxonomyTier.VESSEL);
        assertThat(report.isVesselNative()).isTrue();
        assertThat(report.classifiedLoader()).isEqualTo("VESSEL");
        assertThat(pureValidator.isCompliant(references)).isTrue();
    }

    @Test
    @DisplayName("Tier 2: Mixed-Vessel Mod referencing Vessel API and Forge API is detected")
    void testMixedVesselDetectionWithForge() {
        Set<String> references = Set.of(
            "dev.vessel.api.event.SovereignEventBus",
            "net.minecraftforge.eventbus.api.Event",
            "net.minecraftforge.fml.common.Mod",
            "net.minecraft.world.item.Item"
        );
        Set<String> methods = Set.of(
            "dev.vessel.api.event.SovereignEventBus.register",
            "net.minecraftforge.eventbus.api.Event.getResult"
        );

        ModClassificationReport report = scanner.classifyArtifact(
            "hybrid-mod",
            references,
            methods,
            false,
            pureValidator,
            mixedDetector,
            foliaScanner
        );

        assertThat(report.tier()).isEqualTo(ModTaxonomyTier.MIXED_VESSEL);
        assertThat(report.isHybridMixed()).isTrue();
        assertThat(report.classifiedLoader()).isEqualTo("MIXED_VESSEL");
        assertThat(report.detectedExternalLoaders()).contains("FORGE");
        assertThat(report.auditViolations()).isNotEmpty();
        assertThat(mixedDetector.isMixedVessel(references)).isTrue();
    }

    @Test
    @DisplayName("Tier 2: Mixed-Vessel Mod referencing Vessel API and Fabric API is detected")
    void testMixedVesselDetectionWithFabric() {
        Set<String> references = Set.of(
            "dev.vessel.api.storage.VesselStorageView",
            "net.fabricmc.api.ModInitializer",
            "net.fabricmc.fabric.api.transfer.v1.storage.Storage"
        );
        Set<String> methods = Set.of(
            "net.fabricmc.api.ModInitializer.onInitialize"
        );

        ModClassificationReport report = scanner.classifyArtifact(
            "fabric-hybrid-mod",
            references,
            methods,
            false,
            pureValidator,
            mixedDetector,
            foliaScanner
        );

        assertThat(report.tier()).isEqualTo(ModTaxonomyTier.MIXED_VESSEL);
        assertThat(report.isHybridMixed()).isTrue();
        assertThat(report.detectedExternalLoaders()).contains("FABRIC");
    }

    @Test
    @DisplayName("Tier 3: Single-Loader Fabric mod without Vessel API is classified as FABRIC")
    void testSingleLoaderFabric() {
        Set<String> references = Set.of(
            "net.fabricmc.api.ModInitializer",
            "net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents",
            "java.lang.Object"
        );
        Set<String> methods = Set.of(
            "net.fabricmc.api.ModInitializer.onInitialize"
        );

        ModClassificationReport report = scanner.classifyArtifact(
            "pure-fabric-mod",
            references,
            methods,
            false,
            pureValidator,
            mixedDetector,
            foliaScanner
        );

        assertThat(report.tier()).isEqualTo(ModTaxonomyTier.SINGLE_LOADER);
        assertThat(report.isLegacySingleLoader()).isTrue();
        assertThat(report.classifiedLoader()).isEqualTo("FABRIC");
        assertThat(report.detectedExternalLoaders()).contains("FABRIC");
    }

    @Test
    @DisplayName("Tier 3: Single-Loader NeoForge mod is classified as NEOFORGE")
    void testSingleLoaderNeoForge() {
        Set<String> references = Set.of(
            "net.neoforged.fml.common.Mod",
            "net.neoforged.bus.api.SubscribeEvent",
            "java.lang.Object"
        );
        Set<String> methods = Set.of();

        ModClassificationReport report = scanner.classifyArtifact(
            "neoforge-sample",
            references,
            methods,
            false,
            pureValidator,
            mixedDetector,
            foliaScanner
        );

        assertThat(report.tier()).isEqualTo(ModTaxonomyTier.SINGLE_LOADER);
        assertThat(report.classifiedLoader()).isEqualTo("NEOFORGE");
    }

    @Test
    @DisplayName("Tier 3: Folia plugin passing static thread safety audit gate is certified as FOLIA")
    void testFoliaPluginPassingThreadSafetyGate() {
        Set<String> references = Set.of(
            "org.bukkit.plugin.java.JavaPlugin",
            "io.papermc.paper.threadedregions.RegionizedServer",
            "java.lang.Object"
        );
        Set<String> safeMethods = Set.of(
            "io.papermc.paper.threadedregions.RegionizedServer.getRegionScheduler",
            "org.bukkit.plugin.java.JavaPlugin.getLogger"
        );

        ModClassificationReport report = scanner.classifyArtifact(
            "folia-clean-plugin",
            references,
            safeMethods,
            true, // folia-supported: true
            pureValidator,
            mixedDetector,
            foliaScanner
        );

        assertThat(report.tier()).isEqualTo(ModTaxonomyTier.SINGLE_LOADER);
        assertThat(report.classifiedLoader()).isEqualTo("FOLIA");
        assertThat(report.foliaThreadSafe()).isTrue();
        assertThat(report.demotedFromFolia()).isFalse();
        assertThat(report.auditViolations()).isEmpty();
    }

    @Test
    @DisplayName("Tier 3: Folia plugin with unsafe Bukkit.getWorld call is demoted to PAPER")
    void testFoliaPluginDemotionOnUnsafeCall() {
        Set<String> references = Set.of(
            "org.bukkit.plugin.java.JavaPlugin",
            "io.papermc.paper.threadedregions.RegionizedServer",
            "org.bukkit.Bukkit"
        );
        Set<String> unsafeMethods = Set.of(
            "org.bukkit.Bukkit.getWorld",
            "org.bukkit.plugin.java.JavaPlugin.getLogger"
        );

        ModClassificationReport report = scanner.classifyArtifact(
            "folia-unsafe-plugin",
            references,
            unsafeMethods,
            true, // folia-supported: true but has Bukkit.getWorld
            pureValidator,
            mixedDetector,
            foliaScanner
        );

        assertThat(report.tier()).isEqualTo(ModTaxonomyTier.SINGLE_LOADER);
        assertThat(report.classifiedLoader()).isEqualTo("PAPER");
        assertThat(report.foliaThreadSafe()).isFalse();
        assertThat(report.demotedFromFolia()).isTrue();
        assertThat(report.auditViolations()).contains("org.bukkit.Bukkit.getWorld");
    }

    @Test
    @DisplayName("Bytecode scanner correctly parses ASM bytecode instructions and extracts references")
    void testAsmBytecodeScanning() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(
            Opcodes.V21,
            Opcodes.ACC_PUBLIC,
            "dev/sample/TestVesselModClass",
            null,
            "java/lang/Object",
            new String[] { "dev/vessel/api/event/VesselEvent" }
        );

        MethodVisitor mv = cw.visitMethod(
            Opcodes.ACC_PUBLIC,
            "executeTest",
            "()V",
            null,
            null
        );
        mv.visitCode();
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "dev/vessel/api/event/SovereignEventBus",
            "dispatch",
            "(Ljava/lang/Object;)V",
            false
        );
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        cw.visitEnd();

        byte[] classBytes = cw.toByteArray();

        Set<String> classSink = new HashSet<>();
        Set<String> methodSink = new HashSet<>();
        scanner.scanClassBytes(classBytes, classSink, methodSink);

        assertThat(classSink).contains(
            "java.lang.Object",
            "dev.vessel.api.event.VesselEvent",
            "dev.vessel.api.event.SovereignEventBus"
        );
        assertThat(methodSink).contains(
            "dev.vessel.api.event.SovereignEventBus.dispatch"
        );
    }
}
