package resonantinduction.atomic.fusion;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import resonantinduction.ResonantInduction;
import resonantinduction.atomic.ThermalGrid;
import resonantinduction.registry.RIRegistries;

/**
 * Plasma, as the original: a million kelvin, which it pours into the thermal grid. Every second it cools by a third and spreads
 * into its neighbours, burning through anything but bedrock, iron blocks and electromagnets; cooled to a tenth, it's just fire.
 * Touching it is fatal.
 */
public class PlasmaBlock extends BaseEntityBlock {
    public static final MapCodec<PlasmaBlock> CODEC = simpleCodec(PlasmaBlock::new);
    /** What plasma can't burn through. */
    public static final TagKey<Block> PLASMA_PROOF = TagKey.create(Registries.BLOCK, ResonantInduction.id("plasma_proof"));
    public static final int MAX_TEMPERATURE = 1_000_000;

    public PlasmaBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        entity.hurt(level.damageSources().inFire(), 100);
    }

    /** Puts plasma at {@code pos} (or heats what's there), unless it can't burn through. */
    public static void spawn(Level level, BlockPos pos, int temperature) {
        BlockState there = level.getBlockState(pos);
        if (there.is(PLASMA_PROOF) || level.isOutsideBuildHeight(pos)) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof Tile plasma) {
            plasma.temperature = temperature;
            return;
        }
        level.setBlockAndUpdate(pos, RIRegistries.PLASMA_BLOCK.get().defaultBlockState());
        if (level.getBlockEntity(pos) instanceof Tile plasma) {
            plasma.temperature = temperature;
        }
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
        if (level.isClientSide || type != RIRegistries.PLASMA_BE.get()) {
            return null;
        }
        return (l, p, s, be) -> ((Tile) be).tick(l, p);
    }

    public static class Tile extends BlockEntity {
        private float temperature = MAX_TEMPERATURE;
        private int ticks;

        public Tile(BlockPos pos, BlockState state) {
            super(RIRegistries.PLASMA_BE.get(), pos, state);
        }

        public float temperature() {
            return temperature;
        }

        void tick(Level level, BlockPos pos) {
            ThermalGrid.addTemperature(level, pos, (temperature - ThermalGrid.temperature(level, pos)) * 0.1f);
            if (++ticks % 20 != 0) {
                return;
            }
            temperature /= 1.5f;
            if (temperature <= MAX_TEMPERATURE / 10f) {
                level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
                return;
            }
            for (Direction d : Direction.values()) {
                if (level.random.nextFloat() <= 0.4f && !(level.getBlockEntity(pos.relative(d)) instanceof Tile)) {
                    spawn(level, pos.relative(d), (int) temperature);
                }
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putFloat("temperature", temperature);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            temperature = tag.getFloat("temperature");
        }
    }
}
