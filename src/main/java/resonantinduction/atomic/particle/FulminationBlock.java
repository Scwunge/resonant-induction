package resonantinduction.atomic.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
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
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;
import resonantinduction.RIConfig;
import resonantinduction.atomic.Edges;
import resonantinduction.registry.RIRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Fulmination Generator, as the original: catches the energy of antimatter going off within the blast radius, if it has a line
 * of sight to the blast, and sends it out of every side. Generators in one blast share its energy.
 */
public class FulminationBlock extends BaseEntityBlock {
    public static final MapCodec<FulminationBlock> CODEC = simpleCodec(FulminationBlock::new);
    /** The original's buffer, in joules. */
    private static final double CAPACITY = 10_000_000_000_000d;

    public FulminationBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (Direction dir : Direction.values()) {
            state = state.setValue(Edges.property(dir), false);
        }
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
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
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != RIRegistries.FULMINATION_BE.get()) {
            return null;
        }
        return (l, p, s, be) -> ((Tile) be).tick(l, p);
    }

    /**
     * An antimatter blast of {@code joules} at {@code centre}: generators within {@code radius} that can see it split the energy,
     * each keeping the share its exposure lets through.
     */
    public static void absorb(ServerLevel level, Vec3 centre, float radius, double joules) {
        List<Tile> open = new ArrayList<>();
        List<Float> exposures = new ArrayList<>();
        int r = (int) Math.ceil(radius);
        BlockPos middle = BlockPos.containing(centre);
        for (BlockPos pos : BlockPos.betweenClosed(middle.offset(-r, -r, -r), middle.offset(r, r, r))) {
            double distance = pos.getCenter().distanceTo(centre);
            if (distance > radius || distance <= 0 || !(level.getBlockEntity(pos) instanceof Tile tile)) {
                continue;
            }
            float exposure = exposure(level, centre, pos);
            if (exposure > 0) {
                open.add(tile);
                exposures.add(exposure);
            }
        }
        double each = joules / Math.max(1, open.size()) * RIConfig.get(RIConfig.ANTIMATTER_ENERGY_SCALE);
        for (int i = 0; i < open.size(); i++) {
            open.get(i).receive((long) (each * exposures.get(i)));
        }
    }

    /** The share of rays from the blast to points over the block that nothing else stops (the original's block density, inverted). */
    static float exposure(Level level, Vec3 centre, BlockPos pos) {
        int clear = 0;
        int total = 0;
        for (int x = 0; x <= 2; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = 0; z <= 2; z++) {
                    Vec3 point = new Vec3(pos.getX() + 0.05 + 0.45 * x, pos.getY() + 0.05 + 0.45 * y, pos.getZ() + 0.05 + 0.45 * z);
                    var hit = level.clip(new ClipContext(centre, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
                    if (hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos)) {
                        clear++;
                    }
                    total++;
                }
            }
        }
        return (float) clear / total;
    }

    public static class Tile extends BlockEntity {
        private long energy;

        public Tile(BlockPos pos, BlockState state) {
            super(RIRegistries.FULMINATION_BE.get(), pos, state);
        }

        public static long capacity() {
            return (long) (CAPACITY * RIConfig.get(RIConfig.ANTIMATTER_ENERGY_SCALE));
        }

        public long energy() {
            return energy;
        }

        void receive(long amount) {
            energy = Math.min(capacity(), energy + Math.max(0, amount));
            setChanged();
        }

        void tick(Level level, BlockPos pos) {
            if (energy <= 0) {
                return;
            }
            for (Direction dir : Direction.values()) {
                IEnergyStorage out = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(dir), dir.getOpposite());
                if (out != null && out.canReceive()) {
                    energy -= out.receiveEnergy((int) Math.min(Integer.MAX_VALUE, energy), false);
                }
            }
            // It leaks a little, as the original.
            energy = Math.max(0, energy - 1);
            setChanged();
        }

        public IEnergyStorage getEnergyCapability(@Nullable Direction side) {
            return new IEnergyStorage() {
                @Override
                public int receiveEnergy(int max, boolean simulate) {
                    return 0;
                }

                @Override
                public int extractEnergy(int max, boolean simulate) {
                    int out = (int) Math.min(max, energy);
                    if (!simulate) {
                        energy -= out;
                        setChanged();
                    }
                    return out;
                }

                @Override
                public int getEnergyStored() {
                    return (int) Math.min(Integer.MAX_VALUE, energy);
                }

                @Override
                public int getMaxEnergyStored() {
                    return (int) Math.min(Integer.MAX_VALUE, capacity());
                }

                @Override
                public boolean canExtract() {
                    return true;
                }

                @Override
                public boolean canReceive() {
                    return false;
                }
            };
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putLong("energy", energy);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            energy = tag.getLong("energy");
        }
    }
}
