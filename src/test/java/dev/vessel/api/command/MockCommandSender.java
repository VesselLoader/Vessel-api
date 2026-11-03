package dev.vessel.api.command;

import java.util.HashSet;
import java.util.Set;

/**
 * Top-level mock command sender for unit testing permission evaluation and command dispatch.
 * <p>
 * Strictly adheres to Rule 17 as an independent top-level class with zero inner,
 * nested, or anonymous classes.
 * </p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class MockCommandSender {

    private final String name;
    private final boolean op;
    private final Set<String> permissions;

    /**
     * Constructs a mock sender with the given name and operator status.
     *
     * @param name sender name
     * @param op   operator flag
     */
    public MockCommandSender(String name, boolean op) {
        this.name = name;
        this.op = op;
        this.permissions = new HashSet<>();
    }

    /**
     * Checks if the sender is an operator.
     *
     * @return {@code true} if operator
     */
    public boolean isOp() {
        return op;
    }

    /**
     * Checks if the sender has the given permission node.
     *
     * @param permission permission node
     * @return {@code true} if present
     */
    public boolean hasPermission(String permission) {
        if (op) {
            return true;
        }
        if (permission == null) {
            return false;
        }
        String lower = permission.toLowerCase();
        if (permissions.contains(lower) || permissions.contains("*")) {
            return true;
        }
        int lastDot = lower.lastIndexOf('.');
        while (lastDot != -1) {
            String parentWildcard = lower.substring(0, lastDot) + ".*";
            if (permissions.contains(parentWildcard)) {
                return true;
            }
            lastDot = lower.lastIndexOf('.', lastDot - 1);
        }
        return false;
    }

    /**
     * Grants a permission node to this mock sender.
     *
     * @param permission permission node
     */
    public void addPermission(String permission) {
        this.permissions.add(permission.toLowerCase());
    }

    /**
     * Returns the sender's display name.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "MockCommandSender[" + name + ", op=" + op + "]";
    }
}
