package resonantinduction.atomic.fusion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;
import resonantinduction.RIConfig;
import resonantinduction.registry.RIRegistries;

/**
 * Plasma Heater, as the original: with power, it heats deuterium and tritium (100 mB of each a tick) into plasma, which has to be
 * pumped out into a reactor cell. It takes power only while it has both gases.
 */
public class PlasmaHeaterBlockEntity extends BlockEntity {
    /** The original's use a tick, in joules; it holds twenty ticks' worth. */
    private static final long BASE_USE = 500_000_000;

    private final FluidTank deuterium = new FluidTank(10000, fs -> fs.getFluid().isSame(RIRegistries.DEUTERIUM.get()));
    private final FluidTank tritium = new FluidTank(10000, fs -> fs.getFluid().isSame(RIRegistries.TRITIUM.get()));
    private final FluidTank plasma = new FluidTank(10000, fs -> fs.getFluid().isSame(RIRegistries.PLASMA.get()));
    private long energy;
    private int ticks;
    /** Client side: the rotor's angle, in radians. */
    public float rotation, prevRotation;

    public PlasmaHeaterBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.PLASMA_HEATER_BE.get(), pos, state);
    }

    public FluidTank deuterium() {
        return deuterium;
    }

    public FluidTank tritium() {
        return tritium;
    }

    public FluidTank plasma() {
        return plasma;
    }

    public long energy() {
        return energy;
    }

    public long usePerTick() {
        return (long) Math.ceil(BASE_USE * RIConfig.get(RIConfig.ATOMIC_ENERGY_SCALE));
    }

    public long capacity() {
        return usePerTick() * 20;
    }

    void tickServer() {
        int amount = RIConfig.get(RIConfig.PLASMA_HEAT_AMOUNT);
        if (energy >= usePerTick() && deuterium.getFluidAmount() >= amount && tritium.getFluidAmount() >= amount
                && plasma.fill(new FluidStack(RIRegistries.PLASMA.get(), amount), IFluidHandler.FluidAction.SIMULATE) == amount) {
            deuterium.drain(amount, IFluidHandler.FluidAction.EXECUTE);
            tritium.drain(amount, IFluidHandler.FluidAction.EXECUTE);
            plasma.fill(new FluidStack(RIRegistries.PLASMA.get(), amount), IFluidHandler.FluidAction.EXECUTE);
            energy -= usePerTick();
        }
        if (++ticks % 80 == 0) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
        setChanged();
    }

    /** The rotor spins with the energy stored, as the original. */
    void tickClient() {
        prevRotation = rotation;
        rotation += (float) energy / Math.max(1, capacity());
    }

    /** Takes deuterium and tritium; gives out plasma. */
    public IFluidHandler getFluidCapability(@Nullable Direction side) {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 3;
            }

            private FluidTank tank(int i) {
                return i == 0 ? deuterium : i == 1 ? tritium : plasma;
            }

            @Override
            public FluidStack getFluidInTank(int i) {
                return tank(i).getFluid();
            }

            @Override
            public int getTankCapacity(int i) {
                return tank(i).getCapacity();
            }

            @Override
            public boolean isFluidValid(int i, FluidStack stack) {
                return i < 2 && tank(i).isFluidValid(stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                FluidTank into = deuterium.isFluidValid(resource) ? deuterium : tritium.isFluidValid(resource) ? tritium : null;
                int filled = into == null ? 0 : into.fill(resource, action);
                if (filled > 0 && action.execute()) {
                    setChanged();
                }
                return filled;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return plasma.isFluidValid(resource) ? drain(resource.getAmount(), action) : FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int max, FluidAction action) {
                FluidStack out = plasma.drain(max, action);
                if (!out.isEmpty() && action.execute()) {
                    setChanged();
                }
                return out;
            }
        };
    }

    public IEnergyStorage getEnergyCapability(@Nullable Direction side) {
        return new IEnergyStorage() {
            @Override
            public int receiveEnergy(int max, boolean simulate) {
                if (deuterium.isEmpty() || tritium.isEmpty()) {
                    return 0;
                }
                int accepted = (int) Math.max(0, Math.min(max, capacity() - energy));
                if (!simulate) {
                    energy += accepted;
                    setChanged();
                }
                return accepted;
            }

            @Override
            public int extractEnergy(int max, boolean simulate) {
                return 0;
            }

            @Override
            public int getEnergyStored() {
                return (int) Math.min(Integer.MAX_VALUE, energy);
            }

            @Override
            public int getMaxEnergyStored() {
                return (int) Math.min(Integer.MAX_VALUE, capacity());
            }

            @Override
            public boolean canExtract() {
                return false;
            }

            @Override
            public boolean canReceive() {
                return true;
            }
        };
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("energy", energy);
        tag.put("deuterium", deuterium.writeToNBT(registries, new CompoundTag()));
        tag.put("tritium", tritium.writeToNBT(registries, new CompoundTag()));
        tag.put("plasma", plasma.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy = tag.getLong("energy");
        deuterium.readFromNBT(registries, tag.getCompound("deuterium"));
        tritium.readFromNBT(registries, tag.getCompound("tritium"));
        plasma.readFromNBT(registries, tag.getCompound("plasma"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    static void tick(Level level, BlockPos pos, BlockState state, PlasmaHeaterBlockEntity be) {
        if (level.isClientSide) {
            be.tickClient();
        } else {
            be.tickServer();
        }
    }
}
