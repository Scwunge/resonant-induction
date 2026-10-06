package resonantinduction.atomic.machine;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/** A machine's inventory as hoppers see it: some slots take items in, others give products out. */
public class SidedSlots implements IItemHandler {
    private final ItemStackHandler inventory;
    private final int[] slots;
    private final boolean[] insert;

    public SidedSlots(ItemStackHandler inventory, int[] in, int[] out) {
        this.inventory = inventory;
        this.slots = new int[in.length + out.length];
        this.insert = new boolean[slots.length];
        for (int i = 0; i < in.length; i++) {
            slots[i] = in[i];
            insert[i] = true;
        }
        System.arraycopy(out, 0, slots, in.length, out.length);
    }

    @Override
    public int getSlots() {
        return slots.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inventory.getStackInSlot(slots[slot]);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return insert[slot] ? inventory.insertItem(slots[slot], stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return insert[slot] ? ItemStack.EMPTY : inventory.extractItem(slots[slot], amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return inventory.getSlotLimit(slots[slot]);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return insert[slot] && inventory.isItemValid(slots[slot], stack);
    }
}
