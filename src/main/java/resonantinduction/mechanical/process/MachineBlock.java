package resonantinduction.mechanical.process;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import resonantinduction.battery.BatteryBlock;
import resonantinduction.mechanical.MechanicalBlockEntity;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/** A directional mechanical machine. Placed facing away from the player; a wrench turns it to the clicked side. */
public class MachineBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private final Supplier<? extends BlockEntityType<? extends MechanicalBlockEntity>> type;
    private final BiFunction<BlockPos, BlockState, ? extends MechanicalBlockEntity> factory;
    private final VoxelShape shape;
    private final RenderShape render;
    private final MapCodec<MachineBlock> codec;

    public MachineBlock(Properties properties, Supplier<? extends BlockEntityType<? extends MechanicalBlockEntity>> type,
                        BiFunction<BlockPos, BlockState, ? extends MechanicalBlockEntity> factory, VoxelShape shape, RenderShape render) {
        super(properties);
        this.type = type;
        this.factory = factory;
        this.shape = shape;
        this.render = render;
        this.codec = simpleCodec(p -> new MachineBlock(p, type, factory, shape, render));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
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
    protected RenderShape getRenderShape(BlockState state) {
        return render;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.apply(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != type.get()) {
            return null;
        }
        return level.isClientSide ? (l, p, s, be) -> MechanicalBlockEntity.clientTick(l, p, s, (MechanicalBlockEntity) be)
                : (l, p, s, be) -> MechanicalBlockEntity.serverTick(l, p, s, (MechanicalBlockEntity) be);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        if (level.getBlockEntity(pos) instanceof MechanicalBlockEntity be) {
            be.markRecache();
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.getBlockEntity(pos) instanceof EntityCollider collider) {
            collider.collide(entity);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(BatteryBlock.WRENCHES)) {
            if (!level.isClientSide) {
                level.setBlockAndUpdate(pos, state.setValue(FACING, hit.getDirection()));
                if (level.getBlockEntity(pos) instanceof MechanicalBlockEntity be) {
                    be.markRecache();
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** Block entities that react to entities inside their block. */
    public interface EntityCollider {
        void collide(Entity entity);
    }
}
