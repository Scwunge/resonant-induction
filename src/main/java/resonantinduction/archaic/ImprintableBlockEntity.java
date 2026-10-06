package resonantinduction.archaic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import resonantinduction.registry.RIRegistries;

/**
 * A block that holds an Imprint (the original TileFilterable). It matches items on the imprint; inverted, it matches
 * everything else.
 */
public class ImprintableBlockEntity extends BlockEntity {
    private ItemStack imprint = ItemStack.EMPTY;
    private boolean inverted;

    public ImprintableBlockEntity(BlockPos pos, BlockState state) {
        this(RIRegistries.IMPRINTABLE_BE.get(), pos, state);
    }

    protected ImprintableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ItemStack imprint() {
        return imprint;
    }

    public void setImprint(ItemStack stack) {
        imprint = stack.copy();
        changed();
    }

    public boolean inverted() {
        return inverted;
    }

    public boolean toggleInverted() {
        inverted = !inverted;
        changed();
        return inverted;
    }

    public boolean isFiltering(ItemStack stack) {
        boolean listed = ImprintItem.isFiltering(imprint, stack);
        return inverted != listed;
    }

    protected void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!imprint.isEmpty()) {
            tag.put("imprint", imprint.save(registries));
        }
        tag.putBoolean("inverted", inverted);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        imprint = tag.contains("imprint") ? ItemStack.parseOptional(registries, tag.getCompound("imprint")) : ItemStack.EMPTY;
        inverted = tag.getBoolean("inverted");
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
