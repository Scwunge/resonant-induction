package resonantinduction.fluid;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;
import resonantinduction.registry.RIRegistries;
import resonantinduction.resource.MachineRecipes;

import java.util.Map;

/**
 * Gutter block. Its walls open toward neighbouring gutters and grates, and its floor toward a gutter below. Right-click with a
 * bucket fills or empties the whole connected run; right-click with dirty dust washes it by hand in the water (slowly).
 */
public class GutterBlock extends BaseEntityBlock {
    public static final MapCodec<GutterBlock> CODEC = simpleCodec(GutterBlock::new);
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final Map<Direction, BooleanProperty> SIDES = Map.of(
            Direction.NORTH, BlockStateProperties.NORTH, Direction.SOUTH, BlockStateProperties.SOUTH,
            Direction.EAST, BlockStateProperties.EAST, Direction.WEST, BlockStateProperties.WEST);
    private static final VoxelShape OUTLINE = Block.box(0, 0, 0, 16, 15.84, 16);
    private static final VoxelShape[] COLLISION = new VoxelShape[32];

    static {
        double t = 1.6;
        for (int mask = 0; mask < 32; mask++) {
            VoxelShape s = Shapes.empty();
            if ((mask & 1) == 0) {
                s = Shapes.or(s, Block.box(0, 0, 0, 16, t, 16));
            }
            if ((mask & 2) == 0) {
                s = Shapes.or(s, Block.box(0, 0, 0, 16, 16, t));
            }
            if ((mask & 4) == 0) {
                s = Shapes.or(s, Block.box(0, 0, 16 - t, 16, 16, 16));
            }
            if ((mask & 8) == 0) {
                s = Shapes.or(s, Block.box(0, 0, 0, t, 16, 16));
            }
            if ((mask & 16) == 0) {
                s = Shapes.or(s, Block.box(16 - t, 0, 0, 16, 16, 16));
            }
            COLLISION[mask] = s.optimize();
        }
    }

    public GutterBlock(Properties properties) {
        super(properties);
        BlockState def = stateDefinition.any().setValue(DOWN, false);
        for (BooleanProperty p : SIDES.values()) {
            def = def.setValue(p, false);
        }
        registerDefaultState(def);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DOWN, BlockStateProperties.NORTH, BlockStateProperties.SOUTH, BlockStateProperties.EAST, BlockStateProperties.WEST);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** Gutters open toward gutters and grates beside them, and toward a gutter below. */
    private static boolean opensTo(Direction dir, BlockState neighbour) {
        if (dir == Direction.DOWN) {
            return neighbour.getBlock() instanceof GutterBlock;
        }
        return neighbour.getBlock() instanceof GutterBlock || neighbour.getBlock() instanceof GrateBlock;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        state = state.setValue(DOWN, opensTo(Direction.DOWN, level.getBlockState(pos.below())));
        for (Map.Entry<Direction, BooleanProperty> e : SIDES.entrySet()) {
            state = state.setValue(e.getValue(), opensTo(e.getKey(), level.getBlockState(pos.relative(e.getKey()))));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (dir == Direction.DOWN) {
            return state.setValue(DOWN, opensTo(dir, neighbour));
        }
        BooleanProperty side = SIDES.get(dir);
        return side == null ? state : state.setValue(side, opensTo(dir, neighbour));
    }

    private static int mask(BlockState state) {
        return (state.getValue(DOWN) ? 1 : 0) | (state.getValue(BlockStateProperties.NORTH) ? 2 : 0) | (state.getValue(BlockStateProperties.SOUTH) ? 4 : 0)
                | (state.getValue(BlockStateProperties.WEST) ? 8 : 0) | (state.getValue(BlockStateProperties.EAST) ? 16 : 0);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return OUTLINE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION[mask(state)];
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return OUTLINE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GutterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != RIRegistries.GUTTER_BE.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> FluidNodeBlockEntity.serverTick(lvl, pos, st, (FluidNodeBlockEntity) be);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof FluidNodeBlockEntity be) {
            be.markRecache();
        }
    }

    @Override
    public void handlePrecipitation(BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation) {
        if (precipitation == Biome.Precipitation.RAIN && level.getBlockEntity(pos) instanceof GutterBlockEntity gutter) {
            gutter.fillRain();
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GutterBlockEntity gutter && !gutter.tank().isEmpty()) {
            if (gutter.tank().getFluid().getFluidType().getTemperature() >= 373) {
                entity.igniteForSeconds(5);
            } else if (entity.isOnFire()) {
                entity.clearFire();
            }
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof GutterBlockEntity gutter)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        ItemStack refined = MachineRecipes.mixer(stack);
        if (refined != null) {
            // Washing by hand, as the original: each try uses some water, and one in ten works.
            if (!level.isClientSide) {
                int amount = 50 + level.random.nextInt(50);
                FluidStack water = gutter.tank().drain(amount, IFluidHandler.FluidAction.SIMULATE);
                if (water.getFluid().isSame(Fluids.WATER) && water.getAmount() > 0) {
                    if (level.random.nextFloat() > 0.9f) {
                        if (level.random.nextFloat() > 0.1f) {
                            Block.popResource(level, player.blockPosition(), refined.copy());
                        }
                        stack.shrink(1);
                    }
                    gutter.tank().drain(amount, IFluidHandler.FluidAction.EXECUTE);
                    gutter.onFluidChanged();
                    level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.5f, 1f);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && FluidUtil.interactWithFluidHandler(player, hand, gutter.gridHandler())) {
            return ItemInteractionResult.SUCCESS;
        }
        return FluidUtil.getFluidHandler(stack).isPresent() ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return false;
    }
}
