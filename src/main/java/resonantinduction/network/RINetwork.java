package resonantinduction.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import resonantinduction.ResonantInduction;
import resonantinduction.client.ClientPayloads;

public final class RINetwork {
    /** Players within this many blocks of an arc see it. */
    private static final double ZAP_VIEW_RANGE = 96;

    private RINetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        // The handler body only runs on the client, so ClientPayloads never loads on a dedicated server.
        event.registrar("1").playToClient(ZapPayload.TYPE, ZapPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloads.zap(payload)));
    }

    public static void sendZap(ServerLevel level, Vec3 from, Vec3 to, DyeColor color) {
        PacketDistributor.sendToPlayersNear(level, null, from.x, from.y, from.z, ZAP_VIEW_RANGE,
                new ZapPayload(from, to, color.getTextureDiffuseColor()));
    }

    /** An electric arc between two points, drawn client side. */
    public record ZapPayload(Vec3 from, Vec3 to, int color) implements CustomPacketPayload {
        public static final Type<ZapPayload> TYPE = new Type<>(ResonantInduction.id("zap"));
        public static final StreamCodec<ByteBuf, ZapPayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeDouble(p.from.x).writeDouble(p.from.y).writeDouble(p.from.z);
                    buf.writeDouble(p.to.x).writeDouble(p.to.y).writeDouble(p.to.z);
                    buf.writeInt(p.color);
                },
                buf -> new ZapPayload(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                        new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()), buf.readInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
