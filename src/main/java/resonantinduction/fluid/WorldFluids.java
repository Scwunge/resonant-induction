package resonantinduction.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import resonantinduction.registry.RIRegistries;
import resonantinduction.resource.MaterialFluid;
import resonantinduction.resource.PoolBlock;

/** Fluids standing in the world as blocks: source blocks (a bucket each) and the molten metal and mixture pools. */
public final class WorldFluids {
    private WorldFluids() {}

    /** The fluid in the block at {@code pos}, or empty; a pool is its metal's fluid. */
    public static FluidStack peek(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof PoolBlock pool) {
            Fluid fluid = pool.kind() == PoolBlock.Kind.MOLTEN ? RIRegistries.MOLTEN_METAL.get() : RIRegistries.DUST_MIXTURE.get();
            return MaterialFluid.stack(fluid, PoolBlock.material(level, pos), state.getValue(PoolBlock.LEVEL) * PoolBlock.MB_PER_LEVEL);
        }
        FluidState fluid = state.getFluidState();
        if (fluid.isSource() && state.getBlock() instanceof BucketPickup) {
            return new FluidStack(fluid.getType(), 1000);
        }
        return FluidStack.EMPTY;
    }

    /** The fluid (of any amount) in the block, for path finding: flowing fluid counts too. */
    public static Fluid fluidAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof PoolBlock pool) {
            return pool.kind() == PoolBlock.Kind.MOLTEN ? RIRegistries.MOLTEN_METAL.get() : RIRegistries.DUST_MIXTURE.get();
        }
        FluidState fluid = state.getFluidState();
        if (fluid.isEmpty()) {
            return Fluids.EMPTY;
        }
        Fluid type = fluid.getType();
        return type == Fluids.FLOWING_WATER ? Fluids.WATER : type == Fluids.FLOWING_LAVA ? Fluids.LAVA : type;
    }

    /** Takes the fluid out of the block (if {@code execute}) and returns what it held. */
    public static FluidStack drain(Level level, BlockPos pos, boolean execute) {
        FluidStack stack = peek(level, pos);
        if (stack.isEmpty() || !execute) {
            return stack;
        }
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof PoolBlock) {
            PoolBlock.lower(level, pos, 8);
        } else if (state.getBlock() instanceof BucketPickup pickup) {
            pickup.pickupBlock(null, level, pos, state);
        }
        return stack;
    }

    /** Puts {@code stack} into the world at {@code pos} if there's room, and returns how much went in. */
    public static int fill(Level level, BlockPos pos, FluidStack stack, boolean execute) {
        if (stack.isEmpty()) {
            return 0;
        }
        BlockState state = level.getBlockState(pos);
        if (!state.canBeReplaced() || !state.getFluidState().isEmpty() || state.getBlock() instanceof PoolBlock) {
            return 0;
        }
        if (stack.getFluid() == RIRegistries.MOLTEN_METAL.get() || stack.getFluid() == RIRegistries.DUST_MIXTURE.get()) {
            int levels = Math.min(8, stack.getAmount() / PoolBlock.MB_PER_LEVEL);
            if (levels <= 0) {
                return 0;
            }
            if (execute) {
                PoolBlock.Kind kind = stack.getFluid() == RIRegistries.MOLTEN_METAL.get() ? PoolBlock.Kind.MOLTEN : PoolBlock.Kind.MIXTURE;
                PoolBlock.place(level, pos, kind, MaterialFluid.material(stack), levels);
            }
            return levels * PoolBlock.MB_PER_LEVEL;
        }
        BlockState block = stack.getFluid().defaultFluidState().createLegacyBlock();
        if (stack.getAmount() < 1000 || block.isAir() || !block.getFluidState().isSource()) {
            return 0;
        }
        if (execute) {
            level.setBlockAndUpdate(pos, block);
        }
        return 1000;
    }
}
