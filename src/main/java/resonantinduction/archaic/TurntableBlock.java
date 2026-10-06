package resonantinduction.archaic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import resonantinduction.fluid.FluidNodeBlockEntity;
import resonantinduction.mechanical.MechanicalBlockEntity;

/**
 * Turntable, as the original: while powered by redstone it keeps turning the block on its face a quarter turn about its own axis
 * (every quarter second). Placed against a block, its face points at that block.
 */
public class TurntableBlock extends DirectionalBlock {
    public static final MapCodec<TurntableBlock> CODEC = simpleCodec(TurntableBlock::new);
    private static final int TICK_RATE = 5;

    public TurntableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
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
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
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
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide && level.hasNeighborSignal(pos) && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 2 * TICK_RATE);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.hasNeighborSignal(pos)) {
            return;
        }
        Direction facing = state.getValue(FACING);
        BlockPos target = pos.relative(facing);
        BlockState before = level.getBlockState(target);
        BlockState after = turn(before, level, target, facing);
        if (after != before) {
            level.setBlockAndUpdate(target, after);
            if (level.getBlockEntity(target) instanceof MechanicalBlockEntity m) {
                m.markRecache();
            } else if (level.getBlockEntity(target) instanceof FluidNodeBlockEntity f) {
                f.markRecache();
            }
            level.playSound(null, pos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5f, random.nextFloat() * 0.15f + 0.6f);
        }
        level.scheduleTick(pos, this, TICK_RATE);
    }

    /** {@code state} turned a quarter about {@code axis} (as seen looking from the turntable along it). */
    public static BlockState turn(BlockState state, Level level, BlockPos pos, Direction axis) {
        if (state.isAir()) {
            return state;
        }
        if (axis.getAxis() == Direction.Axis.Y) {
            return state.rotate(level, pos, axis == Direction.UP ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90);
        }
        // About a horizontal axis: turn any facing or axis the block has.
        for (Property<?> p : state.getProperties()) {
            if (p instanceof DirectionProperty dp) {
                Direction d = state.getValue(dp);
                Direction turned = d.getAxis() == axis.getAxis() ? d : d.getClockWise(axis.getAxis());
                if (axis.getAxisDirection() == Direction.AxisDirection.NEGATIVE && d.getAxis() != axis.getAxis()) {
                    turned = d.getCounterClockWise(axis.getAxis());
                }
                if (dp.getPossibleValues().contains(turned)) {
                    return state.setValue(dp, turned);
                }
            } else if (p == BlockStateProperties.AXIS) {
                Direction.Axis a = state.getValue(BlockStateProperties.AXIS);
                if (a != axis.getAxis()) {
                    Direction.Axis other = a == Direction.Axis.Y ? (axis.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X) : Direction.Axis.Y;
                    return state.setValue(BlockStateProperties.AXIS, other);
                }
            }
        }
        return state;
    }
}
