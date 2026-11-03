package dev.vessel.api.network;

/**
 * Classification of connected Minecraft client types for protocol dispatch and payload filtering.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner or nested classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public enum NetworkClientType {

    /**
     * Standard official Java Edition client without custom mod loaders.
     */
    VANILLA("VANILLA", false, false, true),

    /**
     * Bedrock Edition client connected via Geyser / Floodgate bridge.
     */
    BEDROCK("BEDROCK", false, true, false),

    /**
     * Java Edition client running Fabric Loader.
     */
    MODDED_FABRIC("MODDED_FABRIC", true, false, false),

    /**
     * Java Edition client running Minecraft Forge.
     */
    MODDED_FORGE("MODDED_FORGE", true, false, false),

    /**
     * Java Edition client running NeoForge.
     */
    MODDED_NEOFORGE("MODDED_NEOFORGE", true, false, false),

    /**
     * Unclassified or indeterminate client type.
     */
    UNKNOWN("UNKNOWN", false, false, false);

    private final String typeName;
    private final boolean modded;
    private final boolean bedrock;
    private final boolean vanilla;

    NetworkClientType(String typeName, boolean modded, boolean bedrock, boolean vanilla) {
        this.typeName = typeName;
        this.modded = modded;
        this.bedrock = bedrock;
        this.vanilla = vanilla;
    }

    /**
     * Resolves the canonical client type name.
     *
     * @return client type name
     */
    public String typeName() {
        return typeName;
    }

    /**
     * Checks if the client has a mod loader installed.
     *
     * @return true if Fabric, Forge, or NeoForge
     */
    public boolean isModded() {
        return modded;
    }

    /**
     * Checks if the client is Bedrock Edition.
     *
     * @return true if Bedrock
     */
    public boolean isBedrock() {
        return bedrock;
    }

    /**
     * Checks if the client is standard Vanilla Java Edition.
     *
     * @return true if Vanilla
     */
    public boolean isVanilla() {
        return vanilla;
    }
}
