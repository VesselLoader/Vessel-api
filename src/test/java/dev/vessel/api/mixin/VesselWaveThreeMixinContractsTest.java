// ==============================================================================
// VESSEL-API — VesselWaveThreeMixinContractsTest.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of JUnit 5 test suite verifying Wave 3 VUSA Mixin
//          contracts VesselMixinTargetDescriptor and VesselInjectionPointDescriptor.
// ==============================================================================

package dev.vessel.api.mixin;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for Wave 3 VUSA Mixin contracts
 * ({@link VesselMixinTargetDescriptor} and {@link VesselInjectionPointDescriptor}).
 *
 * @version 1.0.0
 */
public final class VesselWaveThreeMixinContractsTest {

    @Test
    void testMixinTargetDescriptorPropertiesAndDefensiveCopy() {
        List<String> mutableTargets = new ArrayList<>();
        mutableTargets.add("net.minecraft.server.MinecraftServer");
        mutableTargets.add("net.minecraft.server.level.ServerLevel");

        VesselMixinTargetDescriptor descriptor = new VesselMixinTargetDescriptor(
                "com.example.mod.mixin.MinecraftServerMixin",
                "examplemod",
                mutableTargets,
                1000,
                true);

        mutableTargets.add("net.minecraft.world.entity.Entity");

        assertEquals("com.example.mod.mixin.MinecraftServerMixin", descriptor.getMixinClassName());
        assertEquals("examplemod", descriptor.getOwnerModId());
        assertEquals(2, descriptor.getTargetClassNames().size());
        assertEquals(1000, descriptor.getPriority());
        assertTrue(descriptor.isRemap());
        assertThrows(
                UnsupportedOperationException.class,
                () -> descriptor.getTargetClassNames().add("net.minecraft.world.level.Level"));

        VesselMixinTargetDescriptor identical = new VesselMixinTargetDescriptor(
                "com.example.mod.mixin.MinecraftServerMixin",
                "examplemod",
                List.of("net.minecraft.server.MinecraftServer", "net.minecraft.server.level.ServerLevel"),
                1000,
                true);
        VesselMixinTargetDescriptor differentPriority = new VesselMixinTargetDescriptor(
                "com.example.mod.mixin.MinecraftServerMixin",
                "examplemod",
                List.of("net.minecraft.server.MinecraftServer", "net.minecraft.server.level.ServerLevel"),
                1100,
                false);

        assertEquals(descriptor, identical);
        assertEquals(descriptor.hashCode(), identical.hashCode());
        assertNotEquals(descriptor, differentPriority);
        assertTrue(descriptor.toString().contains("MinecraftServerMixin"));
    }

    @Test
    void testMixinTargetDescriptorValidation() {
        List<String> validTargets = List.of("net.minecraft.server.MinecraftServer");

        assertThrows(
                NullPointerException.class,
                () -> new VesselMixinTargetDescriptor(null, "modid", validTargets, 1000, true));
        assertThrows(
                NullPointerException.class,
                () -> new VesselMixinTargetDescriptor("MixinClass", null, validTargets, 1000, true));
        assertThrows(
                NullPointerException.class,
                () -> new VesselMixinTargetDescriptor("MixinClass", "modid", null, 1000, true));

        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselMixinTargetDescriptor("  ", "modid", validTargets, 1000, true));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselMixinTargetDescriptor("MixinClass", "  ", validTargets, 1000, true));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselMixinTargetDescriptor("MixinClass", "modid", List.of(), 1000, true));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselMixinTargetDescriptor("MixinClass", "modid", List.of("   "), 1000, true));
    }

    @Test
    void testInjectionPointDescriptorPropertiesAndClassifications() {
        VesselInjectionPointDescriptor headInject = new VesselInjectionPointDescriptor(
                "onTickHead",
                "tickServer(Ljava/util/function/BooleanSupplier;)V",
                "HEAD",
                true);

        assertEquals("onTickHead", headInject.getHandlerMethodName());
        assertEquals("tickServer(Ljava/util/function/BooleanSupplier;)V", headInject.getTargetMethodSignature());
        assertEquals("HEAD", headInject.getAtPointValue());
        assertTrue(headInject.isCancellable());
        assertTrue(headInject.isHeadInjection());
        assertFalse(headInject.isReturnInjection());

        VesselInjectionPointDescriptor returnInject = new VesselInjectionPointDescriptor(
                "onTickReturn",
                "tickServer(Ljava/util/function/BooleanSupplier;)V",
                "RETURN",
                false);
        assertFalse(returnInject.isHeadInjection());
        assertTrue(returnInject.isReturnInjection());

        VesselInjectionPointDescriptor tailInject = new VesselInjectionPointDescriptor(
                "onTickTail",
                "tickServer(Ljava/util/function/BooleanSupplier;)V",
                "tail",
                false);
        assertFalse(tailInject.isHeadInjection());
        assertTrue(tailInject.isReturnInjection());

        VesselInjectionPointDescriptor invokeInject = new VesselInjectionPointDescriptor(
                "onTickInvoke",
                "tickServer(Ljava/util/function/BooleanSupplier;)V",
                "INVOKE",
                true);
        assertFalse(invokeInject.isHeadInjection());
        assertFalse(invokeInject.isReturnInjection());

        VesselInjectionPointDescriptor headInjectCopy = new VesselInjectionPointDescriptor(
                "onTickHead",
                "tickServer(Ljava/util/function/BooleanSupplier;)V",
                "HEAD",
                true);
        assertEquals(headInject, headInjectCopy);
        assertEquals(headInject.hashCode(), headInjectCopy.hashCode());
        assertNotEquals(headInject, returnInject);
        assertTrue(headInject.toString().contains("onTickHead"));
    }

    @Test
    void testInjectionPointDescriptorValidation() {
        assertThrows(
                NullPointerException.class,
                () -> new VesselInjectionPointDescriptor(null, "tick()V", "HEAD", false));
        assertThrows(
                NullPointerException.class,
                () -> new VesselInjectionPointDescriptor("handler", null, "HEAD", false));
        assertThrows(
                NullPointerException.class,
                () -> new VesselInjectionPointDescriptor("handler", "tick()V", null, false));

        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselInjectionPointDescriptor("  ", "tick()V", "HEAD", false));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselInjectionPointDescriptor("handler", "   ", "HEAD", false));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselInjectionPointDescriptor("handler", "tick()V", "", false));
    }
}
