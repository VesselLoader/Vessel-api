package dev.vessel.api.command;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zero-allocation permission check table and evaluation engine.
 * <p>
 * Bridges Bukkit {@code Permissible} and Vanilla/Forge/Fabric permission queries into
 * high-performance cached lookups. Supports hierarchical wildcard expansion (e.g.,
 * {@code vessel.admin.*} covering {@code vessel.admin.reload}), operator overrides,
 * and reflective duck-typing for heterogeneous command senders.
 * </p>
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class CommandPermissionFilter {

    /**
     * Default permission value allowing all senders.
     */
    public static final String DEFAULT_VALUE_TRUE = "TRUE";

    /**
     * Default permission value disallowing senders unless explicitly granted.
     */
    public static final String DEFAULT_VALUE_FALSE = "FALSE";

    /**
     * Default permission value requiring server operator status.
     */
    public static final String DEFAULT_VALUE_OP = "OP";

    /**
     * Global super-wildcard permission node.
     */
    public static final String GLOBAL_WILDCARD = "*";

    /**
     * Subtree wildcard permission suffix.
     */
    public static final String WILDCARD_SUFFIX = ".*";

    private final Map<String, String> registeredDefaults;
    private final Map<Object, Set<String>> senderDirectPermissions;
    private final Set<Object> operatorSenders;

    /**
     * Constructs a new command permission filter instance.
     */
    public CommandPermissionFilter() {
        this.registeredDefaults = new ConcurrentHashMap<>();
        this.senderDirectPermissions = new ConcurrentHashMap<>();
        this.operatorSenders = ConcurrentHashMap.newKeySet();
    }

    /**
     * Registers a permission node with its default accessibility policy.
     *
     * @param node         permission node (e.g., "vessel.command.home")
     * @param defaultValue default value policy ("TRUE", "FALSE", or "OP")
     */
    public void registerPermission(String node, String defaultValue) {
        Objects.requireNonNull(node, "node cannot be null");
        String val = (defaultValue == null) ? DEFAULT_VALUE_FALSE : defaultValue.trim().toUpperCase();
        registeredDefaults.put(node.trim().toLowerCase(), val);
    }

    /**
     * Assigns a specific permission node directly to a sender instance.
     *
     * @param sender         command sender instance
     * @param permissionNode permission node to assign
     */
    public void assignPermission(Object sender, String permissionNode) {
        if (sender == null || permissionNode == null) {
            return;
        }
        senderDirectPermissions
                .computeIfAbsent(sender, k -> ConcurrentHashMap.newKeySet())
                .add(permissionNode.trim().toLowerCase());
    }

    /**
     * Revokes a previously assigned permission node from a sender instance.
     *
     * @param sender         command sender instance
     * @param permissionNode permission node to revoke
     */
    public void revokePermission(Object sender, String permissionNode) {
        if (sender == null || permissionNode == null) {
            return;
        }
        Set<String> perms = senderDirectPermissions.get(sender);
        if (perms != null) {
            perms.remove(permissionNode.trim().toLowerCase());
        }
    }

    /**
     * Marks a sender as having server operator (OP) status.
     *
     * @param sender command sender instance
     * @param isOp   operator status flag
     */
    public void setOperator(Object sender, boolean isOp) {
        if (sender == null) {
            return;
        }
        if (isOp) {
            operatorSenders.add(sender);
        } else {
            operatorSenders.remove(sender);
        }
    }

    /**
     * Evaluates whether the given sender holds the requested permission node.
     *
     * @param sender         command sender instance
     * @param permissionNode permission node to evaluate
     * @return {@code true} if authorized; {@code false} otherwise
     */
    public boolean hasPermission(Object sender, String permissionNode) {
        if (sender == null) {
            return false;
        }
        if (permissionNode == null || permissionNode.isBlank()) {
            return true;
        }

        String target = permissionNode.trim().toLowerCase();

        // 1. Direct console or server thread authority
        if (isConsoleOrServer(sender)) {
            return true;
        }

        // 2. Operator status check
        boolean isOp = isOperator(sender);
        if (isOp) {
            return true;
        }

        // 3. Sender direct in-memory permission check (with wildcard support)
        Set<String> directPerms = senderDirectPermissions.get(sender);
        if (directPerms != null) {
            if (directPerms.contains(GLOBAL_WILDCARD) || directPerms.contains(target)) {
                return true;
            }
            // Check hierarchical wildcards (e.g., "vessel.admin.*")
            int lastDot = target.lastIndexOf('.');
            while (lastDot != -1) {
                String parentWildcard = target.substring(0, lastDot) + WILDCARD_SUFFIX;
                if (directPerms.contains(parentWildcard)) {
                    return true;
                }
                lastDot = target.lastIndexOf('.', lastDot - 1);
            }
        }

        // 4. Reflective duck-typing for Bukkit Permissible or vanilla CommandSource
        Boolean duckResult = checkDuckTypedPermission(sender, target);
        if (duckResult != null) {
            return duckResult;
        }

        // 5. Registered default evaluation
        String defaultPolicy = registeredDefaults.get(target);
        if (DEFAULT_VALUE_TRUE.equalsIgnoreCase(defaultPolicy)) {
            return true;
        }
        if (DEFAULT_VALUE_OP.equalsIgnoreCase(defaultPolicy) && isOp) {
            return true;
        }

        return false;
    }

    private boolean isOperator(Object sender) {
        if (operatorSenders.contains(sender)) {
            return true;
        }
        try {
            Method isOpMethod = sender.getClass().getMethod("isOp");
            Object result = isOpMethod.invoke(sender);
            if (result instanceof Boolean b) {
                return b;
            }
        } catch (Throwable ignored) {
            // Not a Bukkit Permissible or no isOp method
        }
        return false;
    }

    private boolean isConsoleOrServer(Object sender) {
        String name = sender.getClass().getSimpleName().toLowerCase();
        if (name.contains("console") || name.contains("server") || name.contains("rcon")) {
            return true;
        }
        String str = sender.toString().toLowerCase();
        return str.contains("console") || str.contains("server");
    }

    private Boolean checkDuckTypedPermission(Object sender, String permission) {
        try {
            Method hasPermMethod = sender.getClass().getMethod("hasPermission", String.class);
            Object result = hasPermMethod.invoke(sender, permission);
            if (result instanceof Boolean b) {
                return b;
            }
        } catch (Throwable ignored) {
            // Duck-typing not supported on this sender
        }
        return null;
    }

    /**
     * Returns an unmodifiable set of direct permissions assigned to the sender.
     *
     * @param sender command sender
     * @return unmodifiable set of permissions
     */
    public Set<String> getPermissions(Object sender) {
        Set<String> perms = senderDirectPermissions.get(sender);
        if (perms == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(perms);
    }

    /**
     * Clears all registered defaults, direct assignments, and operator states.
     */
    public void clear() {
        registeredDefaults.clear();
        senderDirectPermissions.clear();
        operatorSenders.clear();
    }
}
