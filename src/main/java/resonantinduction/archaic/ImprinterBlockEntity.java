package resonantinduction.archaic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import org.jetbrains.annotations.Nullable;
import resonantinduction.registry.RIRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Imprinter, as the original: lay items on its 3x3 top, put an Imprint in its side, and bring a piston down on it. The stamp
 * toggles each item on the top in the imprint's list: added if it wasn't there, removed if it was.
 */
public class ImprinterBlockEntity extends BlockEntity {
    public static final int IMPRINT_SLOT = 9;

    private final ItemStackHandler inventory = new ItemStackHandler(10) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot != IMPRINT_SLOT || stack.getItem() instanceof ImprintItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == IMPRINT_SLOT ? 1 : 64;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
            }
        }
    };

    public ImprinterBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.IMPRINTER_BE.get(), pos, state);
    }

    public ItemStackHandler inventory() {
        return inventory;
    }

    /** The piston came down. */
    public void stamp() {
        ItemStack imprint = inventory.getStackInSlot(IMPRINT_SLOT);
        if (!(imprint.getItem() instanceof ImprintItem)) {
            return;
        }
        List<ItemStack> filters = new ArrayList<>(ImprintItem.filters(imprint));
        List<ItemStack> seen = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack s = inventory.getStackInSlot(i);
            if (s.isEmpty() || seen.stream().anyMatch(x -> ItemStack.isSameItem(x, s))) {
                continue;
            }
            seen.add(s);
            if (!filters.removeIf(f -> ItemStack.isSameItem(f, s))) {
                filters.add(s.copyWithCount(1));
            }
        }
        ItemStack out = imprint.copy();
        ImprintItem.setFilters(out, filters);
        inventory.setStackInSlot(IMPRINT_SLOT, out);
    }

    /** Hoppers: from above into the top squares; from the sides and below, the imprint. */
    public IItemHandler getItemCapability(@Nullable Direction side) {
        return side == Direction.UP ? new RangedWrapper(inventory, 0, 9) : new RangedWrapper(inventory, IMPRINT_SLOT, IMPRINT_SLOT + 1);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
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
