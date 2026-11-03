package dev.vessel.api.transmutation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Comprehensive test suite verifying Chapter 6 VSTM Transmutation pipeline and adapters.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 */
public final class VstmTransmutationTest {

    private VstmTransmutationPipeline pipeline;

    @BeforeEach
    public void setUp() {
        this.pipeline = new VstmTransmutationPipeline();
    }

    @Test
    @DisplayName("Verify pipeline initialization and symmetry adapter mounting")
    public void testPipelineInitialization() {
        assertThat(pipeline.getTransmutator()).isNotNull();
        assertThat(pipeline.getForgeAdapter()).isNotNull();
        assertThat(pipeline.getNeoForgeAdapter()).isNotNull();
        assertThat(pipeline.getFabricAdapter()).isNotNull();
        assertThat(pipeline.getBukkitAdapter()).isNotNull();
        assertThat(pipeline.getQuiltAdapter()).isNotNull();

        assertThat(pipeline.getTransmutator().getRegisteredRuleCount()).isGreaterThan(10);
        assertThat(pipeline.getTotalClassesTransmuted()).isEqualTo(0L);
        assertThat(pipeline.getTotalNanosecondsElapsed()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Verify bypass filter on native Vessel classes")
    public void testNativeVesselBypass() {
        byte[] dummyBytecode = new byte[] {0x01, 0x02, 0x03};
        byte[] resultSlash = pipeline.transmuteClass("dev/vessel/core/NativeClass", dummyBytecode);
        byte[] resultDot = pipeline.transmuteClass("dev.vessel.core.NativeClass", dummyBytecode);

        assertThat(resultSlash).isSameAs(dummyBytecode);
        assertThat(resultDot).isSameAs(dummyBytecode);
        assertThat(pipeline.getTotalClassesTransmuted()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Verify Forge bytecode transmutation into SovereignEventBus")
    public void testForgeBytecodeTransmutation() {
        byte[] sampleBytecode = generateDummyCallerBytecode(
                "com/example/forge/SampleForgeMod",
                ForgeToVesselSymmetryAdapter.FORGE_EVENT_BUS_INTERFACE,
                "register",
                "(Ljava/lang/Object;)V"
        );

        byte[] transmuted = pipeline.transmuteClass("com.example.forge.SampleForgeMod", sampleBytecode);
        assertThat(transmuted).isNotSameAs(sampleBytecode);

        ClassReader cr = new ClassReader(transmuted);
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, 0);

        boolean foundTransmutedCall = false;
        for (MethodNode mn : classNode.methods) {
            for (int i = 0; i < mn.instructions.size(); i++) {
                AbstractInsnNode insn = mn.instructions.get(i);
                if (insn instanceof MethodInsnNode minsn) {
                    if (minsn.owner.equals(ForgeToVesselSymmetryAdapter.VESSEL_EVENT_BUS)
                            && minsn.name.equals("registerListenerObject")) {
                        foundTransmutedCall = true;
                    }
                }
            }
        }

        assertThat(foundTransmutedCall).isTrue();
        assertThat(pipeline.getTotalClassesTransmuted()).isEqualTo(1L);
        assertThat(pipeline.getTotalNanosecondsElapsed()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("Verify NeoForge bytecode transmutation into Vessel Super-API")
    public void testNeoForgeBytecodeTransmutation() {
        byte[] sampleBytecode = generateDummyCallerBytecode(
                "com/example/neoforge/SampleNeoMod",
                NeoForgeToVesselSymmetryAdapter.NEOFORGE_BUS_INTERFACE,
                "register",
                "(Ljava/lang/Object;)V"
        );

        byte[] transmuted = pipeline.transmuteClass("com.example.neoforge.SampleNeoMod", sampleBytecode);
        assertThat(transmuted).isNotSameAs(sampleBytecode);

        ClassReader cr = new ClassReader(transmuted);
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, 0);

        boolean foundTransmutedCall = false;
        for (MethodNode mn : classNode.methods) {
            for (int i = 0; i < mn.instructions.size(); i++) {
                AbstractInsnNode insn = mn.instructions.get(i);
                if (insn instanceof MethodInsnNode minsn) {
                    if (minsn.owner.equals(NeoForgeToVesselSymmetryAdapter.VESSEL_EVENT_BUS)
                            && minsn.name.equals("registerListenerObject")) {
                        foundTransmutedCall = true;
                    }
                }
            }
        }

        assertThat(foundTransmutedCall).isTrue();
    }

    @Test
    @DisplayName("Verify Fabric Transfer and Event bytecode transmutation")
    public void testFabricBytecodeTransmutation() {
        byte[] sampleBytecode = generateDummyCallerBytecode(
                "com/example/fabric/SampleFabricMod",
                FabricToVesselSymmetryAdapter.FABRIC_EVENT_CLASS,
                "invoker",
                "()Ljava/lang/Object;"
        );

        byte[] transmuted = pipeline.transmuteClass("com.example.fabric.SampleFabricMod", sampleBytecode);
        assertThat(transmuted).isNotSameAs(sampleBytecode);

        ClassReader cr = new ClassReader(transmuted);
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, 0);

        boolean foundTransmutedCall = false;
        for (MethodNode mn : classNode.methods) {
            for (int i = 0; i < mn.instructions.size(); i++) {
                AbstractInsnNode insn = mn.instructions.get(i);
                if (insn instanceof MethodInsnNode minsn) {
                    if (minsn.owner.equals(FabricToVesselSymmetryAdapter.VESSEL_EVENT_BUS)
                            && minsn.name.equals("getInvoker")) {
                        foundTransmutedCall = true;
                    }
                }
            }
        }

        assertThat(foundTransmutedCall).isTrue();
    }

    @Test
    @DisplayName("Verify Bukkit Event and Scheduler transmutation")
    public void testBukkitBytecodeTransmutation() {
        byte[] sampleBytecode = generateDummyCallerBytecode(
                "com/example/bukkit/SampleBukkitPlugin",
                BukkitToVesselSymmetryAdapter.BUKKIT_PLUGIN_MANAGER,
                "callEvent",
                "(L" + BukkitToVesselSymmetryAdapter.BUKKIT_EVENT_CLASS + ";)V"
        );

        byte[] transmuted = pipeline.transmuteClass("com.example.bukkit.SampleBukkitPlugin", sampleBytecode);
        assertThat(transmuted).isNotSameAs(sampleBytecode);

        ClassReader cr = new ClassReader(transmuted);
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, 0);

        boolean foundTransmutedCall = false;
        for (MethodNode mn : classNode.methods) {
            for (int i = 0; i < mn.instructions.size(); i++) {
                AbstractInsnNode insn = mn.instructions.get(i);
                if (insn instanceof MethodInsnNode minsn) {
                    if (minsn.owner.equals(BukkitToVesselSymmetryAdapter.VESSEL_EVENT_BUS)
                            && minsn.name.equals("post")) {
                        foundTransmutedCall = true;
                    }
                }
            }
        }

        assertThat(foundTransmutedCall).isTrue();
    }

    @Test
    @DisplayName("Verify Quilt QSL bytecode transmutation")
    public void testQuiltBytecodeTransmutation() {
        byte[] sampleBytecode = generateDummyCallerBytecode(
                "com/example/quilt/SampleQuiltMod",
                QuiltToVesselSymmetryAdapter.QUILT_EVENT_CLASS,
                "register",
                "(Ljava/lang/Object;)V"
        );

        byte[] transmuted = pipeline.transmuteClass("com.example.quilt.SampleQuiltMod", sampleBytecode);
        assertThat(transmuted).isNotSameAs(sampleBytecode);

        ClassReader cr = new ClassReader(transmuted);
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, 0);

        boolean foundTransmutedCall = false;
        for (MethodNode mn : classNode.methods) {
            for (int i = 0; i < mn.instructions.size(); i++) {
                AbstractInsnNode insn = mn.instructions.get(i);
                if (insn instanceof MethodInsnNode minsn) {
                    if (minsn.owner.equals(QuiltToVesselSymmetryAdapter.VESSEL_EVENT_BUS)
                            && minsn.name.equals("registerListenerObject")) {
                        foundTransmutedCall = true;
                    }
                }
            }
        }

        assertThat(foundTransmutedCall).isTrue();
    }

    @Test
    @DisplayName("Verify individual adapter contract detection and targets")
    public void testIndividualAdapterContracts() {
        ForgeToVesselSymmetryAdapter forge = pipeline.getForgeAdapter();
        assertThat(forge.isForgeClass("net.minecraftforge.common.MinecraftForge")).isTrue();
        assertThat(forge.isForgeClass("net/minecraftforge/eventbus/EventBus")).isTrue();
        assertThat(forge.isForgeClass("com.example.mod.MyClass")).isFalse();
        assertThat(forge.getTargetEventBusClass()).isEqualTo(ForgeToVesselSymmetryAdapter.VESSEL_EVENT_BUS);

        NeoForgeToVesselSymmetryAdapter neoforge = pipeline.getNeoForgeAdapter();
        assertThat(neoforge.isNeoForgeClass("net.neoforged.neoforge.attachment.IAttachmentHolder")).isTrue();
        assertThat(neoforge.getTargetEventBusClass()).isEqualTo(NeoForgeToVesselSymmetryAdapter.VESSEL_EVENT_BUS);

        FabricToVesselSymmetryAdapter fabric = pipeline.getFabricAdapter();
        assertThat(fabric.isFabricClass("net.fabricmc.fabric.api.event.Event")).isTrue();
        assertThat(fabric.getTargetStorageBridgeClass()).isEqualTo(FabricToVesselSymmetryAdapter.VESSEL_STORAGE);

        BukkitToVesselSymmetryAdapter bukkit = pipeline.getBukkitAdapter();
        assertThat(bukkit.isBukkitClass("org.bukkit.plugin.Plugin")).isTrue();
        assertThat(bukkit.isBukkitClass("io.papermc.paper.threadedregions.RegionizedServer")).isTrue();
        assertThat(bukkit.getTargetSchedulerClass()).isEqualTo(BukkitToVesselSymmetryAdapter.VESSEL_SCHEDULER);

        QuiltToVesselSymmetryAdapter quilt = pipeline.getQuiltAdapter();
        assertThat(quilt.isQuiltClass("org.quiltmc.qsl.base.api.event.Event")).isTrue();
        assertThat(quilt.getTargetLoaderClass()).isEqualTo(QuiltToVesselSymmetryAdapter.VESSEL_LOADER_SHIM);
    }

    @Test
    @DisplayName("Rule 17 Compliance: verify zero inner or nested classes across all 7 micro-classes")
    public void testRule17ZeroInnerClasses() {
        Class<?>[] classes = new Class<?>[] {
                VstmTransmutationPipeline.class,
                CrossLoaderOpcodeTransmutator.class,
                ForgeToVesselSymmetryAdapter.class,
                NeoForgeToVesselSymmetryAdapter.class,
                FabricToVesselSymmetryAdapter.class,
                BukkitToVesselSymmetryAdapter.class,
                QuiltToVesselSymmetryAdapter.class,
                VstmTransmutationTest.class
        };

        for (Class<?> clazz : classes) {
            assertThat(clazz.getDeclaredClasses())
                    .withFailMessage("Class %s must have 0 declared inner/nested classes per Rule 17", clazz.getName())
                    .isEmpty();
            assertThat(clazz.isAnonymousClass())
                    .withFailMessage("Class %s must not be anonymous", clazz.getName())
                    .isFalse();
        }
    }

    private byte[] generateDummyCallerBytecode(String className, String targetOwner, String targetName, String targetDesc) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, className, null, "java/lang/Object", null);

        MethodVisitor mvInit = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mvInit.visitCode();
        mvInit.visitVarInsn(Opcodes.ALOAD, 0);
        mvInit.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mvInit.visitInsn(Opcodes.RETURN);
        mvInit.visitMaxs(1, 1);
        mvInit.visitEnd();

        MethodVisitor mvCall = cw.visitMethod(Opcodes.ACC_PUBLIC, "invokeLoader", "(Ljava/lang/Object;)V", null, null);
        mvCall.visitCode();
        mvCall.visitVarInsn(Opcodes.ALOAD, 1);
        mvCall.visitTypeInsn(Opcodes.CHECKCAST, targetOwner);
        if (targetDesc.contains("(Ljava/lang/Object;)")) {
            mvCall.visitVarInsn(Opcodes.ALOAD, 0);
            mvCall.visitMethodInsn(Opcodes.INVOKEINTERFACE, targetOwner, targetName, targetDesc, true);
        } else if (targetDesc.contains("(L" + BukkitToVesselSymmetryAdapter.BUKKIT_EVENT_CLASS + ";)")) {
            mvCall.visitInsn(Opcodes.ACONST_NULL);
            mvCall.visitMethodInsn(Opcodes.INVOKEINTERFACE, targetOwner, targetName, targetDesc, true);
        } else {
            mvCall.visitMethodInsn(Opcodes.INVOKEINTERFACE, targetOwner, targetName, targetDesc, true);
        }
        if (targetDesc.endsWith(")Ljava/lang/Object;") || targetDesc.endsWith(")Z")) {
            mvCall.visitInsn(Opcodes.POP);
        }
        mvCall.visitInsn(Opcodes.RETURN);
        mvCall.visitMaxs(2, 2);
        mvCall.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
