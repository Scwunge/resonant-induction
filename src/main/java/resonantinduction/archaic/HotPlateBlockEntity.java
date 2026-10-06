package resonantinduction.archaic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import resonantinduction.registry.RIRegistries;

/**
 * Hot Plate: four spots that smelt what's on them while a burning firebox is underneath, as the original. A spot takes 10
 * seconds per item in it and smelts the whole stack at once.
 */
public class HotPlateBlockEntity extends BlockEntity {
    public static final int SLOTS = 4;
    public static final int MAX_SMELT_TIME = 200;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return level == null || canSmelt(level, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            ItemStack stack = getStackInSlot(slot);
            if (stack.isEmpty()) {
                sizeCache[slot] = 0;
            } else if (sizeCache[slot] != stack.getCount()) {
                // More (or fewer) items on a spot that's already heating take longer (or less).
                if (smeltTime[slot] > 0) {
                    smeltTime[slot] = Math.max(1, smeltTime[slot] + (stack.getCount() - sizeCache[slot]) * MAX_SMELT_TIME);
                }
                sizeCache[slot] = stack.getCount();
            }
            sync();
        }
    };
    private final int[] smeltTime = new int[SLOTS];
    private final int[] sizeCache = new int[SLOTS];

    public HotPlateBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.HOT_PLATE_BE.get(), pos, state);
    }

    public ItemStackHandler inventory() {
        return inventory;
    }

    public int smeltTime(int slot) {
        return smeltTime[slot];
    }

    public static boolean canSmelt(Level level, ItemStack stack) {
        return !result(level, stack).isEmpty();
    }

    private static ItemStack result(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level)
                .map(r -> r.value().getResultItem(level.registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    public boolean canRun() {
        return level.getBlockEntity(worldPosition.below()) instanceof FireboxBlockEntity firebox && firebox.isBurning();
    }

    public boolean isSmelting() {
        for (int t : smeltTime) {
            if (t > 0) {
                return true;
            }
        }
        return false;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HotPlateBlockEntity be) {
        if (!be.canRun()) {
            return;
        }
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = be.inventory.getStackInSlot(i);
            if (canSmelt(level, stack)) {
                if (be.smeltTime[i] <= 0) {
                    be.sizeCache[i] = stack.getCount();
                    be.smeltTime[i] = MAX_SMELT_TIME * be.sizeCache[i];
                    be.sync();
                } else if (--be.smeltTime[i] == 0) {
                    ItemStack out = result(level, stack).copyWithCount(be.sizeCache[i]);
                    be.inventory.setStackInSlot(i, out);
                }
                be.setChanged();
            } else {
                be.smeltTime[i] = 0;
            }
        }
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.putIntArray("smeltTime", smeltTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        int[] times = tag.getIntArray("smeltTime");
        for (int i = 0; i < SLOTS; i++) {
            smeltTime[i] = i < times.length ? times[i] : 0;
            sizeCache[i] = inventory.getStackInSlot(i).getCount();
        }
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
