package resonantinduction.atomic;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import resonantinduction.registry.RIRegistries;

/**
 * Cells as fluid containers, as the original registered them: a water cell holds a bucket of water, deuterium and tritium cells
 * 200 mB each. Emptied, a cell becomes an empty cell; an empty cell filled with exactly that much becomes the full one.
 */
public class CellFluidHandler implements IFluidHandlerItem {
    private ItemStack container;

    public CellFluidHandler(ItemStack container) {
        this.container = container;
    }

    /** What a full cell of this item holds, or empty. */
    public static FluidStack contents(Item item) {
        if (item == RIRegistries.WATER_CELL.get()) {
            return new FluidStack(Fluids.WATER, 1000);
        }
        if (item == RIRegistries.DEUTERIUM_CELL.get()) {
            return new FluidStack(RIRegistries.DEUTERIUM.get(), 200);
        }
        if (item == RIRegistries.TRITIUM_CELL.get()) {
            return new FluidStack(RIRegistries.TRITIUM.get(), 200);
        }
        return FluidStack.EMPTY;
    }

    private static Item cellFor(Fluid fluid) {
        if (fluid.isSame(Fluids.WATER)) {
            return RIRegistries.WATER_CELL.get();
        }
        if (fluid.isSame(RIRegistries.DEUTERIUM.get())) {
            return RIRegistries.DEUTERIUM_CELL.get();
        }
        if (fluid.isSame(RIRegistries.TRITIUM.get())) {
            return RIRegistries.TRITIUM_CELL.get();
        }
        return null;
    }

    @Override
    public ItemStack getContainer() {
        return container;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return container.getCount() == 1 ? contents(container.getItem()) : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        FluidStack held = contents(container.getItem());
        return held.isEmpty() ? 1000 : held.getAmount();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return cellFor(stack.getFluid()) != null;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (container.getCount() != 1 || !container.is(RIRegistries.EMPTY_CELL.get()) || resource.isEmpty()) {
            return 0;
        }
        Item full = cellFor(resource.getFluid());
        if (full == null) {
            return 0;
        }
        int need = contents(full).getAmount();
        if (resource.getAmount() < need) {
            return 0;
        }
        if (action.execute()) {
            container = new ItemStack(full);
        }
        return need;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        FluidStack held = getFluidInTank(0);
        if (held.isEmpty() || !FluidStack.isSameFluid(held, resource) || resource.getAmount() < held.getAmount()) {
            return FluidStack.EMPTY;
        }
        return drain(held.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        FluidStack held = getFluidInTank(0);
        if (held.isEmpty() || maxDrain < held.getAmount()) {
            return FluidStack.EMPTY;
        }
        if (action.execute()) {
            container = new ItemStack(RIRegistries.EMPTY_CELL.get());
        }
        return held;
    }
}
