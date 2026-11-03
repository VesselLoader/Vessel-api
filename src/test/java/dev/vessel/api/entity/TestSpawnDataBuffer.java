package dev.vessel.api.entity;

/**
 * Mock buffer fixture simulating FriendlyByteBuf / Netty ByteBuf for spawn data tests.
 *
 * @version 1.0.0
 */
public final class TestSpawnDataBuffer {

    private String storedPayload;

    public TestSpawnDataBuffer() {
        this.storedPayload = "";
    }

    public void writeString(String data) {
        this.storedPayload = data;
    }

    public String readString() {
        return storedPayload;
    }

    public boolean hasContent() {
        return storedPayload != null && !storedPayload.isEmpty();
    }
}
