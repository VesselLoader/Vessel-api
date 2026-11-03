package dev.vessel.api.transmutation;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Ahead-Of-Time (AOT) bytecode transmutation pipeline ($A \to A'$) for the Vessel
 * Semantic Transmutation Matrix (VSTM) specified under VUSA Chapter 1, 3, and 6.
 *
 * <p>Executes during pre-flight classloading to transform upstream loader calls
 * directly into Vessel Super-API calls with zero runtime reflection and zero proxy allocations.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class VstmTransmutationPipeline {

    public static final String NATIVE_VESSEL_PACKAGE_PREFIX = "dev/vessel/";
    public static final String NATIVE_VESSEL_PACKAGE_DOT = "dev.vessel.";

    private final CrossLoaderOpcodeTransmutator transmutator;
    private final ForgeToVesselSymmetryAdapter forgeAdapter;
    private final NeoForgeToVesselSymmetryAdapter neoForgeAdapter;
    private final FabricToVesselSymmetryAdapter fabricAdapter;
    private final BukkitToVesselSymmetryAdapter bukkitAdapter;
    private final QuiltToVesselSymmetryAdapter quiltAdapter;

    private final AtomicLong totalTransmutedClasses = new AtomicLong(0);
    private final AtomicLong totalNanosecondsElapsed = new AtomicLong(0);

    /**
     * Constructs a default VSTM transmutation pipeline configured with all cross-loader
     * symmetry adapters (Forge, NeoForge, Fabric, Bukkit, Quilt).
     */
    public VstmTransmutationPipeline() {
        this.transmutator = new CrossLoaderOpcodeTransmutator();
        this.forgeAdapter = new ForgeToVesselSymmetryAdapter();
        this.neoForgeAdapter = new NeoForgeToVesselSymmetryAdapter();
        this.fabricAdapter = new FabricToVesselSymmetryAdapter();
        this.bukkitAdapter = new BukkitToVesselSymmetryAdapter();
        this.quiltAdapter = new QuiltToVesselSymmetryAdapter();

        // Register default cross-loader symmetry tables
        this.forgeAdapter.configureTransmutations(this.transmutator);
        this.neoForgeAdapter.configureTransmutations(this.transmutator);
        this.fabricAdapter.configureTransmutations(this.transmutator);
        this.bukkitAdapter.configureTransmutations(this.transmutator);
        this.quiltAdapter.configureTransmutations(this.transmutator);
    }

    /**
     * Executes the AOT bytecode transmutation pipeline on the specified class bytecode.
     *
     * @param className        fully qualified class name (dot or slash notation)
     * @param originalBytecode raw class bytecode
     * @return transmuted bytecode, or original bytecode if no mutations were applied
     */
    public byte[] transmuteClass(String className, byte[] originalBytecode) {
        if (originalBytecode == null || originalBytecode.length == 0) {
            return originalBytecode;
        }

        Objects.requireNonNull(className, "className cannot be null");

        if (shouldBypass(className)) {
            return originalBytecode;
        }

        long start = System.nanoTime();
        long previousOpcodeCount = this.transmutator.getTransmutedOpcodeCount();

        byte[] transmuted = this.transmutator.transmute(originalBytecode);

        long elapsed = System.nanoTime() - start;
        this.totalNanosecondsElapsed.addAndGet(elapsed);

        if (this.transmutator.getTransmutedOpcodeCount() > previousOpcodeCount) {
            this.totalTransmutedClasses.incrementAndGet();
        }

        return transmuted;
    }

    /**
     * Determines whether a class belongs to native Vessel internals and should bypass transmutation.
     *
     * @param className class name to evaluate
     * @return true if class should be bypassed
     */
    public boolean shouldBypass(String className) {
        if (className == null) {
            return false;
        }
        if (className.startsWith(NATIVE_VESSEL_PACKAGE_DOT)) {
            return true;
        }
        String normalized = className.replace('.', '/');
        return normalized.startsWith(NATIVE_VESSEL_PACKAGE_PREFIX);
    }

    /**
     * Returns the underlying opcode transmutator.
     *
     * @return active opcode transmutator
     */
    public CrossLoaderOpcodeTransmutator getTransmutator() {
        return this.transmutator;
    }

    /**
     * Returns the Forge-to-Vessel symmetry adapter.
     *
     * @return Forge symmetry adapter
     */
    public ForgeToVesselSymmetryAdapter getForgeAdapter() {
        return this.forgeAdapter;
    }

    /**
     * Returns the NeoForge-to-Vessel symmetry adapter.
     *
     * @return NeoForge symmetry adapter
     */
    public NeoForgeToVesselSymmetryAdapter getNeoForgeAdapter() {
        return this.neoForgeAdapter;
    }

    /**
     * Returns the Fabric-to-Vessel symmetry adapter.
     *
     * @return Fabric symmetry adapter
     */
    public FabricToVesselSymmetryAdapter getFabricAdapter() {
        return this.fabricAdapter;
    }

    /**
     * Returns the Bukkit-to-Vessel symmetry adapter.
     *
     * @return Bukkit symmetry adapter
     */
    public BukkitToVesselSymmetryAdapter getBukkitAdapter() {
        return this.bukkitAdapter;
    }

    /**
     * Returns the Quilt-to-Vessel symmetry adapter.
     *
     * @return Quilt symmetry adapter
     */
    public QuiltToVesselSymmetryAdapter getQuiltAdapter() {
        return this.quiltAdapter;
    }

    /**
     * Returns the total count of classes that were transmuted.
     *
     * @return transmuted class count
     */
    public long getTotalClassesTransmuted() {
        return this.totalTransmutedClasses.get();
    }

    /**
     * Returns total nanoseconds spent executing bytecode transmutation.
     *
     * @return total nanoseconds
     */
    public long getTotalNanosecondsElapsed() {
        return this.totalNanosecondsElapsed.get();
    }

    /**
     * Returns the average transmutation duration in microseconds per processed class.
     *
     * @return average microseconds
     */
    public double getAverageTransmutationTimeMicros() {
        long count = this.totalTransmutedClasses.get();
        if (count == 0) {
            return 0.0;
        }
        return (double) this.totalNanosecondsElapsed.get() / (count * 1_000.0);
    }

    /**
     * Resets all performance and metric counters.
     */
    public void resetMetrics() {
        this.totalTransmutedClasses.set(0);
        this.totalNanosecondsElapsed.set(0);
        this.transmutator.resetOpcodeCount();
    }
}
