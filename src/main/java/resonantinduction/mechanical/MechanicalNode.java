package resonantinduction.mechanical;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rotational power, ported from the original MechanicalNode. Every tick a node eases its torque and angular velocity toward
 * what each connected node imposes on it (scaled by the gear ratio and flipped for meshing gears), and loses a little to load.
 * Power = torque x angular velocity.
 */
public class MechanicalNode {
    /** One tick, in seconds. */
    public static final double DELTA = 0.05;

    public double torque;
    public double angularVelocity;
    public double prevAngularVelocity;
    public float acceleration = 2f;
    /** Client-side visual angle (radians). */
    public double angle;
    public double prevAngle;
    protected double load = 2;

    private final Map<MechanicalNode, Direction> connections = new LinkedHashMap<>();
    private final Owner owner;

    /** How the block that owns a node shapes its behaviour. */
    public interface Owner {
        /** Torque lost per second as a fraction of the torque (plus a tenth of this value flat). */
        default double torqueLoad() {
            return 2;
        }

        default double angularVelocityLoad() {
            return 2;
        }

        /** Gear ratio towards {@code with} in direction {@code dir}; the effective ratio is theirs / ours. */
        default float ratio(Direction dir, MechanicalNode with) {
            return 0.5f;
        }

        /** Whether rotation flips across this connection (meshing gears do); both sides must agree. */
        default boolean inverseRotation(Direction dir, MechanicalNode with) {
            return true;
        }

        /** Called every server tick after the physics, for machines to use the power. */
        default void onNodeUpdate(MechanicalNode node) {
        }
    }

    public MechanicalNode(Owner owner) {
        this.owner = owner;
    }

    public Owner owner() {
        return owner;
    }

    public Map<MechanicalNode, Direction> connections() {
        return connections;
    }

    public void apply(double torque, double angularVelocity) {
        this.torque += torque;
        this.angularVelocity += angularVelocity;
    }

    public double getTorque() {
        return angularVelocity != 0 ? torque : 0;
    }

    public double getAngularVelocity() {
        return torque != 0 ? angularVelocity : 0;
    }

    public double getEnergy() {
        return getTorque() * getAngularVelocity();
    }

    /** Watts (energy per second). */
    public double getPower() {
        return getEnergy() / DELTA;
    }

    /** One server tick of the original physics. */
    public void serverTick() {
        prevAngularVelocity = angularVelocity;
        double accel = acceleration * DELTA;

        double torqueLoad = owner.torqueLoad();
        double torqueLoss = Math.min(Math.abs(getTorque()), (Math.abs(getTorque() * torqueLoad) + torqueLoad / 10) * DELTA);
        torque += torque > 0 ? -torqueLoss : torqueLoss;
        double velocityLoad = owner.angularVelocityLoad();
        double velocityLoss = Math.min(Math.abs(getAngularVelocity()), (Math.abs(getAngularVelocity() * velocityLoad) + velocityLoad / 10) * DELTA);
        angularVelocity += angularVelocity > 0 ? -velocityLoss : velocityLoss;
        if (getEnergy() <= 0) {
            angularVelocity = 0;
            torque = 0;
        }

        for (Map.Entry<MechanicalNode, Direction> e : connections.entrySet()) {
            MechanicalNode other = e.getKey();
            Direction dir = e.getValue();
            float ratio = other.owner.ratio(dir.getOpposite(), this) / owner.ratio(dir, other);
            boolean inverse = owner.inverseRotation(dir, other) && other.owner.inverseRotation(dir.getOpposite(), this);
            int inversion = inverse ? -1 : 1;

            double targetTorque = inversion * other.getTorque() / ratio;
            double applyTorque = targetTorque * accel;
            if (Math.abs(torque + applyTorque) < Math.abs(targetTorque)) {
                torque += applyTorque;
            } else if (Math.abs(torque - applyTorque) > Math.abs(targetTorque)) {
                torque -= applyTorque;
            }
            double targetVelocity = inversion * other.getAngularVelocity() * ratio;
            double applyVelocity = targetVelocity * accel;
            if (Math.abs(angularVelocity + applyVelocity) < Math.abs(targetVelocity)) {
                angularVelocity += applyVelocity;
            } else if (Math.abs(angularVelocity - applyVelocity) > Math.abs(targetVelocity)) {
                angularVelocity -= applyVelocity;
            }
        }
        owner.onNodeUpdate(this);
    }

    /** Client-side: advance the visual angle (the original capped it at half a turn per tick). */
    public void clientTick() {
        prevAngle = angle;
        double step = Math.max(-Math.PI, Math.min(Math.PI, angularVelocity)) * DELTA;
        angle = (angle + step) % (Math.PI * 2);
        if (Math.abs(angle - prevAngle) > Math.PI) {
            prevAngle = angle - step;
        }
    }

    public void save(CompoundTag tag) {
        tag.putDouble("torque", torque);
        tag.putDouble("angularVelocity", angularVelocity);
    }

    public void load(CompoundTag tag) {
        torque = tag.getDouble("torque");
        angularVelocity = tag.getDouble("angularVelocity");
    }
}
