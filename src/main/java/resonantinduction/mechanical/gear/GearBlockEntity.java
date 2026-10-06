package resonantinduction.mechanical.gear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import resonantinduction.mechanical.MechanicalBlockEntity;
import resonantinduction.mechanical.MechanicalNode;
import resonantinduction.mechanical.shaft.ShaftBlockEntity;
import resonantinduction.registry.RIRegistries;

/**
 * A gear lying against the face of a block (ATTACH points at that block). It meshes with gears beside it on the same face,
 * drives (or is driven by) the block behind it, and takes a shaft running into its centre from the front.
 * Tiers: 0 wood, 1 stone, 2 metal, 3 creative (always spins, the original's tier 10).
 */
public class GearBlockEntity extends MechanicalBlockEntity {
    private int crankTicks;
    private boolean crankClockwise = true;

    public GearBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.GEAR_BE.get(), pos, state);
    }

    public Direction attach() {
        return getBlockState().getValue(GearBlock.ATTACH);
    }

    public int tier() {
        return ((GearBlock) getBlockState().getBlock()).tier();
    }

    /** Hand crank: 20 ticks of turning (sneaking turns the other way), as in the original. */
    public void crank(boolean clockwise) {
        crankClockwise = clockwise;
        crankTicks = 20;
    }

    public void reverse() {
        node.torque = -node.torque;
        node.angularVelocity = -node.angularVelocity;
    }

    @Override
    protected void tickServer() {
        if (crankTicks > 0) {
            crankTicks--;
            node.apply(crankClockwise ? 15 : -15, crankClockwise ? 0.025 : -0.025);
        }
        super.tickServer();
    }

    @Override
    public void onNodeUpdate(MechanicalNode n) {
        if (tier() == GearBlock.CREATIVE) {
            n.torque = 100;
            n.angularVelocity = 100;
        }
    }

    @Override
    public double torqueLoad() {
        return switch (tier()) {
            case 0 -> 0.3;
            case 1 -> 0.2;
            case 2 -> 0.1;
            default -> 0;
        };
    }

    @Override
    public double angularVelocityLoad() {
        return switch (tier()) {
            case 0 -> 0.03;
            case 1 -> 0.02;
            case 2 -> 0.01;
            default -> 0;
        };
    }

    @Override
    protected boolean canMesh(Direction dir, MechanicalBlockEntity other) {
        Direction attach = attach();
        if (dir == attach) {
            // The block behind: a machine, or a gear back to back with this one. Not a shaft's side.
            return !(other instanceof GearBlockEntity gear) || gear.attach() == attach.getOpposite();
        }
        if (dir == attach.getOpposite()) {
            // In front: only a shaft running into the gear's centre.
            return other instanceof ShaftBlockEntity shaft && shaft.axis() == attach.getAxis();
        }
        // Beside: gears on the same face.
        return other instanceof GearBlockEntity gear && gear.attach() == attach;
    }

    /** Gears on the same face spin opposite ways; back-to-back gears and shafts turn together. */
    @Override
    public boolean inverseRotation(Direction dir, MechanicalNode with) {
        return dir.getAxis() != attach().getAxis();
    }
}
