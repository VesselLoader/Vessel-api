package dev.vessel.api.network;

/**
 * Serialization and deserialization codec for custom network payloads.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @param <T> payload type
 * @version 1.1.0
 * @since 1.0.0
 */
public interface VesselPacketCodec<T extends VesselPacketPayload> {

    /**
     * Serializes payload to raw byte array buffer.
     *
     * @param payload packet payload
     * @return encoded byte array
     */
    byte[] encode(T payload);

    /**
     * Deserializes raw byte array buffer into payload instance.
     *
     * @param data encoded byte array
     * @return decoded packet payload
     */
    T decode(byte[] data);
}
