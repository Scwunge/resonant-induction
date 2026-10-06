package resonantinduction.resource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import resonantinduction.registry.RIRegistries;

/** Remembers which metal a dust pile, molten pool or dust mixture is made of (the original TileMaterial). */
public class MaterialBlockEntity extends BlockEntity {
    private String material = "";

    public MaterialBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.MATERIAL_BE.get(), pos, state);
    }

    public String material() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material == null ? "" : material;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("material", material);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        material = tag.getString("material");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        super.onDataPacket(net, pkt, registries);
        if (level != null) {
            // The colour is a tint: redraw the chunk.
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 8);
        }
    }
}
