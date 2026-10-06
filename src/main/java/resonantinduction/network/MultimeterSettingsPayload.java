package resonantinduction.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import resonantinduction.ResonantInduction;
import resonantinduction.multimeter.Measure;
import resonantinduction.multimeter.MultimeterBlockEntity;

/** Client to server: new multimeter settings from its screen. */
public record MultimeterSettingsPayload(BlockPos pos, int mode, int detect, int graph, double limit) implements CustomPacketPayload {
    public static final Type<MultimeterSettingsPayload> TYPE = new Type<>(ResonantInduction.id("multimeter_settings"));
    public static final StreamCodec<ByteBuf, MultimeterSettingsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                BlockPos.STREAM_CODEC.encode(buf, p.pos);
                buf.writeByte(p.mode).writeByte(p.detect).writeByte(p.graph).writeDouble(p.limit);
            },
            buf -> new MultimeterSettingsPayload(BlockPos.STREAM_CODEC.decode(buf), buf.readByte(), buf.readByte(), buf.readByte(), buf.readDouble()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MultimeterSettingsPayload p, IPayloadContext context) {
        Player player = context.player();
        // Only from someone standing at the screen, with sane values.
        if (player.distanceToSqr(p.pos.getX() + 0.5, p.pos.getY() + 0.5, p.pos.getZ() + 0.5) > 64 || !player.level().isLoaded(p.pos)
                || !Double.isFinite(p.limit)) {
            return;
        }
        if (player.level().getBlockEntity(p.pos) instanceof MultimeterBlockEntity meter) {
            MultimeterBlockEntity.DetectMode[] modes = MultimeterBlockEntity.DetectMode.values();
            meter.applySettings(modes[Math.floorMod(p.mode, modes.length)], Measure.byIndex(p.detect), Measure.byIndex(p.graph), p.limit);
        }
    }
}
