package dev.vessel.api.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Direct opcode receptor and clean-room emulation adapter for Bukkit and Spigot YamlConfiguration / FileConfiguration.
 *
 * <p>Enables Paper and Spigot plugins to perform standard configuration operations without requiring
 * external SnakeYAML or Bukkit jar dependencies on the classpath.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public final class BukkitYamlSymmetryAdapter {

    private final VesselConfigurationTree tree;
    private final YamlToVesselParser parser;

    /**
     * Constructs an empty Bukkit YamlConfiguration adapter.
     */
    public BukkitYamlSymmetryAdapter() {
        this(new VesselConfigurationTree(), new YamlToVesselParser());
    }

    /**
     * Constructs an adapter wrapping an existing configuration tree and parser.
     *
     * @param tree   backing configuration tree
     * @param parser yaml parser
     */
    public BukkitYamlSymmetryAdapter(VesselConfigurationTree tree, YamlToVesselParser parser) {
        this.tree = Objects.requireNonNull(tree, "tree cannot be null");
        this.parser = Objects.requireNonNull(parser, "parser cannot be null");
    }

    /**
     * Static factory matching {@code YamlConfiguration.loadConfiguration(File)}.
     *
     * @param file source YAML file
     * @return loaded configuration adapter instance
     */
    public static BukkitYamlSymmetryAdapter loadConfiguration(File file) {
        Objects.requireNonNull(file, "file cannot be null");
        BukkitYamlSymmetryAdapter config = new BukkitYamlSymmetryAdapter();
        if (file.exists()) {
            try {
                config.load(file);
            } catch (IOException e) {
                // Bukkit swallows or logs load exceptions
            }
        }
        return config;
    }

    /**
     * Loads configuration data from a YAML string.
     *
     * @param yamlContent raw YAML string
     */
    public void loadFromString(String yamlContent) {
        tree.clear();
        parser.parseInto(yamlContent, tree);
    }

    /**
     * Serializes this configuration back to formatted YAML text.
     *
     * @return YAML string
     */
    public String saveToString() {
        return parser.toYaml(tree);
    }

    /**
     * Loads configuration from a File on disk.
     *
     * @param file source file
     * @throws IOException on I/O failure
     */
    public void load(File file) throws IOException {
        Objects.requireNonNull(file, "file cannot be null");
        load(file.toPath());
    }

    /**
     * Loads configuration from a Path.
     *
     * @param path source path
     * @throws IOException on I/O failure
     */
    public void load(Path path) throws IOException {
        Objects.requireNonNull(path, "path cannot be null");
        String content = Files.readString(path, StandardCharsets.UTF_8);
        loadFromString(content);
    }

    /**
     * Saves configuration to a File on disk.
     *
     * @param file destination file
     * @throws IOException on I/O failure
     */
    public void save(File file) throws IOException {
        Objects.requireNonNull(file, "file cannot be null");
        save(file.toPath());
    }

    /**
     * Saves configuration to a Path on disk.
     *
     * @param path destination path
     * @throws IOException on I/O failure
     */
    public void save(Path path) throws IOException {
        Objects.requireNonNull(path, "path cannot be null");
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        String yaml = saveToString();
        Files.writeString(path, yaml, StandardCharsets.UTF_8);
    }

    /**
     * Resolves a string value at path.
     *
     * @param path canonical path
     * @return string value or null
     */
    public String getString(String path) {
        return tree.getString(path);
    }

    /**
     * Resolves a string value at path with default fallback.
     *
     * @param path canonical path
     * @param def  default fallback
     * @return string value or def
     */
    public String getString(String path, String def) {
        return tree.getString(path, def);
    }

    /**
     * Resolves an integer value at path with default fallback 0.
     *
     * @param path canonical path
     * @return int value or 0
     */
    public int getInt(String path) {
        return tree.getInt(path);
    }

    /**
     * Resolves an integer value at path with a specified default fallback.
     *
     * @param path canonical path
     * @param def  default fallback
     * @return int value or def
     */
    public int getInt(String path, int def) {
        return tree.getInt(path, def);
    }

    /**
     * Resolves a boolean value at path with default fallback false.
     *
     * @param path canonical path
     * @return boolean value or false
     */
    public boolean getBoolean(String path) {
        return tree.getBoolean(path);
    }

    /**
     * Resolves a boolean value at path with a specified default fallback.
     *
     * @param path canonical path
     * @param def  default fallback
     * @return boolean value or def
     */
    public boolean getBoolean(String path, boolean def) {
        return tree.getBoolean(path, def);
    }

    /**
     * Resolves a double precision float at path with default fallback 0.0.
     *
     * @param path canonical path
     * @return double value or 0.0
     */
    public double getDouble(String path) {
        return tree.getDouble(path);
    }

    /**
     * Resolves a double precision float at path with a specified default fallback.
     *
     * @param path canonical path
     * @param def  default fallback
     * @return double value or def
     */
    public double getDouble(String path, double def) {
        return tree.getDouble(path, def);
    }

    /**
     * Resolves a long integer value at path with default fallback 0L.
     *
     * @param path canonical path
     * @return long value or 0L
     */
    public long getLong(String path) {
        return tree.getLong(path);
    }

    /**
     * Resolves a long integer value at path with a specified default fallback.
     *
     * @param path canonical path
     * @param def  default fallback
     * @return long value or def
     */
    public long getLong(String path, long def) {
        return tree.getLong(path, def);
    }

    /**
     * Resolves a string list value at path.
     *
     * @param path canonical path
     * @return List of strings
     */
    public List<String> getStringList(String path) {
        return tree.getStringList(path);
    }

    /**
     * Resolves a generic list value at path.
     *
     * @param path canonical path
     * @return List of objects
     */
    public List<?> getList(String path) {
        return tree.getList(path);
    }

    /**
     * Resolves raw object value at path.
     *
     * @param path canonical path
     * @return raw object or null
     */
    public Object get(String path) {
        return tree.get(path);
    }

    /**
     * Resolves raw object value at path with default fallback.
     *
     * @param path canonical path
     * @param def  default fallback
     * @return raw object or def
     */
    public Object get(String path, Object def) {
        return tree.get(path, def);
    }

    /**
     * Associates a value with a canonical path.
     *
     * @param path  canonical path
     * @param value value to set
     */
    public void set(String path, Object value) {
        tree.set(path, value);
    }

    /**
     * Checks if a path is defined in this configuration.
     *
     * @param path canonical path
     * @return true if path exists
     */
    public boolean contains(String path) {
        return tree.contains(path);
    }

    /**
     * Checks if a path is set.
     *
     * @param path canonical path
     * @return true if path is set
     */
    public boolean isSet(String path) {
        return tree.contains(path);
    }

    /**
     * Resolves keys in this configuration.
     *
     * @param deep if true, returns all dot paths; if false, returns top level
     * @return set of keys
     */
    public Set<String> getKeys(boolean deep) {
        return tree.getKeys(deep);
    }

    /**
     * Resolves all key-value mappings.
     *
     * @return map of path -> value
     */
    public Map<String, Object> getValues() {
        return tree.getValues();
    }

    /**
     * Exposes the underlying flat configuration tree.
     *
     * @return flat configuration tree instance
     */
    public VesselConfigurationTree getUnderlyingTree() {
        return tree;
    }
}
