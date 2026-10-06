package resonantinduction.quantum;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Per-world quantum state, kept with the overworld's saved data: every complete gate by frequency (so gates in unloaded
 * chunks can still be reached), and the one-slot inventory and one-bucket tank that all gates of a frequency share.
 */
public class QuantumGateData extends SavedData {
    private static final String NAME = "resonantinduction_quantum";
    public static final int TANK_CAPACITY = 1000;

    private final Map<Integer, Set<GlobalPos>> gates = new HashMap<>();
    private final Map<Integer, ItemStack> items = new HashMap<>();
    private final Map<Integer, FluidTank> tanks = new HashMap<>();

    public static QuantumGateData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(QuantumGateData::new, QuantumGateData::load, null), NAME);
    }

    public Set<GlobalPos> gates(int frequency) {
        return gates.getOrDefault(frequency, Set.of());
    }

    public void addGate(int frequency, GlobalPos pos) {
        if (gates.computeIfAbsent(frequency, k -> new HashSet<>()).add(pos)) {
            setDirty();
        }
    }

    public void removeGate(int frequency, GlobalPos pos) {
        Set<GlobalPos> set = gates.get(frequency);
        if (set != null && set.remove(pos)) {
            if (set.isEmpty()) {
                gates.remove(frequency);
            }
            setDirty();
        }
    }

    /** Forgets every gate and the shared storage of a frequency (used by tests; gates re-register while loaded). */
    public void clearFrequency(int frequency) {
        gates.remove(frequency);
        items.remove(frequency);
        tanks.remove(frequency);
        setDirty();
    }

    public IItemHandlerModifiable inventory(int frequency) {
        return new IItemHandlerModifiable() {
            @Override
            public void setStackInSlot(int slot, ItemStack stack) {
                items.put(frequency, stack);
                setDirty();
            }

            @Override
            public int getSlots() {
                return 1;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return items.getOrDefault(frequency, ItemStack.EMPTY);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                ItemStack existing = getStackInSlot(0);
                if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack)) {
                    return stack;
                }
                int limit = Math.min(getSlotLimit(0), stack.getMaxStackSize());
                int space = limit - existing.getCount();
                if (space <= 0) {
                    return stack;
                }
                int moved = Math.min(space, stack.getCount());
                if (!simulate) {
                    setStackInSlot(0, stack.copyWithCount(existing.getCount() + moved));
                }
                return stack.copyWithCount(stack.getCount() - moved);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                ItemStack existing = getStackInSlot(0);
                if (existing.isEmpty() || amount <= 0) {
                    return ItemStack.EMPTY;
                }
                int taken = Math.min(amount, existing.getCount());
                if (!simulate) {
                    setStackInSlot(0, existing.copyWithCount(existing.getCount() - taken));
                }
                return existing.copyWithCount(taken);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 64;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return true;
            }
        };
    }

    public IFluidHandler tank(int frequency) {
        return tanks.computeIfAbsent(frequency, k -> newTank());
    }

    private FluidTank newTank() {
        return new FluidTank(TANK_CAPACITY) {
            @Override
            protected void onContentsChanged() {
                setDirty();
            }
        };
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        Set<Integer> frequencies = new HashSet<>(gates.keySet());
        frequencies.addAll(items.keySet());
        frequencies.addAll(tanks.keySet());
        for (int frequency : frequencies) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("frequency", frequency);
            ListTag positions = new ListTag();
            for (GlobalPos pos : gates(frequency)) {
                GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).result().ifPresent(positions::add);
            }
            entry.put("gates", positions);
            ItemStack stack = items.getOrDefault(frequency, ItemStack.EMPTY);
            if (!stack.isEmpty()) {
                entry.put("item", stack.save(registries));
            }
            FluidTank tank = tanks.get(frequency);
            if (tank != null && !tank.isEmpty()) {
                entry.put("fluid", tank.getFluid().save(registries));
            }
            list.add(entry);
        }
        tag.put("frequencies", list);
        return tag;
    }

    private static QuantumGateData load(CompoundTag tag, HolderLookup.Provider registries) {
        QuantumGateData data = new QuantumGateData();
        for (Tag t : tag.getList("frequencies", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            int frequency = entry.getInt("frequency");
            for (Tag p : entry.getList("gates", Tag.TAG_COMPOUND)) {
                GlobalPos.CODEC.parse(NbtOps.INSTANCE, p).result().ifPresent(pos -> data.gates.computeIfAbsent(frequency, k -> new HashSet<>()).add(pos));
            }
            if (entry.contains("item")) {
                data.items.put(frequency, ItemStack.parseOptional(registries, entry.getCompound("item")));
            }
            if (entry.contains("fluid")) {
                FluidTank tank = data.newTank();
                tank.setFluid(FluidStack.parseOptional(registries, entry.getCompound("fluid")));
                data.tanks.put(frequency, tank);
            }
        }
        return data;
    }
}
