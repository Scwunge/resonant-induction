package resonantinduction.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
        event.registrar("1").playToServer(MultimeterSettingsPayload.TYPE, MultimeterSettingsPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> MultimeterSettingsPayload.handle(payload, context)));
    }

    public static void sendZap(ServerLevel level, Vec3 from, Vec3 to, DyeColor color) {
        sendNear(level, from, new ZapPayload(from, to, color.getTextureDiffuseColor(), false));
    }

    /** A straight laser beam. */
    public static void sendBeam(ServerLevel level, Vec3 from, Vec3 to, int argb) {
        sendNear(level, from, new ZapPayload(from, to, argb, true));
    }

    /** Only to players whose client has this mod (vanilla clients and test players lack the channel). */
    private static void sendNear(ServerLevel level, Vec3 at, ZapPayload payload) {
        double rangeSq = ZAP_VIEW_RANGE * ZAP_VIEW_RANGE;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(at) <= rangeSq && player.connection.hasChannel(payload)) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    /** An electric arc (or, if straight, a laser beam) between two points, drawn client side. */
    public record ZapPayload(Vec3 from, Vec3 to, int color, boolean straight) implements CustomPacketPayload {
        public static final Type<ZapPayload> TYPE = new Type<>(ResonantInduction.id("zap"));
        public static final StreamCodec<ByteBuf, ZapPayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeDouble(p.from.x).writeDouble(p.from.y).writeDouble(p.from.z);
                    buf.writeDouble(p.to.x).writeDouble(p.to.y).writeDouble(p.to.z);
                    buf.writeInt(p.color);
                    buf.writeBoolean(p.straight);
                },
                buf -> new ZapPayload(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                        new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()), buf.readInt(), buf.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
