package dev.vessel.api.classification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Pre-flight static ASM bytecode scanner for analyzing class imports,
 * field access, and method invocations under Chapter 2 Mod Taxonomy.
 */
public final class PreflightBytecodeTaxonomyScanner {

    private static final String PAPER_PREFIX = "io.papermc.";
    private static final String PAPER_LEGACY_PREFIX = "com.destroystokyo.paper.";
    private static final String PURPUR_PREFIX = "org.purpurmc.";
    private static final String BUKKIT_PREFIX = "org.bukkit.";
    private static final String SPIGOT_PREFIX = "org.spigotmc.";
    private static final String QUILT_PREFIX = "org.quiltmc.";
    private static final String FABRIC_PREFIX = "net.fabricmc.";
    private static final String NEOFORGE_PREFIX = "net.neoforged.";
    private static final String FORGE_PREFIX = "net.minecraftforge.";

    public PreflightBytecodeTaxonomyScanner() {
    }

    /**
     * Scans raw class bytecode and populates referenced classes and invoked methods.
     *
     * @param classBytes raw JVM bytecode
     * @param referencedClassesSink collector set for referenced class names
     * @param invokedMethodsSink collector set for invoked method signatures
     */
    public void scanClassBytes(
        byte[] classBytes,
        Set<String> referencedClassesSink,
        Set<String> invokedMethodsSink
    ) {
        Objects.requireNonNull(classBytes, "classBytes cannot be null");
        Objects.requireNonNull(referencedClassesSink, "referencedClassesSink cannot be null");
        Objects.requireNonNull(invokedMethodsSink, "invokedMethodsSink cannot be null");

        ClassReader reader = new ClassReader(classBytes);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        scanClassNode(classNode, referencedClassesSink, invokedMethodsSink);
    }

    /**
     * Inspects an ASM ClassNode tree and extracts all referenced classes and invoked methods.
     *
     * @param classNode ASM class node
     * @param referencedClassesSink collector set for referenced class names
     * @param invokedMethodsSink collector set for invoked method signatures
     */
    public void scanClassNode(
        ClassNode classNode,
        Set<String> referencedClassesSink,
        Set<String> invokedMethodsSink
    ) {
        Objects.requireNonNull(classNode, "classNode cannot be null");
        Objects.requireNonNull(referencedClassesSink, "referencedClassesSink cannot be null");
        Objects.requireNonNull(invokedMethodsSink, "invokedMethodsSink cannot be null");

        if (classNode.superName != null) {
            referencedClassesSink.add(classNode.superName.replace('/', '.'));
        }

        if (classNode.interfaces != null) {
            for (String itf : classNode.interfaces) {
                if (itf != null) {
                    referencedClassesSink.add(itf.replace('/', '.'));
                }
            }
        }

        if (classNode.fields != null) {
            for (FieldNode field : classNode.fields) {
                if (field != null && field.desc != null) {
                    extractTypeDescriptors(field.desc, referencedClassesSink);
                }
            }
        }

        if (classNode.methods != null) {
            for (MethodNode method : classNode.methods) {
                if (method == null) {
                    continue;
                }
                if (method.desc != null) {
                    extractMethodDescriptors(method.desc, referencedClassesSink);
                }
                if (method.instructions != null) {
                    for (AbstractInsnNode insn : method.instructions) {
                        if (insn instanceof MethodInsnNode minsn) {
                            if (minsn.owner != null) {
                                String normalizedOwner = minsn.owner.replace('/', '.');
                                referencedClassesSink.add(normalizedOwner);
                                invokedMethodsSink.add(normalizedOwner + "." + minsn.name);
                            }
                        } else if (insn instanceof FieldInsnNode finsn) {
                            if (finsn.owner != null) {
                                referencedClassesSink.add(finsn.owner.replace('/', '.'));
                            }
                        } else if (insn instanceof TypeInsnNode tinsn) {
                            if (tinsn.desc != null) {
                                referencedClassesSink.add(tinsn.desc.replace('/', '.'));
                            }
                        } else if (insn instanceof LdcInsnNode ldc) {
                            if (ldc.cst instanceof Type typeConstant) {
                                extractTypeFromAsmType(typeConstant, referencedClassesSink);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Executes complete 3-Tier classification against collected symbols and returns report.
     *
     * @param containerId identifier of the artifact
     * @param referencedClasses collection of class references
     * @param invokedMethods collection of invoked method references
     * @param foliaSupportedInDescriptor whether descriptor declares folia support
     * @param pureValidator validator for Tier 1
     * @param mixedDetector detector for Tier 2
     * @param foliaScanner auditor for Folia gate
     * @return immutable classification report
     */
    public ModClassificationReport classifyArtifact(
        String containerId,
        Set<String> referencedClasses,
        Set<String> invokedMethods,
        boolean foliaSupportedInDescriptor,
        PureVesselComplianceValidator pureValidator,
        MixedVesselReferenceDetector mixedDetector,
        FoliaThreadSafetyAuditScanner foliaScanner
    ) {
        Objects.requireNonNull(containerId, "containerId cannot be null");
        Objects.requireNonNull(referencedClasses, "referencedClasses cannot be null");
        Objects.requireNonNull(invokedMethods, "invokedMethods cannot be null");
        Objects.requireNonNull(pureValidator, "pureValidator cannot be null");
        Objects.requireNonNull(mixedDetector, "mixedDetector cannot be null");
        Objects.requireNonNull(foliaScanner, "foliaScanner cannot be null");

        // Tier 1: Pure Vessel Check
        if (pureValidator.isCompliant(referencedClasses)) {
            return new ModClassificationReport(
                containerId,
                ModTaxonomyTier.VESSEL,
                "VESSEL",
                true,
                false,
                Set.of(),
                List.of(),
                "Tier 1: Certified Pure Vessel native mod (0% external loader contamination)"
            );
        }

        // Tier 2: Mixed-Vessel Check
        if (mixedDetector.isMixedVessel(referencedClasses)) {
            Set<String> detectedLoaders = mixedDetector.detectExternalLoaders(referencedClasses);
            List<String> violations = pureValidator.findExternalLoaderViolations(referencedClasses);
            return new ModClassificationReport(
                containerId,
                ModTaxonomyTier.MIXED_VESSEL,
                "MIXED_VESSEL",
                false,
                false,
                detectedLoaders,
                violations,
                "Tier 2: Certified Mixed-Vessel mod invoking both Vessel API and external loaders"
            );
        }

        // Tier 3: Single-Loader Classification
        Set<String> externalLoaders = mixedDetector.detectExternalLoaders(referencedClasses);
        boolean hasPaper = containsPrefix(referencedClasses, PAPER_PREFIX) || containsPrefix(referencedClasses, PAPER_LEGACY_PREFIX);
        boolean hasPurpur = containsPrefix(referencedClasses, PURPUR_PREFIX);
        boolean hasBukkit = containsPrefix(referencedClasses, BUKKIT_PREFIX) || containsPrefix(referencedClasses, SPIGOT_PREFIX);

        if (hasBukkit || hasPaper || hasPurpur) {
            if (foliaSupportedInDescriptor) {
                boolean threadSafe = foliaScanner.isThreadSafeForFolia(true, invokedMethods);
                if (threadSafe) {
                    return new ModClassificationReport(
                        containerId,
                        ModTaxonomyTier.SINGLE_LOADER,
                        "FOLIA",
                        true,
                        false,
                        externalLoaders,
                        List.of(),
                        "Tier 3: Certified Folia regional concurrency plugin passing static audit gate"
                    );
                } else {
                    List<String> unsafeInvocations = foliaScanner.findUnsafeInvocations(invokedMethods);
                    String demotedLoader = foliaScanner.resolveDemotedLoader(hasPaper);
                    return new ModClassificationReport(
                        containerId,
                        ModTaxonomyTier.SINGLE_LOADER,
                        demotedLoader,
                        false,
                        true,
                        externalLoaders,
                        unsafeInvocations,
                        "Tier 3: Demoted from Folia to " + demotedLoader + " due to unsafe synchronous Bukkit calls"
                    );
                }
            } else if (hasPurpur) {
                return new ModClassificationReport(
                    containerId,
                    ModTaxonomyTier.SINGLE_LOADER,
                    "PURPUR",
                    false,
                    false,
                    externalLoaders,
                    List.of(),
                    "Tier 3: Purpur server extension plugin"
                );
            } else if (hasPaper) {
                return new ModClassificationReport(
                    containerId,
                    ModTaxonomyTier.SINGLE_LOADER,
                    "PAPER",
                    false,
                    false,
                    externalLoaders,
                    List.of(),
                    "Tier 3: Paper server plugin"
                );
            } else {
                return new ModClassificationReport(
                    containerId,
                    ModTaxonomyTier.SINGLE_LOADER,
                    "BUKKIT",
                    false,
                    false,
                    externalLoaders,
                    List.of(),
                    "Tier 3: Legacy Bukkit/Spigot server plugin"
                );
            }
        }

        if (containsPrefix(referencedClasses, QUILT_PREFIX)) {
            return new ModClassificationReport(
                containerId,
                ModTaxonomyTier.SINGLE_LOADER,
                "QUILT",
                false,
                false,
                externalLoaders,
                List.of(),
                "Tier 3: Quilt mod relying on QSL/Quilt-Loader"
            );
        }

        if (containsPrefix(referencedClasses, FABRIC_PREFIX)) {
            return new ModClassificationReport(
                containerId,
                ModTaxonomyTier.SINGLE_LOADER,
                "FABRIC",
                false,
                false,
                externalLoaders,
                List.of(),
                "Tier 3: Fabric mod relying on Fabric-API / Fabric-Loader"
            );
        }

        if (containsPrefix(referencedClasses, NEOFORGE_PREFIX)) {
            return new ModClassificationReport(
                containerId,
                ModTaxonomyTier.SINGLE_LOADER,
                "NEOFORGE",
                false,
                false,
                externalLoaders,
                List.of(),
                "Tier 3: NeoForge mod relying on NeoForged ecosystem"
            );
        }

        if (containsPrefix(referencedClasses, FORGE_PREFIX)) {
            return new ModClassificationReport(
                containerId,
                ModTaxonomyTier.SINGLE_LOADER,
                "FORGE",
                false,
                false,
                externalLoaders,
                List.of(),
                "Tier 3: Forge mod relying on MinecraftForge ecosystem"
            );
        }

        return new ModClassificationReport(
            containerId,
            ModTaxonomyTier.SINGLE_LOADER,
            "VANILLA_STANDALONE",
            false,
            false,
            externalLoaders,
            List.of(),
            "Tier 3: Standalone artifact without recognized loader APIs"
        );
    }

    private void extractTypeDescriptors(String descriptor, Set<String> sink) {
        try {
            Type type = Type.getType(descriptor);
            extractTypeFromAsmType(type, sink);
        } catch (Exception ignored) {
            // Non-critical malformed descriptor fallback
        }
    }

    private void extractMethodDescriptors(String methodDescriptor, Set<String> sink) {
        try {
            Type methodType = Type.getMethodType(methodDescriptor);
            extractTypeFromAsmType(methodType.getReturnType(), sink);
            for (Type argumentType : methodType.getArgumentTypes()) {
                extractTypeFromAsmType(argumentType, sink);
            }
        } catch (Exception ignored) {
            // Non-critical malformed descriptor fallback
        }
    }

    private void extractTypeFromAsmType(Type type, Set<String> sink) {
        if (type == null) {
            return;
        }
        if (type.getSort() == Type.OBJECT) {
            sink.add(type.getClassName());
        } else if (type.getSort() == Type.ARRAY) {
            Type elementType = type.getElementType();
            if (elementType.getSort() == Type.OBJECT) {
                sink.add(elementType.getClassName());
            }
        }
    }

    private boolean containsPrefix(Set<String> classNames, String prefix) {
        for (String name : classNames) {
            if (name != null && name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
