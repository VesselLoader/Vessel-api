package dev.vessel.api.command;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable metadata and structural descriptor representing a transpiled Brigadier command node.
 * <p>
 * Strictly adheres to Rule 17 as an independent top-level class with zero inner,
 * nested, or anonymous classes.
 * </p>
 *
 * @param commandName           canonical root command name (e.g., "vessel")
 * @param bukkitCommand         underlying Bukkit Command or CommandExecutor instance
 * @param description           command description or help text
 * @param permission            required permission node, or empty
 * @param aliases               unmodifiable list of registered alias strings
 * @param brigadierNodeInstance native com.mojang.brigadier.tree.CommandNode or LiteralArgumentBuilder if available
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record BrigadierTranspiledNode(
        String commandName,
        Object bukkitCommand,
        String description,
        String permission,
        List<String> aliases,
        Object brigadierNodeInstance
) {

    /**
     * Compact constructor ensuring non-null immutable lists and strings.
     */
    public BrigadierTranspiledNode {
        Objects.requireNonNull(commandName, "commandName cannot be null");
        Objects.requireNonNull(bukkitCommand, "bukkitCommand cannot be null");
        description = (description == null) ? "" : description;
        permission = (permission == null) ? "" : permission;
        aliases = (aliases == null) ? Collections.emptyList() : List.copyOf(aliases);
    }

    /**
     * Factory constructor for transpiled nodes without native Brigadier runtime objects.
     *
     * @param commandName   root command name
     * @param bukkitCommand source command object
     * @param description   description
     * @param permission    permission node
     * @param aliases       aliases
     */
    public BrigadierTranspiledNode(
            String commandName,
            Object bukkitCommand,
            String description,
            String permission,
            List<String> aliases
    ) {
        this(commandName, bukkitCommand, description, permission, aliases, null);
    }

    /**
     * Checks if native Mojang Brigadier runtime objects were successfully instantiated.
     *
     * @return {@code true} if brigadierNodeInstance is present
     */
    public boolean hasNativeBrigadierNode() {
        return brigadierNodeInstance != null;
    }
}
