package resonantinduction.atomic.reactor;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import resonantinduction.atomic.FuelRodItem;
import resonantinduction.registry.RIRegistries;

/**
 * Reactor Cell block. Cells stack into columns (the model changes for the bottom, middle and top). Right-click with a fuel rod puts
 * it in; right-click takes the rod out; sneak-use (or a click with nothing to do) opens the cell's screen.
 */
public class ReactorCellBlock extends BaseEntityBlock {
    public static final MapCodec<ReactorCellBlock> CODEC = simpleCodec(ReactorCellBlock::new);
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    public enum Part implements StringRepresentable {
        SINGLE, BOTTOM, MIDDLE, TOP;

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }

    public ReactorCellBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, Part.SINGLE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    private Part partFor(BlockGetter level, BlockPos pos) {
        boolean above = level.getBlockState(pos.above()).is(this);
        boolean below = level.getBlockState(pos.below()).is(this);
        return above && below ? Part.MIDDLE : above ? Part.BOTTOM : below ? Part.TOP : Part.SINGLE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(PART, partFor(context.getLevel(), context.getClickedPos()));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return dir.getAxis() == Direction.Axis.Y ? state.setValue(PART, partFor(level, pos)) : state;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorCellBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != RIRegistries.REACTOR_CELL_BE.get()) {
            return null;
        }
        return (l, p, s, be) -> ReactorCellBlockEntity.serverTick(l, p, s, (ReactorCellBlockEntity) be);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.isSecondaryUseActive() && stack.getItem() instanceof FuelRodItem && level.getBlockEntity(pos) instanceof ReactorCellBlockEntity cell) {
            ReactorCellBlockEntity primary = cell.primary();
            if (primary.inventory().getStackInSlot(0).isEmpty()) {
                if (!level.isClientSide) {
                    primary.inventory().setStackInSlot(0, stack.split(1));
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ReactorCellBlockEntity cell)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            ReactorCellBlockEntity primary = cell.primary();
            ItemStack rod = primary.inventory().getStackInSlot(0);
            if (!player.isSecondaryUseActive() && !rod.isEmpty()) {
                primary.inventory().setStackInSlot(0, ItemStack.EMPTY);
                if (!player.addItem(rod)) {
                    player.drop(rod, false);
                }
            } else if (player instanceof ServerPlayer sp) {
                sp.openMenu(primary, buf -> buf.writeBlockPos(primary.getBlockPos()));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0 && level.getBlockEntity(pos) instanceof ReactorCellBlockEntity cell && cell.temperature() >= 373) {
            level.addParticle(ParticleTypes.CLOUD, pos.getX() + random.nextInt(2), pos.getY() + 1, pos.getZ() + random.nextInt(2), 0, 0.1, 0);
            level.addParticle(ParticleTypes.BUBBLE, pos.getX() + random.nextInt(5) - 2, pos.getY(), pos.getZ() + random.nextInt(5) - 2, 0, 0, 0);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ReactorCellBlockEntity cell) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, cell.inventory().getStackInSlot(0));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
