package resonantinduction.fluid;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/** A block entity with a {@link FluidNode}: gutters, pipes, the pump and the grate. */
public interface FluidNodeProvider {
    /** The node seen from {@code from} (the side of this block touched), or null for none there. */
    @Nullable
    FluidNode getFluidNode(@Nullable Direction from);
}
