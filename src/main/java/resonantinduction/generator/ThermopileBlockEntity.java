package resonantinduction.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.event.EventHooks;
import resonantinduction.registry.RIRegistries;

/**
 * Thermopile: makes power from the difference between hot and cold blocks touching it, as in the original. Water counts 1 cold,
 * ice and snow 2; fire 1 hot, lava 2. Output is 15 FE/t times (3 - |hot - cold|) when it has both. After two minutes of use it
 * reaches equilibrium: water boils away, ice melts, fire goes out and lava cools to stone.
 */
public class ThermopileBlockEntity extends GeneratorBlockEntity {
    private static final int MAX_USE_TICKS = 120 * 20;
    private int usingTicks;

    public ThermopileBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.THERMOPILE_BE.get(), pos, state);
    }

    @Override
    protected int capacity() {
        return 300;
    }

    @Override
    protected int generate(ServerLevel level) {
        int hot = 0;
        int cold = 0;
        for (Direction d : Direction.values()) {
            BlockState state = level.getBlockState(worldPosition.relative(d));
            if (state.getFluidState().is(Fluids.WATER) && state.getFluidState().isSource()) {
                cold++;
            } else if (state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.SNOW) || state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)) {
                cold += 2;
            } else if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
                hot++;
            } else if (state.getFluidState().is(Fluids.LAVA) && state.getFluidState().isSource()) {
                hot += 2;
            }
        }
        int multiplier = 3 - Math.abs(hot - cold);
        if (multiplier <= 0 || hot <= 0 || cold <= 0) {
            return 0;
        }
        if (++usingTicks >= MAX_USE_TICKS) {
            usingTicks = 0;
            equalize(level);
        }
        return 15 * multiplier;
    }

    private void equalize(ServerLevel level) {
        for (Direction d : Direction.values()) {
            BlockPos pos = worldPosition.relative(d);
            BlockState state = level.getBlockState(pos);
            if (state.getFluidState().is(Fluids.WATER) && state.getFluidState().isSource()) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            } else if (state.is(Blocks.ICE) || state.is(Blocks.SNOW_BLOCK)) {
                level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
            } else if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            } else if (state.getFluidState().is(Fluids.LAVA) && state.getFluidState().isSource()) {
                level.setBlockAndUpdate(pos, EventHooks.fireFluidPlaceBlockEvent(level, pos, pos, Blocks.STONE.defaultBlockState()));
            }
        }
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("usingTicks", usingTicks);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        usingTicks = tag.getInt("usingTicks");
    }
}
