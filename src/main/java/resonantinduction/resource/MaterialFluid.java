package resonantinduction.resource;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import resonantinduction.fluid.VirtualFluid;
import resonantinduction.registry.RIRegistries;

import java.util.function.Supplier;

/**
 * Molten metal and dust mixture as fluids for tanks, gutters and pipes. They carry their metal as a component on the fluid
 * stack (the original had one fluid per metal); in the world they are {@link PoolBlock}s, so this fluid has no block or bucket.
 */
public class MaterialFluid extends VirtualFluid {
    public MaterialFluid(Supplier<FluidType> type) {
        super(type);
    }

    public static FluidStack stack(Fluid fluid, String material, int amount) {
        FluidStack stack = new FluidStack(fluid, amount);
        stack.set(RIRegistries.MATERIAL.get(), material);
        return stack;
    }

    public static String material(FluidStack stack) {
        return stack.getOrDefault(RIRegistries.MATERIAL.get(), "");
    }
}
