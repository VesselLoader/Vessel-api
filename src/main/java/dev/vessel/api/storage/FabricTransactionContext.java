package dev.vessel.api.storage;

import java.util.ArrayList;
import java.util.List;

/**
 * Clean-room transaction context implementation mirroring Fabric Transfer API v1
 * ({@code net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext}).
 *
 * <p>Provides zero-allocation nested transaction mechanics, rollback triggers, and
 * commit hooks for opcode-level symmetry adapters.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class FabricTransactionContext {

    public static final int STATE_OPEN = 0;
    public static final int STATE_COMMITTED = 1;
    public static final int STATE_ABORTED = 2;

    private final FabricTransactionContext parent;
    private final int nestingDepth;
    private final List<Runnable> rollbackActions = new ArrayList<>();
    private final List<Runnable> commitActions = new ArrayList<>();
    private int state = STATE_OPEN;

    /**
     * Constructs a top-level root transaction.
     */
    public FabricTransactionContext() {
        this(null);
    }

    /**
     * Constructs a nested transaction with an optional parent.
     *
     * @param parent parent transaction context or null if root
     */
    public FabricTransactionContext(FabricTransactionContext parent) {
        this.parent = parent;
        this.nestingDepth = parent == null ? 0 : parent.nestingDepth + 1;
    }

    public FabricTransactionContext getParent() {
        return parent;
    }

    public int getNestingDepth() {
        return nestingDepth;
    }

    public boolean isOpen() {
        return state == STATE_OPEN;
    }

    public boolean isCommitted() {
        return state == STATE_COMMITTED;
    }

    public boolean isAborted() {
        return state == STATE_ABORTED;
    }

    /**
     * Opens a new nested transaction child under this context.
     *
     * @return new child transaction context
     */
    public FabricTransactionContext openNested() {
        if (!isOpen()) {
            throw new IllegalStateException("Cannot open nested transaction on non-open parent: state=" + state);
        }
        return new FabricTransactionContext(this);
    }

    /**
     * Registers a callback executed only if this transaction is aborted.
     *
     * @param action rollback handler
     */
    public void addRollbackAction(Runnable action) {
        if (action != null) {
            rollbackActions.add(action);
        }
    }

    /**
     * Registers a callback executed only if this transaction is committed.
     *
     * @param action commit handler
     */
    public void addCommitAction(Runnable action) {
        if (action != null) {
            commitActions.add(action);
        }
    }

    /**
     * Commits this transaction and executes all registered commit actions.
     */
    public void commit() {
        if (state != STATE_OPEN) {
            throw new IllegalStateException("Transaction is already closed: state=" + state);
        }
        state = STATE_COMMITTED;
        for (Runnable action : commitActions) {
            action.run();
        }
    }

    /**
     * Aborts this transaction and executes all registered rollback actions.
     */
    public void abort() {
        if (state != STATE_OPEN) {
            return;
        }
        state = STATE_ABORTED;
        for (Runnable action : rollbackActions) {
            action.run();
        }
    }

    /**
     * AutoCloseable compatibility: aborts the transaction if left open.
     */
    public void close() {
        if (isOpen()) {
            abort();
        }
    }
}
