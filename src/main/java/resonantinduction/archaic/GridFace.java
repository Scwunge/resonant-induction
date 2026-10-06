package resonantinduction.archaic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * The 3x3 grid on top of a table, as seen by someone standing at its front: slot = row * 3 + column, row 0 the far side and
 * column 0 the left.
 */
public final class GridFace {
    private GridFace() {}

    /** The slot under {@code hit} on a table at {@code pos} whose front faces {@code front}. */
    public static int slot(Vec3 hit, BlockPos pos, Direction front) {
        double x = hit.x - pos.getX() - 0.5;
        double z = hit.z - pos.getZ() - 0.5;
        Direction away = front.getOpposite();
        Direction right = away.getClockWise();
        double u = x * right.getStepX() + z * right.getStepZ() + 0.5;
        double d = x * away.getStepX() + z * away.getStepZ() + 0.5;
        int col = Math.min(2, Math.max(0, (int) (u * 3)));
        int row = 2 - Math.min(2, Math.max(0, (int) (d * 3)));
        return row * 3 + col;
    }

    /** Where the centre of {@code slot} is, in block coordinates (x, z). */
    public static double[] centre(int slot, Direction front) {
        int row = slot / 3;
        int col = slot % 3;
        double u = (col + 0.5) / 3 - 0.5;
        double d = 1 - (row + 0.5) / 3 - 0.5;
        Direction away = front.getOpposite();
        Direction right = away.getClockWise();
        return new double[] {0.5 + u * right.getStepX() + d * away.getStepX(), 0.5 + u * right.getStepZ() + d * away.getStepZ()};
    }
}
