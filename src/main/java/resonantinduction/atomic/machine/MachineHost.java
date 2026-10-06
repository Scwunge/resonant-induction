package resonantinduction.atomic.machine;

import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

/** Anything shown on the shared machine screen: its slots, tanks, layout and the numbers synced to the screen. */
public interface MachineHost {
    ItemStackHandler inventory();

    List<FluidTank> tanks();

    /** The layout; called every frame on the client, so it may show live numbers from {@link #data()}. */
    MachineLayout layout(ContainerData data);

    ContainerData data();

    BlockEntity self();
}
