package dev.vessel.api.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Top-level mock Bukkit command executor and tab completer for testing.
 * <p>
 * Strictly adheres to Rule 17 as an independent top-level class with zero inner,
 * nested, or anonymous classes.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class MockBukkitCommandExecutor {

    private final String name;
    private final String description;
    private final String permission;
    private final List<String> aliases;

    private final AtomicInteger executionCount;
    private final AtomicInteger tabCompleteCount;
    private volatile String lastLabel;
    private volatile String[] lastArgs;

    /**
     * Constructs a mock command executor with standard metadata.
     *
     * @param name        command name
     * @param description description
     * @param permission  required permission node
     * @param aliases     aliases list
     */
    public MockBukkitCommandExecutor(String name, String description, String permission, String... aliases) {
        this.name = name;
        this.description = description;
        this.permission = permission;
        this.aliases = Arrays.asList(aliases);
        this.executionCount = new AtomicInteger(0);
        this.tabCompleteCount = new AtomicInteger(0);
        this.lastLabel = null;
        this.lastArgs = null;
    }

    /**
     * Bukkit onCommand execution callback.
     *
     * @param sender  sender
     * @param command command
     * @param label   label
     * @param args    arguments
     * @return {@code true}
     */
    public boolean onCommand(Object sender, Object command, String label, String[] args) {
        this.lastLabel = label;
        this.lastArgs = args;
        this.executionCount.incrementAndGet();
        return true;
    }

    /**
     * Bukkit onTabComplete suggestions callback.
     *
     * @param sender  sender
     * @param command command
     * @param alias   alias
     * @param args    arguments
     * @return list of completions
     */
    public List<String> onTabComplete(Object sender, Object command, String alias, String[] args) {
        this.tabCompleteCount.incrementAndGet();
        List<String> suggestions = new ArrayList<>();
        suggestions.add("sub1");
        suggestions.add("sub2");
        suggestions.add("reload");
        return suggestions;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPermission() {
        return permission;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public int getExecutionCount() {
        return executionCount.get();
    }

    public int getTabCompleteCount() {
        return tabCompleteCount.get();
    }

    public String getLastLabel() {
        return lastLabel;
    }

    public String[] getLastArgs() {
        return lastArgs;
    }
}
