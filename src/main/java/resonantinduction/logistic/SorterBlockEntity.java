package resonantinduction.logistic;

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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import resonantinduction.archaic.ImprintItem;
import resonantinduction.registry.RIRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Sorter, as the original: each of its six faces can hold an Imprint. An item that falls onto it (or is put in by a hopper or
 * pipe) leaves through a face whose imprint lists it (inverted: one whose imprint doesn't), into an inventory there or dropped;
 * if no face wants it, through any open face, preferring inventories.
 */
public class SorterBlockEntity extends BlockEntity {
    private final ItemStackHandler imprints = new ItemStackHandler(6) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof ImprintItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            changed();
        }
    };
    private boolean inverted;

    public SorterBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.SORTER_BE.get(), pos, state);
    }

    public ItemStackHandler imprints() {
        return imprints;
    }

    public boolean inverted() {
        return inverted;
    }

    public void toggleInverted() {
        inverted = !inverted;
        changed();
    }

    private void changed() {
        setChanged();
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        BlockState shown = state.setValue(SorterBlock.INVERTED, inverted);
        for (Direction d : Direction.values()) {
            shown = shown.setValue(SorterBlock.SIDES.get(d), !imprints.getStackInSlot(d.get3DDataValue()).isEmpty());
        }
        if (shown != state) {
            level.setBlock(worldPosition, shown, 3);
        } else {
            level.sendBlockUpdated(worldPosition, state, state, 2);
        }
    }

    private boolean open(Direction d) {
        BlockPos p = worldPosition.relative(d);
        return !level.getBlockState(p).isRedstoneConductor(level, p);
    }

    /** Sends {@code stack} out of the right face. */
    public void sort(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        List<Direction> possible = new ArrayList<>();
        for (Direction d : Direction.values()) {
            if (!inverted == ImprintItem.isFiltering(imprints.getStackInSlot(d.get3DDataValue()), stack) && open(d)) {
                possible.add(d);
            }
        }
        if (possible.isEmpty()) {
            List<Direction> inventories = new ArrayList<>();
            for (Direction d : Direction.values()) {
                if (open(d)) {
                    possible.add(d);
                }
                if (level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(d), d.getOpposite()) != null) {
                    inventories.add(d);
                }
            }
            if (!inventories.isEmpty()) {
                possible = inventories;
            }
        }
        Direction out = possible.isEmpty() ? Direction.UP : possible.get(level.random.nextInt(possible.size()));
        BlockPos to = worldPosition.relative(out);
        ItemStack left = ItemTransfer.store(level, to, out.getOpposite(), stack);
        ItemTransfer.drop(level, to, left);
    }

    /** Anything put in from any side is sorted at once. */
    public IItemHandler getItemCapability(@Nullable Direction side) {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return 1;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return ItemStack.EMPTY;
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (!simulate) {
                    sort(stack.copy());
                }
                return ItemStack.EMPTY;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("imprints", imprints.serializeNBT(registries));
        tag.putBoolean("isInverted", inverted);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        imprints.deserializeNBT(registries, tag.getCompound("imprints"));
        inverted = tag.getBoolean("isInverted");
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
