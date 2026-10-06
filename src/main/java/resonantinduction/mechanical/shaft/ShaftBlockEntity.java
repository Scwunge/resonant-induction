package resonantinduction.mechanical.shaft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import resonantinduction.mechanical.MechanicalBlockEntity;
import resonantinduction.mechanical.MechanicalNode;
import resonantinduction.mechanical.gear.GearBlockEntity;
import resonantinduction.registry.RIRegistries;

/** Gear shaft: carries rotation along its axis to shafts, gears facing it, and machines at either end. */
public class ShaftBlockEntity extends MechanicalBlockEntity {
    public ShaftBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.SHAFT_BE.get(), pos, state);
    }

    public Direction.Axis axis() {
        return getBlockState().getValue(BlockStateProperties.AXIS);
    }

    public int tier() {
        return ((ShaftBlock) getBlockState().getBlock()).tier();
    }

    @Override
    public double torqueLoad() {
        return switch (tier()) {
            case 0 -> 0.03;
            case 1 -> 0.02;
            default -> 0.01;
        };
    }

    @Override
    public double angularVelocityLoad() {
        return 0;
    }

    @Override
    protected boolean canMesh(Direction dir, MechanicalBlockEntity other) {
        if (dir.getAxis() != axis()) {
            return false;
        }
        if (other instanceof ShaftBlockEntity shaft) {
            return shaft.axis() == axis();
        }
        if (other instanceof GearBlockEntity gear) {
            return gear.attach() == dir;
        }
        return true;
    }

    @Override
    public boolean inverseRotation(Direction dir, MechanicalNode with) {
        return false;
    }
}
