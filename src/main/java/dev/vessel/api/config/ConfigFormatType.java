package dev.vessel.api.config;

/**
 * Supported configuration serialization formats within the VESSEL Universal Configuration Substrate.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner or nested classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public enum ConfigFormatType {

    /**
     * Standard YAML configuration used predominantly by Bukkit/Spigot/Paper plugins.
     */
    YAML("YAML", ".yml"),

    /**
     * TOML configuration table used by Minecraft Forge and NeoForge mods via NightConfig.
     */
    TOML("TOML", ".toml"),

    /**
     * JSON formatted configuration.
     */
    JSON("JSON", ".json"),

    /**
     * High-speed binary sync frame streamed across Netty during connection handshake.
     */
    BINARY_SYNC("BINARY_SYNC", ".vcfg");

    private final String formatName;
    private final String fileExtension;

    ConfigFormatType(String formatName, String fileExtension) {
        this.formatName = formatName;
        this.fileExtension = fileExtension;
    }

    /**
     * Resolves the canonical format name.
     *
     * @return format name string
     */
    public String formatName() {
        return formatName;
    }

    /**
     * Resolves the standard file extension.
     *
     * @return file extension including leading dot
     */
    public String fileExtension() {
        return fileExtension;
    }
}
