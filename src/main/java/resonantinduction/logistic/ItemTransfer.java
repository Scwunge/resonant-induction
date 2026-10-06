package resonantinduction.logistic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.function.Predicate;

/** Moving items in and out of the inventories next to a machine, and dropping them in the world. */
public final class ItemTransfer {
    private ItemTransfer() {}

    /** Puts {@code stack} into the inventory at {@code pos} (entered from the side facing {@code from}); returns what's left. */
    public static ItemStack store(Level level, BlockPos pos, Direction from, ItemStack stack) {
        IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, from);
        return h == null ? stack : ItemHandlerHelper.insertItemStacked(h, stack, false);
    }

    /** Takes up to {@code amount} of the first item {@code allowed} accepts from the inventory at {@code pos}. */
    public static ItemStack grab(Level level, BlockPos pos, Direction from, int amount, Predicate<ItemStack> allowed) {
        IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, from);
        if (h == null) {
            return ItemStack.EMPTY;
        }
        for (int slot = 0; slot < h.getSlots(); slot++) {
            ItemStack in = h.getStackInSlot(slot);
            if (!in.isEmpty() && allowed.test(in)) {
                ItemStack got = h.extractItem(slot, amount, false);
                if (!got.isEmpty()) {
                    return got;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    /** Drops {@code stack} in the middle of the block at {@code pos}, without throwing it about. */
    public static void drop(Level level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemEntity item = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, stack);
        item.setDeltaMovement(0, 0, 0);
        level.addFreshEntity(item);
    }
}
