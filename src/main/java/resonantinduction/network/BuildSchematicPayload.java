package resonantinduction.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import resonantinduction.ResonantInduction;
import resonantinduction.registry.RIRegistries;
import resonantinduction.schematic.CreativeBuilderBlock;
import resonantinduction.schematic.Schematics;

/** Client to server: build a schematic from the Creative Builder's screen. */
public record BuildSchematicPayload(BlockPos pos, int schematic, int size) implements CustomPacketPayload {
    public static final Type<BuildSchematicPayload> TYPE = new Type<>(ResonantInduction.id("build_schematic"));
    public static final StreamCodec<ByteBuf, BuildSchematicPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                BlockPos.STREAM_CODEC.encode(buf, p.pos);
                buf.writeByte(p.schematic).writeByte(p.size);
            },
            buf -> new BuildSchematicPayload(BlockPos.STREAM_CODEC.decode(buf), buf.readByte(), buf.readByte()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BuildSchematicPayload p, IPayloadContext context) {
        Player player = context.player();
        // Only a creative player at the builder, for a schematic that exists and is turned on, at a sane size.
        if (!player.isCreative() || player.distanceToSqr(p.pos.getCenter()) > 64 || !player.level().isLoaded(p.pos)
                || !player.level().getBlockState(p.pos).is(RIRegistries.CREATIVE_BUILDER.get())
                || p.schematic < 0 || p.schematic >= Schematics.ALL.size() || p.size < 1 || p.size > CreativeBuilderBlock.MAX_SIZE) {
            return;
        }
        Schematics.Schematic schematic = Schematics.ALL.get(p.schematic);
        if (schematic.available()) {
            Direction dir = Direction.orderedByNearest(player)[0];
            CreativeBuilderBlock.build((ServerLevel) player.level(), p.pos, schematic, dir, p.size);
        }
    }
}
