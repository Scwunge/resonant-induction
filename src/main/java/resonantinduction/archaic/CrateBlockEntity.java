package resonantinduction.archaic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import resonantinduction.registry.RIRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Crate, as the original: holds a great many of one kind of item (32, 64 or 256 stacks by tier). It can be locked to an item
 * (a filter), and with the ore filter on it takes anything that shares the held item's material tag (copper ingots from any
 * mod), turning it into the held kind.
 */
public class CrateBlockEntity extends BlockEntity {
    private ItemStack item = ItemStack.EMPTY;
    private int count;
    private ItemStack filter = ItemStack.EMPTY;
    private boolean oreFilter;
    long prevClickTime = -1000;

    public CrateBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.CRATE_BE.get(), pos, state);
    }

    public int tier() {
        return ((CrateBlock) getBlockState().getBlock()).tier();
    }

    public static int slots(int tier) {
        return tier >= 2 ? 256 : tier == 1 ? 64 : 32;
    }

    /** Items a crate holds: 64 to a slot whatever the item, as the original. */
    public int capacity() {
        return slots(tier()) * 64;
    }

    public int count() {
        return count;
    }

    public ItemStack item() {
        return item;
    }

    public ItemStack filter() {
        return filter;
    }

    public boolean oreFilter() {
        return oreFilter;
    }

    /** What the crate holds, or (when empty) what it's locked to; empty if neither. */
    public ItemStack sample() {
        return count > 0 ? item : filter;
    }

    /** The material tag ("c:ingots/copper") an item shares with its equivalents from other mods, if any. */
    public static Optional<TagKey<Item>> materialTag(ItemStack stack) {
        return stack.getItemHolder().tags().filter(t -> t.location().getNamespace().equals("c") && t.location().getPath().contains("/")).findFirst();
    }

    public boolean accepts(ItemStack stack) {
        if (stack.isEmpty() || stack.isDamaged()) {
            return false;
        }
        ItemStack sample = sample();
        if (sample.isEmpty() || ItemStack.isSameItemSameComponents(sample, stack)) {
            return true;
        }
        if (oreFilter) {
            Optional<TagKey<Item>> tag = materialTag(sample);
            return tag.isPresent() && stack.is(tag.get());
        }
        return false;
    }

    /** Puts in as much of {@code stack} as fits; returns what's left. */
    public ItemStack add(ItemStack stack, boolean simulate) {
        if (!accepts(stack)) {
            return stack;
        }
        int moved = Math.min(stack.getCount(), capacity() - count);
        if (moved <= 0) {
            return stack;
        }
        if (!simulate) {
            if (count == 0) {
                ItemStack sample = sample();
                item = (sample.isEmpty() ? stack : sample).copyWithCount(1);
            }
            count += moved;
            changed();
        }
        return stack.copyWithCount(stack.getCount() - moved);
    }

    /** Takes out up to {@code amount}. */
    public ItemStack take(int amount, boolean simulate) {
        int taken = Math.min(amount, count);
        if (taken <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack out = item.copyWithCount(taken);
        if (!simulate) {
            count -= taken;
            if (count == 0) {
                item = ItemStack.EMPTY;
            }
            changed();
        }
        return out;
    }

    public void setFilter(ItemStack stack) {
        filter = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        changed();
    }

    public void toggleOreFilter() {
        oreFilter = !oreFilter;
        changed();
    }

    /** Wrench: swap what's held for the next item sharing its material tag. */
    public boolean cycleVariant() {
        if (count == 0) {
            return false;
        }
        Optional<TagKey<Item>> tag = materialTag(item);
        if (tag.isEmpty()) {
            return false;
        }
        List<Item> items = new ArrayList<>();
        for (Holder<Item> h : BuiltInRegistries.ITEM.getTagOrEmpty(tag.get())) {
            items.add(h.value());
        }
        int at = items.indexOf(item.getItem());
        if (items.size() < 2 || at < 0) {
            return false;
        }
        item = new ItemStack(items.get((at + 1) % items.size()));
        changed();
        return true;
    }

    void setContents(ItemStack stack, int amount) {
        item = amount > 0 ? stack.copyWithCount(1) : ItemStack.EMPTY;
        count = Math.max(0, amount);
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    /** For hoppers and pipes: slot 0 hands out a stack at a time, slot 1 takes anything that fits. */
    public IItemHandler getItemCapability(@Nullable Direction side) {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return 2;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return slot == 0 && count > 0 ? item.copyWithCount(Math.min(count, item.getMaxStackSize())) : ItemStack.EMPTY;
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return slot == 1 ? add(stack, simulate) : stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return slot == 0 ? take(Math.min(amount, item.getMaxStackSize()), simulate) : ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 64;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot == 1 && accepts(stack);
            }
        };
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        CrateContents contents = input.get(RIRegistries.CRATE_CONTENTS.get());
        if (contents != null) {
            item = contents.count() > 0 ? contents.item().copyWithCount(1) : ItemStack.EMPTY;
            count = contents.count();
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (count > 0) {
            builder.set(RIRegistries.CRATE_CONTENTS.get(), new CrateContents(item.copyWithCount(1), count));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("item");
        tag.remove("count");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (count > 0) {
            tag.put("item", item.copyWithCount(1).save(registries));
            tag.putInt("count", count);
        }
        if (!filter.isEmpty()) {
            tag.put("filter", filter.save(registries));
        }
        tag.putBoolean("oreFilter", oreFilter);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        count = tag.getInt("count");
        item = count > 0 ? ItemStack.parseOptional(registries, tag.getCompound("item")) : ItemStack.EMPTY;
        if (item.isEmpty()) {
            count = 0;
        }
        filter = tag.contains("filter") ? ItemStack.parseOptional(registries, tag.getCompound("filter")) : ItemStack.EMPTY;
        oreFilter = tag.getBoolean("oreFilter");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
