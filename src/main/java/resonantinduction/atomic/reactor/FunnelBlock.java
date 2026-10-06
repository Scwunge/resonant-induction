package resonantinduction.atomic.reactor;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;
import resonantinduction.atomic.Edges;
import resonantinduction.registry.RIRegistries;

/**
 * Steam Funnel, as the original: catches gas (steam) rising into it from below, holds sixteen buckets, and passes it up into the
 * turbine or funnel above. Funnels side by side share one border.
 */
public class FunnelBlock extends BaseEntityBlock {
    public static final MapCodec<FunnelBlock> CODEC = simpleCodec(FunnelBlock::new);

    public FunnelBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (Direction dir : Direction.values()) {
            state = state.setValue(Edges.property(dir), false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        Edges.addProperties(builder);
    }

    private boolean joins(BlockState other) {
        return other.is(this);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return Edges.join(defaultBlockState(), context.getLevel(), context.getClickedPos(), this::joins);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return Edges.update(state, dir, neighbour, this::joins);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tile(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != RIRegistries.FUNNEL_BE.get()) {
            return null;
        }
        return (l, p, s, be) -> ((Tile) be).tick(l, p);
    }

    public static class Tile extends BlockEntity {
        private final FluidTank tank = new FluidTank(16000, fs -> fs.getFluidType().isLighterThanAir());

        public Tile(BlockPos pos, BlockState state) {
            super(RIRegistries.FUNNEL_BE.get(), pos, state);
        }

        public FluidTank tank() {
            return tank;
        }

        void tick(Level level, BlockPos pos) {
            if (tank.isEmpty()) {
                return;
            }
            IFluidHandler above = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.above(), Direction.DOWN);
            if (above != null) {
                int moved = above.fill(tank.getFluid().copy(), IFluidHandler.FluidAction.EXECUTE);
                if (moved > 0) {
                    tank.drain(moved, IFluidHandler.FluidAction.EXECUTE);
                    setChanged();
                }
            }
        }

        /** Gas goes in from below and out at the top. */
        @Nullable
        public IFluidHandler getFluidCapability(@Nullable Direction side) {
            return new IFluidHandler() {
                @Override
                public int getTanks() {
                    return 1;
                }

                @Override
                public FluidStack getFluidInTank(int t) {
                    return tank.getFluid();
                }

                @Override
                public int getTankCapacity(int t) {
                    return tank.getCapacity();
                }

                @Override
                public boolean isFluidValid(int t, FluidStack stack) {
                    return side == Direction.DOWN && tank.isFluidValid(stack);
                }

                @Override
                public int fill(FluidStack resource, FluidAction action) {
                    return side == Direction.DOWN ? tank.fill(resource, action) : 0;
                }

                @Override
                public FluidStack drain(FluidStack resource, FluidAction action) {
                    return side == Direction.UP && FluidStack.isSameFluidSameComponents(resource, tank.getFluid()) ? tank.drain(resource.getAmount(), action) : FluidStack.EMPTY;
                }

                @Override
                public FluidStack drain(int maxDrain, FluidAction action) {
                    return side == Direction.UP ? tank.drain(maxDrain, action) : FluidStack.EMPTY;
                }
            };
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            tank.readFromNBT(registries, tag.getCompound("tank"));
        }
    }
}
