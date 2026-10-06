package resonantinduction.atomic.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import resonantinduction.RIConfig;

import java.util.List;

/**
 * Base of the atomic machines: an inventory (slot 0 a battery that powers it), an FE buffer, tanks, a work timer and a GUI. Each
 * machine says how much it uses a tick (the original's joules, times the atomic energy scale) and lays out its screen.
 */
public abstract class AtomicMachineBlockEntity extends BlockEntity implements MenuProvider, MachineHost {
    public static final int BATTERY_SLOT = 0;

    protected final ItemStackHandler inventory;
    protected long energy;
    protected int timer;
    private int ticks;
    private long syncedHash;

    protected AtomicMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state);
        this.inventory = new ItemStackHandler(slots) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot == BATTERY_SLOT ? stack.getCapability(Capabilities.EnergyStorage.ITEM) != null : AtomicMachineBlockEntity.this.isItemValid(slot, stack);
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }

    @Override
    public ItemStackHandler inventory() {
        return inventory;
    }

    /** FE used per tick of work: the original's joules times the atomic energy scale. */
    public long usePerTick() {
        return (long) Math.ceil(baseUse() * RIConfig.get(RIConfig.ATOMIC_ENERGY_SCALE));
    }

    /** The original's joules per tick. */
    protected abstract long baseUse();

    /** Holds two ticks' worth, as the original. */
    public long capacity() {
        return Math.max(1, usePerTick() * 2);
    }

    public long energy() {
        return energy;
    }

    public int timer() {
        return timer;
    }

    /** Ticks a job takes. */
    public abstract int jobTime();

    /** Whether there's a job it can do now. */
    protected abstract boolean canWork();

    /** Finishes a job. */
    protected abstract void finishJob();

    protected abstract boolean isItemValid(int slot, ItemStack stack);

    /** The machine's tanks, for the screen and syncing. */
    public abstract List<FluidTank> tanks();

    /** What the screen shows. */
    public abstract MachineLayout layout();

    @Override
    public MachineLayout layout(ContainerData data) {
        return layout();
    }

    @Override
    public ContainerData data() {
        return data;
    }

    @Override
    public BlockEntity self() {
        return this;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AtomicMachineBlockEntity be) {
        be.tickServer();
    }

    protected void tickServer() {
        ticks++;
        handleContainers();
        if (canWork()) {
            discharge();
            if (energy >= usePerTick()) {
                if (timer == 0) {
                    timer = jobTime();
                }
                if (--timer < 1) {
                    finishJob();
                    timer = 0;
                }
                energy -= usePerTick();
            }
        } else {
            timer = 0;
        }
        if (ticks % 10 == 0) {
            syncTanks();
        }
        setChanged();
    }

    /** Moves fluid between the tanks and the containers in their slots (buckets, cells). Machines override to wire up slots. */
    protected void handleContainers() {}

    /**
     * Empties a full container from {@code in} into {@code tank} (or fills an empty one from it), putting the result in
     * {@code out}, as the original's fillOrDrainTank.
     */
    protected void fillOrDrain(int in, int out, FluidTank tank, boolean allowDrainOut) {
        ItemStack input = inventory.getStackInSlot(in);
        if (input.isEmpty()) {
            return;
        }
        ItemStack one = input.copyWithCount(1);
        IFluidHandlerItem handler = one.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) {
            return;
        }
        FluidStack contained = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (!contained.isEmpty()) {
            if (tank.fill(contained, IFluidHandler.FluidAction.SIMULATE) < contained.getAmount()) {
                return;
            }
            FluidStack drained = handler.drain(contained.getAmount(), IFluidHandler.FluidAction.EXECUTE);
            ItemStack result = handler.getContainer();
            if (fitsIn(out, result)) {
                tank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                inventory.extractItem(in, 1, false);
                inventory.insertItem(out, result, false);
            }
        } else if (allowDrainOut && !tank.isEmpty()) {
            int filled = handler.fill(tank.getFluid().copy(), IFluidHandler.FluidAction.EXECUTE);
            ItemStack result = handler.getContainer();
            if (filled > 0 && fitsIn(out, result)) {
                tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                inventory.extractItem(in, 1, false);
                inventory.insertItem(out, result, false);
            }
        }
    }

    private boolean fitsIn(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        ItemStack there = inventory.getStackInSlot(slot);
        return there.isEmpty() || (ItemStack.isSameItemSameComponents(there, stack) && there.getCount() + stack.getCount() <= there.getMaxStackSize());
    }

    /** Draws power from the battery in slot 0. */
    protected void discharge() {
        ItemStack battery = inventory.getStackInSlot(BATTERY_SLOT);
        IEnergyStorage storage = battery.isEmpty() ? null : battery.getCapability(Capabilities.EnergyStorage.ITEM);
        if (storage != null && storage.canExtract()) {
            long room = capacity() - energy;
            if (room > 0) {
                energy += storage.extractEnergy((int) Math.min(Integer.MAX_VALUE, room), false);
            }
        }
    }

    /** Adds {@code stack} to {@code slot} (merging); returns false if it won't fit. */
    protected boolean output(int slot, ItemStack stack) {
        if (!fitsIn(slot, stack)) {
            return false;
        }
        ItemStack there = inventory.getStackInSlot(slot);
        inventory.setStackInSlot(slot, there.isEmpty() ? stack.copy() : there.copyWithCount(there.getCount() + stack.getCount()));
        return true;
    }

    protected boolean hasRoomFor(int slot, ItemStack stack) {
        return fitsIn(slot, stack);
    }

    /** What the client sees, hashed: when it changes, the machine sends an update. */
    protected long syncHash() {
        long hash = timer > 0 ? 1 : 2;
        for (FluidTank t : tanks()) {
            hash = hash * 31 + FluidStack.hashFluidAndComponents(t.getFluid()) * 7 + t.getFluidAmount();
        }
        return hash;
    }

    private void syncTanks() {
        long hash = syncHash();
        if (hash != syncedHash) {
            syncedHash = hash;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    /** Machines take power only while they have work, as the original. */
    @Nullable
    public IEnergyStorage getEnergyCapability(@Nullable Direction side) {
        return new IEnergyStorage() {
            @Override
            public int receiveEnergy(int max, boolean simulate) {
                if (!canWork()) {
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

    /** Synced to the screen: energy (as two ints), capacity (two), timer, job time. */
    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> (int) (energy >>> 31);
                case 1 -> (int) (energy & Integer.MAX_VALUE);
                case 2 -> (int) (capacity() >>> 31);
                case 3 -> (int) (capacity() & Integer.MAX_VALUE);
                case 4 -> timer;
                case 5 -> jobTime();
                default -> 0;
            };
        }

        @Override
        public void set(int i, int value) {}

        @Override
        public int getCount() {
            return 6;
        }
    };

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MachineMenu(id, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.putLong("energy", energy);
        tag.putInt("timer", timer);
        List<FluidTank> tanks = tanks();
        for (int i = 0; i < tanks.size(); i++) {
            tag.put("tank" + i, tanks.get(i).writeToNBT(registries, new CompoundTag()));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        energy = tag.getLong("energy");
        timer = tag.getInt("timer");
        List<FluidTank> tanks = tanks();
        for (int i = 0; i < tanks.size(); i++) {
            tanks.get(i).readFromNBT(registries, tag.getCompound("tank" + i));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        List<FluidTank> tanks = tanks();
        for (int i = 0; i < tanks.size(); i++) {
            tag.put("tank" + i, tanks.get(i).writeToNBT(registries, new CompoundTag()));
        }
        tag.putInt("timer", timer);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        List<FluidTank> tanks = tanks();
        for (int i = 0; i < tanks.size(); i++) {
            tanks.get(i).readFromNBT(registries, tag.getCompound("tank" + i));
        }
        timer = tag.getInt("timer");
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        handleUpdateTag(pkt.getTag(), registries);
    }
}
