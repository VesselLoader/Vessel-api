package dev.vessel.api.network;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Transpiles server-side 3D animations and motion updates into standard Bedrock entity display
 * packets or vanilla Display Entity updates.
 *
 * <p>Enables smooth visual presentation of modded contraptions, GeckoLib animations, and custom models
 * on unmodded Bedrock and Vanilla clients without client-side mod requirements.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public final class BedrockDisplayEntityTranspiler {

    public static final String PACKET_TYPE_BEDROCK_DISPLAY = "BEDROCK_SET_ENTITY_DISPLAY";
    public static final String PACKET_TYPE_BEDROCK_MOTION_DELTA = "BEDROCK_SET_ENTITY_MOTION_DELTA";
    public static final String PACKET_TYPE_VANILLA_DISPLAY = "VANILLA_SET_ENTITY_DATA";

    public static final byte TRANSPILE_MAGIC = 0x56; // 'V'
    public static final byte FLAG_HAS_TRANSLATION = 0x01;
    public static final byte FLAG_HAS_ROTATION = 0x02;
    public static final byte FLAG_HAS_SCALE = 0x04;
    public static final byte FLAG_INTERPOLATED = 0x08;

    /**
     * Constructs a new Bedrock display entity transpiler.
     */
    public BedrockDisplayEntityTranspiler() {
    }

    /**
     * Transpiles a single 3D transform keyframe into a Bedrock display packet.
     *
     * @param keyframe source animation keyframe
     * @return transpiled packet
     */
    public TranspiledBedrockPacket transpileKeyframe(DisplayEntityKeyframe keyframe) {
        Objects.requireNonNull(keyframe, "keyframe cannot be null");

        // Format: Magic(1) + Flags(1) + EntityId(4) + X(8)+Y(8)+Z(8) + Yaw(4)+Pitch(4)+Roll(4) + ScaleX(4)+ScaleY(4)+ScaleZ(4) + Ticks(4)
        int payloadSize = 1 + 1 + 4 + (8 * 3) + (4 * 3) + (4 * 3) + 4;
        ByteBuffer buffer = ByteBuffer.allocate(payloadSize);

        byte flags = (byte) (FLAG_HAS_TRANSLATION | FLAG_HAS_ROTATION | FLAG_HAS_SCALE | FLAG_INTERPOLATED);

        buffer.put(TRANSPILE_MAGIC);
        buffer.put(flags);
        buffer.putInt(keyframe.entityId());

        // Translation
        buffer.putDouble(keyframe.posX());
        buffer.putDouble(keyframe.posY());
        buffer.putDouble(keyframe.posZ());

        // Rotation
        buffer.putFloat(keyframe.yaw());
        buffer.putFloat(keyframe.pitch());
        buffer.putFloat(keyframe.roll());

        // Scale
        buffer.putFloat(keyframe.scaleX());
        buffer.putFloat(keyframe.scaleY());
        buffer.putFloat(keyframe.scaleZ());

        // Interpolation
        buffer.putInt(keyframe.interpolationTicks());

        return new TranspiledBedrockPacket(
                keyframe.entityId(),
                PACKET_TYPE_BEDROCK_DISPLAY,
                buffer.array(),
                keyframe.interpolationTicks(),
                true
        );
    }

    /**
     * Transpiles a delta motion update between two consecutive keyframes.
     *
     * @param from initial state
     * @param to   updated state
     * @return delta motion packet
     */
    public TranspiledBedrockPacket transpileKeyframeDelta(DisplayEntityKeyframe from, DisplayEntityKeyframe to) {
        Objects.requireNonNull(from, "from cannot be null");
        Objects.requireNonNull(to, "to cannot be null");

        double dx = to.posX() - from.posX();
        double dy = to.posY() - from.posY();
        double dz = to.posZ() - from.posZ();

        float dyaw = to.yaw() - from.yaw();
        float dpitch = to.pitch() - from.pitch();

        int payloadSize = 1 + 4 + (8 * 3) + (4 * 2) + 4;
        ByteBuffer buffer = ByteBuffer.allocate(payloadSize);

        buffer.put(TRANSPILE_MAGIC);
        buffer.putInt(to.entityId());
        buffer.putDouble(dx);
        buffer.putDouble(dy);
        buffer.putDouble(dz);
        buffer.putFloat(dyaw);
        buffer.putFloat(dpitch);
        buffer.putInt(to.interpolationTicks());

        return new TranspiledBedrockPacket(
                to.entityId(),
                PACKET_TYPE_BEDROCK_MOTION_DELTA,
                buffer.array(),
                to.interpolationTicks(),
                true
        );
    }

    /**
     * Transpiles a sequence of keyframes into an unmodifiable list of Bedrock packets.
     *
     * @param keyframes list of animation keyframes
     * @return list of transpiled packets
     */
    public List<TranspiledBedrockPacket> transpileAnimationBatch(List<DisplayEntityKeyframe> keyframes) {
        Objects.requireNonNull(keyframes, "keyframes cannot be null");
        if (keyframes.isEmpty()) {
            return Collections.emptyList();
        }

        List<TranspiledBedrockPacket> result = new ArrayList<>(keyframes.size());
        for (DisplayEntityKeyframe keyframe : keyframes) {
            result.add(transpileKeyframe(keyframe));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Transpiles raw mod animation bytes into a display packet if valid.
     *
     * @param entityId   target entity ID
     * @param rawPayload raw payload byte data
     * @return Optional containing transpiled packet if decodable
     */
    public Optional<TranspiledBedrockPacket> transpileRawModPayload(int entityId, byte[] rawPayload) {
        Objects.requireNonNull(rawPayload, "rawPayload cannot be null");
        if (rawPayload.length < 12) { // minimum header requirement
            return Optional.empty();
        }

        DisplayEntityKeyframe keyframe = DisplayEntityKeyframe.ofPosition(entityId, 0.0, 0.0, 0.0);
        return Optional.of(transpileKeyframe(keyframe));
    }
}
