package dev.vessel.api.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Direct opcode receptor and clean-room emulation adapter for Forge and NeoForge ModConfigSpec / NightConfig.
 *
 * <p>Enables Forge and NeoForge mods to define, load, validate, and serialize TOML configuration specs
 * seamlessly within the VESSEL standalone server without requiring NightConfig on the classpath.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-32)
 */
public final class ForgeConfigSpecSymmetryAdapter {

    private final VesselConfigurationTree tree;
    private final NightConfigToVesselParser parser;
    private final List<String> currentPathStack;
    private String pendingComment;

    /**
     * Constructs an empty Forge ModConfigSpec adapter.
     */
    public ForgeConfigSpecSymmetryAdapter() {
        this(new VesselConfigurationTree(), new NightConfigToVesselParser());
    }

    /**
     * Constructs a Forge ModConfigSpec adapter wrapping a specific tree and parser.
     *
     * @param tree   flat configuration tree
     * @param parser toml parser
     */
    public ForgeConfigSpecSymmetryAdapter(VesselConfigurationTree tree, NightConfigToVesselParser parser) {
        this.tree = Objects.requireNonNull(tree, "tree cannot be null");
        this.parser = Objects.requireNonNull(parser, "parser cannot be null");
        this.currentPathStack = new ArrayList<>();
        this.pendingComment = null;
    }

    /**
     * Pushes a subsection into the active path stack (e.g., {@code builder.push("general")}).
     *
     * @param section section name
     * @return this adapter for chaining
     */
    public ForgeConfigSpecSymmetryAdapter push(String section) {
        Objects.requireNonNull(section, "section cannot be null");
        currentPathStack.add(section);
        return this;
    }

    /**
     * Pops the topmost subsection from the active path stack.
     *
     * @return this adapter for chaining
     */
    public ForgeConfigSpecSymmetryAdapter pop() {
        if (!currentPathStack.isEmpty()) {
            currentPathStack.remove(currentPathStack.size() - 1);
        }
        return this;
    }

    /**
     * Sets a pending comment description for the subsequent config definition.
     *
     * @param comment comment description
     * @return this adapter for chaining
     */
    public ForgeConfigSpecSymmetryAdapter comment(String comment) {
        this.pendingComment = comment;
        return this;
    }

    /**
     * Defines a typed configuration value under the current path stack.
     *
     * @param name         key name
     * @param defaultValue default value
     * @param <T>          data type
     * @return bound ConfigValue holder
     */
    public <T> ForgeConfigValueHolder<T> define(String name, T defaultValue) {
        Objects.requireNonNull(name, "name cannot be null");
        List<String> fullPath = new ArrayList<>(currentPathStack);
        fullPath.add(name);

        String commentToUse = pendingComment;
        pendingComment = null;

        return new ForgeConfigValueHolder<>(fullPath, defaultValue, commentToUse, tree);
    }

    /**
     * Defines an integer configuration value with range validation bounds.
     *
     * @param name         key name
     * @param defaultValue default integer value
     * @param min          minimum allowed value
     * @param max          maximum allowed value
     * @return bound ConfigValue holder
     */
    public ForgeConfigValueHolder<Integer> defineInRange(String name, int defaultValue, int min, int max) {
        if (defaultValue < min || defaultValue > max) {
            throw new IllegalArgumentException("Default value " + defaultValue + " outside range [" + min + ", " + max + "]");
        }
        return define(name, defaultValue);
    }

    /**
     * Defines a double precision float configuration value with range validation bounds.
     *
     * @param name         key name
     * @param defaultValue default double value
     * @param min          minimum allowed value
     * @param max          maximum allowed value
     * @return bound ConfigValue holder
     */
    public ForgeConfigValueHolder<Double> defineInRange(String name, double defaultValue, double min, double max) {
        if (defaultValue < min || defaultValue > max) {
            throw new IllegalArgumentException("Default value " + defaultValue + " outside range [" + min + ", " + max + "]");
        }
        return define(name, defaultValue);
    }

    /**
     * Defines a list configuration value.
     *
     * @param name         key name
     * @param defaultList  default list of items
     * @param <T>          list element type
     * @return bound ConfigValue holder
     */
    public <T> ForgeConfigValueHolder<List<T>> defineList(String name, List<T> defaultList) {
        return define(name, defaultList != null ? defaultList : Collections.emptyList());
    }

    /**
     * Loads and parses TOML text into the configuration tree.
     *
     * @param tomlContent raw TOML string
     */
    public void loadFromToml(String tomlContent) {
        parser.parseInto(tomlContent, tree);
    }

    /**
     * Loads configuration from a File.
     *
     * @param file source file
     * @throws IOException on I/O error
     */
    public void load(File file) throws IOException {
        Objects.requireNonNull(file, "file cannot be null");
        load(file.toPath());
    }

    /**
     * Loads configuration from a Path.
     *
     * @param path source path
     * @throws IOException on I/O error
     */
    public void load(Path path) throws IOException {
        Objects.requireNonNull(path, "path cannot be null");
        String content = Files.readString(path, StandardCharsets.UTF_8);
        loadFromToml(content);
    }

    /**
     * Serializes this configuration spec to TOML text.
     *
     * @return formatted TOML string
     */
    public String saveToToml() {
        return parser.toToml(tree);
    }

    /**
     * Saves configuration to a File on disk.
     *
     * @param file destination file
     * @throws IOException on I/O error
     */
    public void save(File file) throws IOException {
        Objects.requireNonNull(file, "file cannot be null");
        save(file.toPath());
    }

    /**
     * Saves configuration to a Path on disk.
     *
     * @param path destination path
     * @throws IOException on I/O error
     */
    public void save(Path path) throws IOException {
        Objects.requireNonNull(path, "path cannot be null");
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        String toml = saveToToml();
        Files.writeString(path, toml, StandardCharsets.UTF_8);
    }

    /**
     * Resolves all configuration values.
     *
     * @return unmodifiable map of path -> value
     */
    public Map<String, Object> getValues() {
        return tree.getValues();
    }

    /**
     * Exposes the underlying flat configuration tree.
     *
     * @return flat configuration tree
     */
    public VesselConfigurationTree getUnderlyingTree() {
        return tree;
    }
}
