package resonantinduction.schematic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import resonantinduction.client.CreativeBuilderScreen;

import java.util.Map;

/**
 * Creative Builder: a creative-mode block that puts down one of the schematics round itself, picked with its size on its screen
 * (as the original's builder did). Only players in creative mode can use it.
 */
public class CreativeBuilderBlock extends Block {
    public static final MapCodec<CreativeBuilderBlock> CODEC = simpleCodec(CreativeBuilderBlock::new);
    public static final int MAX_SIZE = 30;

    public CreativeBuilderBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isCreative()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.resonantinduction.creative_builder.creative_only"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) {
            CreativeBuilderScreen.open(pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Replaces the builder with {@code schematic} at {@code size}, turned the way {@code dir} says where that matters. */
    public static void build(ServerLevel level, BlockPos pos, Schematics.Schematic schematic, Direction dir, int size) {
        Map<BlockPos, BlockState> structure = schematic.structure(dir, size);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        for (Map.Entry<BlockPos, BlockState> e : structure.entrySet()) {
            BlockPos at = pos.offset(e.getKey());
            if (!level.isOutsideBuildHeight(at)) {
                level.setBlock(at, e.getValue(), Block.UPDATE_ALL);
            }
        }
    }
}
