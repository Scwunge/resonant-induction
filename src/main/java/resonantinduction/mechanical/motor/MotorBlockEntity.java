package resonantinduction.mechanical.motor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;
import resonantinduction.RIConfig;
import resonantinduction.mechanical.MechanicalBlockEntity;
import resonantinduction.mechanical.MechanicalNode;
import resonantinduction.registry.RIRegistries;

/**
 * Electric Motor / Generator, as in the original: its front and back turn (or are turned by) gears and shafts; the other four
 * faces take or give power. As a motor (default) it spends stored energy as torque and speed, split by its gear (low, medium,
 * high); as a generator it brakes the rotation and stores the energy, pushing it out of its electric faces.
 * One mechanical joule is {@code feMechanicalRatio} FE.
 */
public class MotorBlockEntity extends MechanicalBlockEntity {
    private static final int CAPACITY = 1_000_000;

    private int energy;
    private boolean motor = true;
    private int gear;

    public MotorBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.MOTOR_BE.get(), pos, state);
    }

    public Direction facing() {
        return getBlockState().getValue(MotorBlock.FACING);
    }

    public boolean isMotor() {
        return motor;
    }

    public boolean toggleMode() {
        motor = !motor;
        setChanged();
        return motor;
    }

    public int toggleGear() {
        gear = (gear + 1) % 3;
        setChanged();
        return gear;
    }

    public int energy() {
        return energy;
    }

    @Override
    public double torqueLoad() {
        return 0.5;
    }

    @Override
    public double angularVelocityLoad() {
        return 0.5;
    }

    @Nullable
    @Override
    public MechanicalNode getNode(@Nullable Direction from) {
        return from == null || from.getAxis() == facing().getAxis() ? node : null;
    }

    @Override
    protected boolean canMesh(Direction dir, MechanicalBlockEntity other) {
        return dir.getAxis() == facing().getAxis();
    }

    @Override
    public boolean inverseRotation(Direction dir, MechanicalNode with) {
        return false;
    }

    private double ratio() {
        return RIConfig.get(RIConfig.MECHANICAL_FE_RATIO);
    }

    @Override
    public void onNodeUpdate(MechanicalNode n) {
        if (motor) {
            produceMechanical(n);
        } else {
            receiveMechanical(n);
            pushPower();
        }
    }

    /** Generator: take as much rotational energy as fits and brake the shaft by that share. */
    private void receiveMechanical(MechanicalNode n) {
        double power = n.getEnergy();
        if (power <= 0) {
            return;
        }
        int fe = (int) Math.min(CAPACITY - energy, power * ratio());
        if (fe > 0) {
            energy += fe;
            double used = fe / ratio() / power;
            n.apply(-n.getTorque() * used, -n.getAngularVelocity() * used);
            setChanged();
        }
    }

    /** Motor: turn the stored energy into torque and speed, as the original split them by gear. */
    private void produceMechanical(MechanicalNode n) {
        double extract = energy / ratio();
        if (extract <= 0) {
            return;
        }
        double torqueRatio = (gear + 1) / 2.2 * extract;
        if (torqueRatio <= 0) {
            return;
        }
        double maxVelocity = extract / torqueRatio;
        double maxTorque = extract / maxVelocity;
        double setVelocity = maxVelocity;
        double setTorque = maxTorque;
        double currentTorque = Math.abs(n.getTorque());
        if (currentTorque != 0) {
            setTorque = maxTorque * (n.getTorque() / currentTorque);
        }
        double currentVelocity = Math.abs(n.getAngularVelocity());
        if (currentVelocity != 0) {
            setVelocity = maxVelocity * (n.getAngularVelocity() / currentVelocity);
        }
        n.apply(setTorque - n.getTorque(), setVelocity - n.getAngularVelocity());
        energy = (int) Math.max(0, energy - Math.abs(setTorque * setVelocity) * ratio());
        setChanged();
    }

    private void pushPower() {
        for (Direction side : Direction.values()) {
            if (energy <= 0) {
                return;
            }
            if (side.getAxis() == facing().getAxis()) {
                continue;
            }
            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, worldPosition.relative(side), side.getOpposite());
            if (target != null && target.canReceive()) {
                energy -= Math.max(0, target.receiveEnergy(energy, false));
            }
        }
    }

    @Nullable
    public IEnergyStorage getEnergyCapability(@Nullable Direction side) {
        if (side != null && side.getAxis() == facing().getAxis()) {
            return null;
        }
        return new IEnergyStorage() {
            @Override
            public int receiveEnergy(int max, boolean simulate) {
                if (!motor) {
                    return 0;
                }
                int accepted = Math.max(0, Math.min(max, CAPACITY - energy));
                if (!simulate && accepted > 0) {
                    energy += accepted;
                    setChanged();
                }
                return accepted;
            }

            @Override
            public int extractEnergy(int max, boolean simulate) {
                if (motor) {
                    return 0;
                }
                int extracted = Math.max(0, Math.min(max, energy));
                if (!simulate && extracted > 0) {
                    energy -= extracted;
                    setChanged();
                }
                return extracted;
            }

            @Override
            public int getEnergyStored() {
                return energy;
            }

            @Override
            public int getMaxEnergyStored() {
                return CAPACITY;
            }

            @Override
            public boolean canExtract() {
                return !motor;
            }

            @Override
            public boolean canReceive() {
                return motor;
            }
        };
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", energy);
        tag.putBoolean("motor", motor);
        tag.putInt("gear", gear);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy = tag.getInt("energy");
        motor = !tag.contains("motor") || tag.getBoolean("motor");
        gear = tag.getInt("gear");
    }
}
