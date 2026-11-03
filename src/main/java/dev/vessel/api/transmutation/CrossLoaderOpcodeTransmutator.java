package dev.vessel.api.transmutation;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * High-performance AOT bytecode opcode transmutator for cross-loader compatibility.
 * Directly transforms upstream loader method calls, field accesses, and type descriptors
 * into native Vessel Super-API contracts during pre-flight class loading.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class CrossLoaderOpcodeTransmutator {

    private static final String DELIMITER = "#";

    private final Map<String, String> typeReplacements = new ConcurrentHashMap<>();
    private final Map<String, String> methodReplacements = new ConcurrentHashMap<>();
    private final Map<String, String> fieldReplacements = new ConcurrentHashMap<>();
    private final AtomicLong transmutedOpcodeCount = new AtomicLong(0);

    /**
     * Constructs a new opcode transmutator instance.
     */
    public CrossLoaderOpcodeTransmutator() {
    }

    /**
     * Registers a type descriptor and internal class name replacement.
     *
     * @param sourceInternalName upstream internal class name (e.g. "net/minecraftforge/eventbus/api/IEventBus")
     * @param targetInternalName Vessel Super-API replacement class name
     */
    public void registerTypeReplacement(String sourceInternalName, String targetInternalName) {
        Objects.requireNonNull(sourceInternalName, "sourceInternalName cannot be null");
        Objects.requireNonNull(targetInternalName, "targetInternalName cannot be null");
        this.typeReplacements.put(sourceInternalName.replace('.', '/'), targetInternalName.replace('.', '/'));
    }

    /**
     * Registers an opcode method call replacement rule.
     *
     * @param sourceOwner  upstream class owner internal name
     * @param sourceName   upstream method name
     * @param sourceDesc   upstream method descriptor
     * @param targetOwner  Vessel class owner internal name
     * @param targetName   Vessel target method name
     * @param targetDesc   Vessel target method descriptor
     * @param targetOpcode target JVM invoke opcode (e.g. INVOKEVIRTUAL, INVOKESTATIC, INVOKEINTERFACE)
     */
    public void registerMethodReplacement(
            String sourceOwner,
            String sourceName,
            String sourceDesc,
            String targetOwner,
            String targetName,
            String targetDesc,
            int targetOpcode) {
        Objects.requireNonNull(sourceOwner, "sourceOwner cannot be null");
        Objects.requireNonNull(sourceName, "sourceName cannot be null");
        Objects.requireNonNull(sourceDesc, "sourceDesc cannot be null");
        Objects.requireNonNull(targetOwner, "targetOwner cannot be null");
        Objects.requireNonNull(targetName, "targetName cannot be null");
        Objects.requireNonNull(targetDesc, "targetDesc cannot be null");

        String key = composeMethodKey(sourceOwner.replace('.', '/'), sourceName, sourceDesc);
        String val = targetOwner.replace('.', '/') + DELIMITER + targetName + DELIMITER + targetDesc + DELIMITER + targetOpcode;
        this.methodReplacements.put(key, val);
    }

    /**
     * Registers an opcode field access replacement rule.
     *
     * @param sourceOwner  upstream class owner internal name
     * @param sourceName   upstream field name
     * @param sourceDesc   upstream field descriptor
     * @param targetOwner  Vessel class owner internal name
     * @param targetName   Vessel target field name
     * @param targetDesc   Vessel target field descriptor
     * @param targetOpcode target JVM field opcode (GETSTATIC, PUTSTATIC, GETFIELD, PUTFIELD)
     */
    public void registerFieldReplacement(
            String sourceOwner,
            String sourceName,
            String sourceDesc,
            String targetOwner,
            String targetName,
            String targetDesc,
            int targetOpcode) {
        Objects.requireNonNull(sourceOwner, "sourceOwner cannot be null");
        Objects.requireNonNull(sourceName, "sourceName cannot be null");
        Objects.requireNonNull(sourceDesc, "sourceDesc cannot be null");
        Objects.requireNonNull(targetOwner, "targetOwner cannot be null");
        Objects.requireNonNull(targetName, "targetName cannot be null");
        Objects.requireNonNull(targetDesc, "targetDesc cannot be null");

        String key = composeFieldKey(sourceOwner.replace('.', '/'), sourceName, sourceDesc);
        String val = targetOwner.replace('.', '/') + DELIMITER + targetName + DELIMITER + targetDesc + DELIMITER + targetOpcode;
        this.fieldReplacements.put(key, val);
    }

    /**
     * Checks if a type has an active replacement rule.
     *
     * @param sourceInternalName internal type name
     * @return true if registered
     */
    public boolean hasTypeReplacement(String sourceInternalName) {
        if (sourceInternalName == null) {
            return false;
        }
        return this.typeReplacements.containsKey(sourceInternalName.replace('.', '/'));
    }

    /**
     * Resolves the target replacement type, or returns the original if not registered.
     *
     * @param sourceInternalName internal type name
     * @return target type name
     */
    public String getTargetType(String sourceInternalName) {
        if (sourceInternalName == null) {
            return null;
        }
        return this.typeReplacements.getOrDefault(sourceInternalName.replace('.', '/'), sourceInternalName);
    }

    /**
     * Transmutes input class bytecode by applying all registered opcode, method, field,
     * and type transformation rules.
     *
     * @param bytecode raw input bytecode
     * @return transmuted bytecode
     */
    public byte[] transmute(byte[] bytecode) {
        if (bytecode == null || bytecode.length == 0) {
            return bytecode;
        }

        ClassReader cr = new ClassReader(bytecode);
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, 0);

        boolean mutated = false;

        // 1. Mutate super class if mapped
        if (classNode.superName != null && this.typeReplacements.containsKey(classNode.superName)) {
            classNode.superName = this.typeReplacements.get(classNode.superName);
            this.transmutedOpcodeCount.incrementAndGet();
            mutated = true;
        }

        // 2. Mutate implemented interfaces
        if (classNode.interfaces != null) {
            for (int i = 0; i < classNode.interfaces.size(); i++) {
                String itf = classNode.interfaces.get(i);
                if (this.typeReplacements.containsKey(itf)) {
                    classNode.interfaces.set(i, this.typeReplacements.get(itf));
                    this.transmutedOpcodeCount.incrementAndGet();
                    mutated = true;
                }
            }
        }

        // 3. Mutate fields
        if (classNode.fields != null) {
            for (FieldNode fn : classNode.fields) {
                String updatedDesc = transmuteDescriptor(fn.desc);
                if (!updatedDesc.equals(fn.desc)) {
                    fn.desc = updatedDesc;
                    this.transmutedOpcodeCount.incrementAndGet();
                    mutated = true;
                }
            }
        }

        // 4. Mutate methods and method instructions
        if (classNode.methods != null) {
            for (MethodNode mn : classNode.methods) {
                String updatedDesc = transmuteDescriptor(mn.desc);
                if (!updatedDesc.equals(mn.desc)) {
                    mn.desc = updatedDesc;
                    this.transmutedOpcodeCount.incrementAndGet();
                    mutated = true;
                }

                if (mn.instructions != null) {
                    for (int i = 0; i < mn.instructions.size(); i++) {
                        AbstractInsnNode insn = mn.instructions.get(i);
                        if (insn instanceof MethodInsnNode minsn) {
                            if (transmuteMethodInstruction(minsn)) {
                                mutated = true;
                            }
                        } else if (insn instanceof FieldInsnNode finsn) {
                            if (transmuteFieldInstruction(finsn)) {
                                mutated = true;
                            }
                        } else if (insn instanceof TypeInsnNode tinsn) {
                            if (transmuteTypeInstruction(tinsn)) {
                                mutated = true;
                            }
                        }
                    }
                }
            }
        }

        if (!mutated) {
            return bytecode;
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(cw);
        return cw.toByteArray();
    }

    private boolean transmuteMethodInstruction(MethodInsnNode minsn) {
        String key = composeMethodKey(minsn.owner, minsn.name, minsn.desc);
        String replacement = this.methodReplacements.get(key);
        if (replacement != null) {
            String[] parts = replacement.split(DELIMITER);
            if (parts.length >= 4) {
                minsn.owner = parts[0];
                minsn.name = parts[1];
                minsn.desc = parts[2];
                minsn.setOpcode(Integer.parseInt(parts[3]));
                this.transmutedOpcodeCount.incrementAndGet();
                return true;
            }
        }

        boolean changed = false;
        if (this.typeReplacements.containsKey(minsn.owner)) {
            minsn.owner = this.typeReplacements.get(minsn.owner);
            this.transmutedOpcodeCount.incrementAndGet();
            changed = true;
        }

        String updatedDesc = transmuteDescriptor(minsn.desc);
        if (!updatedDesc.equals(minsn.desc)) {
            minsn.desc = updatedDesc;
            this.transmutedOpcodeCount.incrementAndGet();
            changed = true;
        }

        return changed;
    }

    private boolean transmuteFieldInstruction(FieldInsnNode finsn) {
        String key = composeFieldKey(finsn.owner, finsn.name, finsn.desc);
        String replacement = this.fieldReplacements.get(key);
        if (replacement != null) {
            String[] parts = replacement.split(DELIMITER);
            if (parts.length >= 4) {
                finsn.owner = parts[0];
                finsn.name = parts[1];
                finsn.desc = parts[2];
                finsn.setOpcode(Integer.parseInt(parts[3]));
                this.transmutedOpcodeCount.incrementAndGet();
                return true;
            }
        }

        boolean changed = false;
        if (this.typeReplacements.containsKey(finsn.owner)) {
            finsn.owner = this.typeReplacements.get(finsn.owner);
            this.transmutedOpcodeCount.incrementAndGet();
            changed = true;
        }

        String updatedDesc = transmuteDescriptor(finsn.desc);
        if (!updatedDesc.equals(finsn.desc)) {
            finsn.desc = updatedDesc;
            this.transmutedOpcodeCount.incrementAndGet();
            changed = true;
        }

        return changed;
    }

    private boolean transmuteTypeInstruction(TypeInsnNode tinsn) {
        if (this.typeReplacements.containsKey(tinsn.desc)) {
            tinsn.desc = this.typeReplacements.get(tinsn.desc);
            this.transmutedOpcodeCount.incrementAndGet();
            return true;
        }
        return false;
    }

    private String transmuteDescriptor(String desc) {
        if (desc == null || this.typeReplacements.isEmpty()) {
            return desc;
        }
        String current = desc;
        for (Map.Entry<String, String> entry : this.typeReplacements.entrySet()) {
            String sourceToken = "L" + entry.getKey() + ";";
            String targetToken = "L" + entry.getValue() + ";";
            if (current.contains(sourceToken)) {
                current = current.replace(sourceToken, targetToken);
            }
        }
        return current;
    }

    private String composeMethodKey(String owner, String name, String desc) {
        return owner + DELIMITER + name + DELIMITER + desc;
    }

    private String composeFieldKey(String owner, String name, String desc) {
        return owner + DELIMITER + name + DELIMITER + desc;
    }

    /**
     * Returns total count of opcodes and symbols transmuted across all processed classes.
     *
     * @return count of mutations
     */
    public long getTransmutedOpcodeCount() {
        return this.transmutedOpcodeCount.get();
    }

    /**
     * Resets the transmuted opcode count counter.
     */
    public void resetOpcodeCount() {
        this.transmutedOpcodeCount.set(0);
    }

    /**
     * Returns total count of registered replacement rules.
     *
     * @return count of rules
     */
    public int getRegisteredRuleCount() {
        return this.typeReplacements.size() + this.methodReplacements.size() + this.fieldReplacements.size();
    }
}
