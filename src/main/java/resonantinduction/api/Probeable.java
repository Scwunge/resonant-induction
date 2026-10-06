package resonantinduction.api;

import net.minecraft.core.Direction;

/** Readings a block entity offers to the Multimeter beyond energy and fluid (mechanical, thermal and pressure systems). */
public interface Probeable {
    default double probeTorque(Direction side) {
        return 0;
    }

    default double probeAngularVelocity(Direction side) {
        return 0;
    }

    default double probeTemperature(Direction side) {
        return 0;
    }

    default double probePressure(Direction side) {
        return 0;
    }
}
