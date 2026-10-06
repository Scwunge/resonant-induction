package resonantinduction.atomic.machine;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** A machine's fluid side: fills go into its input tank, drains come out of its output tank. */
public class TwoTankHandler implements IFluidHandler {
    private final FluidTank in;
    private final FluidTank out;
    private final Runnable changed;

    public TwoTankHandler(FluidTank in, FluidTank out, Runnable changed) {
        this.in = in;
        this.out = out;
        this.changed = changed;
    }

    @Override
    public int getTanks() {
        return 2;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return (tank == 0 ? in : out).getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return (tank == 0 ? in : out).getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tank == 0 && in.isFluidValid(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (in == null) {
            return 0;
        }
        int filled = in.fill(resource, action);
        if (filled > 0 && action.execute()) {
            changed.run();
        }
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (out == null || !FluidStack.isSameFluidSameComponents(resource, out.getFluid())) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (out == null) {
            return FluidStack.EMPTY;
        }
        FluidStack drained = out.drain(maxDrain, action);
        if (!drained.isEmpty() && action.execute()) {
            changed.run();
        }
        return drained;
    }
}
