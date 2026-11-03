package dev.vessel.api.command;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Transpiler converting Bukkit-style commands ({@code Command}, {@code PluginCommand}, {@code CommandExecutor})
 * into unified Brigadier command node syntax trees.
 * <p>
 * Bridges legacy imperative Bukkit command semantics into modern declarative AST representations
 * accepted by Vanilla, Forge, Fabric, and Paper servers.
 * </p>
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class BukkitToBrigadierTranspiler {

    /**
     * Default greedy argument node name capturing arbitrary Bukkit sub-arguments.
     */
    public static final String DEFAULT_ARGUMENTS_NODE_NAME = "args";

    /**
     * Constructs a new transpiler instance.
     */
    public BukkitToBrigadierTranspiler() {
    }

    /**
     * Transpiles a Bukkit command object into a unified Brigadier command node tree.
     *
     * @param name          canonical root command name (e.g., "spawn")
     * @param bukkitCommand underlying Bukkit Command or CommandExecutor instance
     * @return transpiled node descriptor containing metadata and optional native Brigadier node
     */
    public BrigadierTranspiledNode transpile(String name, Object bukkitCommand) {
        return transpile(name, bukkitCommand, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Transpiles a Bukkit command object with a specific ClassLoader for Brigadier resolution.
     *
     * @param name          canonical root command name
     * @param bukkitCommand underlying Bukkit Command instance
     * @param classLoader   ClassLoader to search for com.mojang.brigadier classes
     * @return transpiled node descriptor
     */
    public BrigadierTranspiledNode transpile(String name, Object bukkitCommand, ClassLoader classLoader) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(bukkitCommand, "bukkitCommand cannot be null");

        String normalizedName = name.startsWith("/") ? name.substring(1) : name;
        String description = extractStringProperty(bukkitCommand, "getDescription", "");
        String permission = extractStringProperty(bukkitCommand, "getPermission", "");
        List<String> aliases = extractAliases(bukkitCommand);

        Object nativeBrigadierNode = tryBuildNativeBrigadierNode(normalizedName, classLoader);

        return new BrigadierTranspiledNode(
                normalizedName,
                bukkitCommand,
                description,
                permission,
                aliases,
                nativeBrigadierNode
        );
    }

    private Object tryBuildNativeBrigadierNode(String commandName, ClassLoader classLoader) {
        if (classLoader == null) {
            return null;
        }
        try {
            Class<?> literalBuilderClass = Class.forName(
                    "com.mojang.brigadier.builder.LiteralArgumentBuilder",
                    false,
                    classLoader
            );
            Method literalMethod = literalBuilderClass.getMethod("literal", String.class);
            return literalMethod.invoke(null, commandName);
        } catch (Throwable ignored) {
            // Brigadier runtime classes not available on classpath; returns null cleanly
            return null;
        }
    }

    private String extractStringProperty(Object target, String methodName, String defaultValue) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            if (result instanceof String str && !str.isBlank()) {
                return str;
            }
        } catch (Throwable ignored) {
            // Getter not found or failed
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractAliases(Object target) {
        try {
            Method method = target.getClass().getMethod("getAliases");
            Object result = method.invoke(target);
            if (result instanceof List<?> list) {
                List<String> validAliases = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof String alias && !alias.isBlank()) {
                        validAliases.add(alias);
                    }
                }
                return Collections.unmodifiableList(validAliases);
            }
        } catch (Throwable ignored) {
            // Method not present
        }
        return Collections.emptyList();
    }
}
