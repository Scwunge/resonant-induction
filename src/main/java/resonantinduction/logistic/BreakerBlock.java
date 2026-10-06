package resonantinduction.logistic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * Breaker, as the original: half a second after it's given a redstone signal it breaks the block in front of it, putting the drops
 * into an inventory behind it or dropping them there.
 */
public class BreakerBlock extends DirectionalBlock {
    public static final MapCodec<BreakerBlock> CODEC = simpleCodec(BreakerBlock::new);

    public BreakerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        neighborChanged(state, level, pos, this, pos, movedByPiston);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide && level.hasNeighborSignal(pos) && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 10);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.hasNeighborSignal(pos)) {
            return;
        }
        Direction facing = state.getValue(FACING);
        BlockPos target = pos.relative(facing);
        BlockState victim = level.getBlockState(target);
        if (victim.isAir() || victim.getDestroySpeed(level, target) < 0 || victim.getBlock() instanceof LiquidBlock) {
            return;
        }
        // The original took the block's drops with no tool, as if it could break anything.
        for (ItemStack drop : Block.getDrops(victim, level, target, level.getBlockEntity(target), null, new ItemStack(Items.DIAMOND_PICKAXE))) {
            BlockPos behind = pos.relative(facing.getOpposite());
            ItemStack left = ItemTransfer.store(level, behind, facing, drop);
            ItemTransfer.drop(level, behind, left);
        }
        level.destroyBlock(target, false);
    }
}
