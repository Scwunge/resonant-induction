package resonantinduction.atomic.reactor;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import resonantinduction.atomic.fusion.PlasmaBlock;

/** Plasma injected into a reactor cell escapes, a bucket at a time, as plasma blocks two blocks out, as the original. */
final class FusionHooks {
    private FusionHooks() {}

    static void releasePlasma(ServerLevel level, ReactorCellBlockEntity cell) {
        if (cell.tank().getFluidAmount() < 1000) {
            return;
        }
        Direction dir = Direction.from2DDataValue(level.random.nextInt(4));
        var at = cell.getBlockPos().relative(dir, 2).above(Math.max(level.random.nextInt(cell.height()) - 1, 0));
        if (level.isEmptyBlock(at)) {
            PlasmaBlock.spawn(level, at, PlasmaBlock.MAX_TEMPERATURE);
            cell.tank().drain(1000, IFluidHandler.FluidAction.EXECUTE);
        }
    }
}
