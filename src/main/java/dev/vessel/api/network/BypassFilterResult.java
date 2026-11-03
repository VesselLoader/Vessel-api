package dev.vessel.api.network;

/**
 * Action determined by the custom payload bypass filter for an outgoing packet.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner or nested classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public enum BypassFilterResult {

    /**
     * Payload is fully compatible with the client; forward across the pipeline.
     */
    FORWARD("FORWARD", true, false),

    /**
     * Incompatible mod payload is silently absorbed/dropped without kicking the client.
     */
    ABSORB_SILENT("ABSORB_SILENT", false, false),

    /**
     * Payload contains 3D/animation data that can be transpiled into Bedrock/Vanilla display packets.
     */
    TRANSPILE_BEDROCK("TRANSPILE_BEDROCK", false, true);

    private final String actionName;
    private final boolean forwardToClient;
    private final boolean requiresTranspilation;

    BypassFilterResult(String actionName, boolean forwardToClient, boolean requiresTranspilation) {
        this.actionName = actionName;
        this.forwardToClient = forwardToClient;
        this.requiresTranspilation = requiresTranspilation;
    }

    /**
     * Action string name.
     *
     * @return action name
     */
    public String actionName() {
        return actionName;
    }

    /**
     * Whether the payload should be forwarded unmodified.
     *
     * @return true if forward
     */
    public boolean shouldForward() {
        return forwardToClient;
    }

    /**
     * Whether the payload requires conversion/transpilation.
     *
     * @return true if transpile
     */
    public boolean requiresTranspilation() {
        return requiresTranspilation;
    }
}
