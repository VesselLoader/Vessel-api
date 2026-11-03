package dev.vessel.api.command;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sovereign Universal Command Hub and Central Execution Router.
 * <p>
 * Unifies Bukkit {@code CommandExecutor}, {@code PluginCommand}, and Vanilla/Brigadier command
 * dispatch. Coordinates permission verification via {@link CommandPermissionFilter}, generates
 * syntax node representations via {@link BukkitToBrigadierTranspiler}, and resolves zero-GC
 * prefix queries through {@link TabCompletionCache}.
 * </p>
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VesselCommandHub {

    private static final VesselCommandHub DEFAULT_INSTANCE = new VesselCommandHub();

    private final Map<String, Object> commandRegistry;
    private final Map<String, BrigadierTranspiledNode> transpiledNodes;
    private final CommandPermissionFilter permissionFilter;
    private final TabCompletionCache tabCompletionCache;
    private final BukkitToBrigadierTranspiler transpiler;

    /**
     * Returns the singleton instance of the Universal Command Hub.
     *
     * @return global command hub
     */
    public static VesselCommandHub getInstance() {
        return DEFAULT_INSTANCE;
    }

    /**
     * Constructs a new independent command hub instance with default components.
     */
    public VesselCommandHub() {
        this(new CommandPermissionFilter(), new TabCompletionCache(), new BukkitToBrigadierTranspiler());
    }

    /**
     * Constructs a command hub with custom sub-components.
     *
     * @param permissionFilter   permission filtering engine
     * @param tabCompletionCache tab completion cache
     * @param transpiler         Brigadier AST transpiler
     */
    public VesselCommandHub(
            CommandPermissionFilter permissionFilter,
            TabCompletionCache tabCompletionCache,
            BukkitToBrigadierTranspiler transpiler
    ) {
        this.commandRegistry = new ConcurrentHashMap<>();
        this.transpiledNodes = new ConcurrentHashMap<>();
        this.permissionFilter = Objects.requireNonNull(permissionFilter, "permissionFilter cannot be null");
        this.tabCompletionCache = Objects.requireNonNull(tabCompletionCache, "tabCompletionCache cannot be null");
        this.transpiler = Objects.requireNonNull(transpiler, "transpiler cannot be null");
    }

    /**
     * Registers a command under the specified name and associates it with the given executor.
     *
     * @param name     command name (with or without leading slash)
     * @param executor command executor or Bukkit command object
     */
    public void registerCommand(String name, Object executor) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(executor, "executor cannot be null");

        String normalized = normalizeCommandName(name);
        commandRegistry.put(normalized, executor);

        // Index in tab completion prefix cache
        tabCompletionCache.insert(normalized);

        // Transpile to Brigadier node descriptor
        BrigadierTranspiledNode node = transpiler.transpile(normalized, executor);
        transpiledNodes.put(normalized, node);

        // Register default permission if declared
        if (!node.permission().isBlank()) {
            permissionFilter.registerPermission(node.permission(), CommandPermissionFilter.DEFAULT_VALUE_OP);
        }

        // Index any aliases
        for (String alias : node.aliases()) {
            String normAlias = normalizeCommandName(alias);
            commandRegistry.put(normAlias, executor);
            tabCompletionCache.insert(normAlias);
            transpiledNodes.put(normAlias, node);
        }
    }

    /**
     * Unregisters a command by name.
     *
     * @param name command name
     * @return {@code true} if a command was removed
     */
    public boolean unregisterCommand(String name) {
        if (name == null) {
            return false;
        }
        String normalized = normalizeCommandName(name);
        Object removed = commandRegistry.remove(normalized);
        transpiledNodes.remove(normalized);
        tabCompletionCache.remove(normalized);
        return removed != null;
    }

    /**
     * Executes a raw command line string on behalf of the given sender.
     *
     * @param sender      command sender (e.g., Player, Console, or CommandSourceStack)
     * @param commandLine full command line string (e.g., "home lobby" or "/sethome base")
     * @return {@code true} if executed successfully; {@code false} if unauthorized or command not found
     */
    public boolean executeCommand(Object sender, String commandLine) {
        if (sender == null || commandLine == null || commandLine.isBlank()) {
            return false;
        }

        String trimmed = commandLine.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1).trim();
        }

        String[] tokens = trimmed.split("\\s+");
        if (tokens.length == 0 || tokens[0].isBlank()) {
            return false;
        }

        String rootCmd = tokens[0].toLowerCase(Locale.ROOT);
        Object executor = commandRegistry.get(rootCmd);
        if (executor == null) {
            return false;
        }

        BrigadierTranspiledNode node = transpiledNodes.get(rootCmd);
        if (node != null && !node.permission().isBlank()) {
            if (!permissionFilter.hasPermission(sender, node.permission())) {
                return false;
            }
        }

        String[] subArgs = new String[tokens.length - 1];
        System.arraycopy(tokens, 1, subArgs, 0, subArgs.length);

        return dispatchToExecutor(executor, sender, rootCmd, subArgs);
    }

    /**
     * Resolves tab completions for the given command line string.
     *
     * @param sender      command sender
     * @param commandLine current text entered by the user
     * @return unmodifiable list of matching tab completion suggestions
     */
    public List<String> getTabCompletions(Object sender, String commandLine) {
        if (sender == null || commandLine == null) {
            return Collections.emptyList();
        }

        String trimmed = commandLine.startsWith("/") ? commandLine.substring(1) : commandLine;
        String[] tokens = trimmed.split("\\s+", -1);

        // Completing the root command name
        if (tokens.length <= 1) {
            String prefix = (tokens.length == 1) ? tokens[0] : "";
            return tabCompletionCache.complete(prefix);
        }

        // Completing sub-arguments for an existing command
        String rootCmd = tokens[0].toLowerCase(Locale.ROOT);
        Object executor = commandRegistry.get(rootCmd);
        if (executor == null) {
            return Collections.emptyList();
        }

        String[] subArgs = new String[tokens.length - 1];
        System.arraycopy(tokens, 1, subArgs, 0, subArgs.length);

        // Query executor's onTabComplete if available
        List<String> executorCompletions = queryExecutorTabComplete(executor, sender, rootCmd, subArgs);
        if (executorCompletions != null) {
            return executorCompletions;
        }

        // Fallback to prefix search on last token
        String lastToken = tokens[tokens.length - 1];
        return tabCompletionCache.complete(lastToken);
    }

    private boolean dispatchToExecutor(Object executor, Object sender, String label, String[] args) {
        // Attempt 1: Standard Bukkit onCommand(CommandSender, Command, String, String[])
        try {
            Method onCommand = executor.getClass().getMethod(
                    "onCommand",
                    sender.getClass(),
                    Object.class,
                    String.class,
                    String[].class
            );
            Object result = onCommand.invoke(executor, sender, null, label, args);
            if (result instanceof Boolean b) {
                return b;
            }
            return true;
        } catch (Throwable ignored) {
            // Fall through
        }

        // Attempt 2: Generic reflection onCommand
        for (Method method : executor.getClass().getMethods()) {
            if ("onCommand".equals(method.getName()) && method.getParameterCount() >= 4) {
                try {
                    Object result = method.invoke(executor, sender, null, label, args);
                    if (result instanceof Boolean b) {
                        return b;
                    }
                    return true;
                } catch (Throwable ignored) {
                    // Try next
                }
            }
        }

        // Attempt 3: execute(sender, args)
        try {
            Method executeMethod = executor.getClass().getMethod("execute", Object.class, String[].class);
            Object result = executeMethod.invoke(executor, sender, args);
            if (result instanceof Boolean b) {
                return b;
            }
            return true;
        } catch (Throwable ignored) {
            // Try next
        }

        // Attempt 4: Runnable executor
        if (executor instanceof Runnable runnable) {
            runnable.run();
            return true;
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private List<String> queryExecutorTabComplete(Object executor, Object sender, String alias, String[] args) {
        for (Method method : executor.getClass().getMethods()) {
            if ("onTabComplete".equals(method.getName()) && method.getParameterCount() >= 4) {
                try {
                    Object result = method.invoke(executor, sender, null, alias, args);
                    if (result instanceof List<?> list) {
                        List<String> completions = new ArrayList<>();
                        for (Object item : list) {
                            if (item != null) {
                                completions.add(item.toString());
                            }
                        }
                        return Collections.unmodifiableList(completions);
                    }
                } catch (Throwable ignored) {
                    // Ignore
                }
            }
        }
        return null;
    }

    private String normalizeCommandName(String name) {
        String trimmed = name.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    /**
     * Checks if a command is registered.
     *
     * @param name command name
     * @return {@code true} if present
     */
    public boolean hasCommand(String name) {
        if (name == null) {
            return false;
        }
        return commandRegistry.containsKey(normalizeCommandName(name));
    }

    /**
     * Returns the registered executor for the given command name.
     *
     * @param name command name
     * @return executor instance, or {@code null}
     */
    public Object getCommand(String name) {
        if (name == null) {
            return null;
        }
        return commandRegistry.get(normalizeCommandName(name));
    }

    /**
     * Returns the transpiled node descriptor for the given command name.
     *
     * @param name command name
     * @return transpiled node, or {@code null}
     */
    public BrigadierTranspiledNode getTranspiledNode(String name) {
        if (name == null) {
            return null;
        }
        return transpiledNodes.get(normalizeCommandName(name));
    }

    /**
     * Returns the underlying permission filter.
     *
     * @return permission filter
     */
    public CommandPermissionFilter getPermissionFilter() {
        return permissionFilter;
    }

    /**
     * Returns the underlying tab completion cache.
     *
     * @return tab completion cache
     */
    public TabCompletionCache getTabCompletionCache() {
        return tabCompletionCache;
    }

    /**
     * Returns the underlying Brigadier AST transpiler.
     *
     * @return transpiler
     */
    public BukkitToBrigadierTranspiler getTranspiler() {
        return transpiler;
    }

    /**
     * Returns total number of registered command names and aliases.
     *
     * @return registered commands count
     */
    public int size() {
        return commandRegistry.size();
    }

    /**
     * Clears all registered commands and completions.
     */
    public void clear() {
        commandRegistry.clear();
        transpiledNodes.clear();
        tabCompletionCache.clear();
        permissionFilter.clear();
    }
}
