package dev.vessel.api.command;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Direct bytecode symmetry adapter and opcode receptor for Bukkit Command interfaces.
 * <p>
 * Transmutes invocations of {@code PluginCommand.setExecutor()}, {@code PluginCommand.setTabCompleter()},
 * {@code CommandExecutor.onCommand()}, and {@code TabCompleter.onTabComplete()} directly into
 * native {@link VesselCommandHub} registrations and dispatch paths.
 * </p>
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class BukkitCommandSymmetryAdapter {

    private static final Map<Object, Object> EXECUTOR_REGISTRY = new ConcurrentHashMap<>();
    private static final Map<Object, Object> COMPLETER_REGISTRY = new ConcurrentHashMap<>();

    private BukkitCommandSymmetryAdapter() {
        // Utility opcode receptor
    }

    /**
     * Opcode receptor for {@code PluginCommand.setExecutor(CommandExecutor executor)}.
     *
     * @param pluginCommand target Bukkit PluginCommand instance
     * @param executor      executor instance implementing onCommand
     */
    public static void setExecutor(Object pluginCommand, Object executor) {
        if (pluginCommand == null) {
            return;
        }
        if (executor != null) {
            EXECUTOR_REGISTRY.put(pluginCommand, executor);
            String commandName = extractCommandName(pluginCommand);
            if (commandName != null && !commandName.isBlank()) {
                VesselCommandHub.getInstance().registerCommand(commandName, executor);
            }
        } else {
            EXECUTOR_REGISTRY.remove(pluginCommand);
        }
    }

    /**
     * Opcode receptor for {@code PluginCommand.setTabCompleter(TabCompleter completer)}.
     *
     * @param pluginCommand target Bukkit PluginCommand instance
     * @param completer     completer instance implementing onTabComplete
     */
    public static void setTabCompleter(Object pluginCommand, Object completer) {
        if (pluginCommand == null) {
            return;
        }
        if (completer != null) {
            COMPLETER_REGISTRY.put(pluginCommand, completer);
        } else {
            COMPLETER_REGISTRY.remove(pluginCommand);
        }
    }

    /**
     * Opcode receptor for {@code CommandExecutor.onCommand(CommandSender, Command, String, String[])}.
     *
     * @param executor target executor instance
     * @param sender   command sender instance
     * @param command  Bukkit Command instance
     * @param label    command label used
     * @param args     sub-arguments passed
     * @return {@code true} if valid command syntax; {@code false} otherwise
     */
    public static boolean onCommand(Object executor, Object sender, Object command, String label, String[] args) {
        Objects.requireNonNull(executor, "executor cannot be null");
        try {
            Method method = executor.getClass().getMethod(
                    "onCommand",
                    sender != null ? sender.getClass() : Object.class,
                    command != null ? command.getClass() : Object.class,
                    String.class,
                    String[].class
            );
            Object result = method.invoke(executor, sender, command, label, args);
            if (result instanceof Boolean b) {
                return b;
            }
            return true;
        } catch (Throwable ignored) {
            // Attempt generic dynamic lookup
            for (Method method : executor.getClass().getMethods()) {
                if ("onCommand".equals(method.getName()) && method.getParameterCount() >= 4) {
                    try {
                        Object result = method.invoke(executor, sender, command, label, args);
                        if (result instanceof Boolean b) {
                            return b;
                        }
                        return true;
                    } catch (Throwable innerIgnored) {
                        // Continue
                    }
                }
            }
        }
        return false;
    }

    /**
     * Opcode receptor for {@code TabCompleter.onTabComplete(CommandSender, Command, String, String[])}.
     *
     * @param completer target completer instance
     * @param sender    command sender
     * @param command   Bukkit Command instance
     * @param alias     alias used
     * @param args      sub-arguments entered
     * @return unmodifiable list of suggestions
     */
    @SuppressWarnings("unchecked")
    public static List<String> onTabComplete(
            Object completer,
            Object sender,
            Object command,
            String alias,
            String[] args
    ) {
        if (completer == null) {
            return Collections.emptyList();
        }
        for (Method method : completer.getClass().getMethods()) {
            if ("onTabComplete".equals(method.getName()) && method.getParameterCount() >= 4) {
                try {
                    Object result = method.invoke(completer, sender, command, alias, args);
                    if (result instanceof List<?> list) {
                        List<String> suggestions = new ArrayList<>();
                        for (Object item : list) {
                            if (item != null) {
                                suggestions.add(item.toString());
                            }
                        }
                        return Collections.unmodifiableList(suggestions);
                    }
                } catch (Throwable ignored) {
                    // Continue
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * Executes a full command line on a Bukkit executor through the unified Command Hub.
     *
     * @param executor    executor instance
     * @param sender      command sender
     * @param commandLine command line string
     * @return {@code true} if successful
     */
    public static boolean executeBukkitCommand(Object executor, Object sender, String commandLine) {
        return VesselCommandHub.getInstance().executeCommand(sender, commandLine);
    }

    /**
     * Resolves the registered executor for a given PluginCommand.
     *
     * @param pluginCommand target command instance
     * @return associated executor, or {@code null}
     */
    public static Object getExecutor(Object pluginCommand) {
        if (pluginCommand == null) {
            return null;
        }
        return EXECUTOR_REGISTRY.get(pluginCommand);
    }

    /**
     * Resolves the registered completer for a given PluginCommand.
     *
     * @param pluginCommand target command instance
     * @return associated completer, or {@code null}
     */
    public static Object getTabCompleter(Object pluginCommand) {
        if (pluginCommand == null) {
            return null;
        }
        return COMPLETER_REGISTRY.get(pluginCommand);
    }

    private static String extractCommandName(Object pluginCommand) {
        try {
            Method getName = pluginCommand.getClass().getMethod("getName");
            Object result = getName.invoke(pluginCommand);
            if (result instanceof String s && !s.isBlank()) {
                return s;
            }
        } catch (Throwable ignored) {
            // Not a named command
        }
        return null;
    }

    /**
     * Clears all executor and completer mappings.
     */
    public static void clear() {
        EXECUTOR_REGISTRY.clear();
        COMPLETER_REGISTRY.clear();
    }
}
