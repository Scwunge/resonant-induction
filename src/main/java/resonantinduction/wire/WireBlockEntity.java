package resonantinduction.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;
import resonantinduction.registry.RIRegistries;

/**
 * Per-wire settings: insulation (with its colour) and the lever that turns it into a switch wire. The metal comes from the block.
 * Insulated wires only join wires of the same colour, or white (the default) ones; bare wires join any wire of their metal.
 */
public class WireBlockEntity extends BlockEntity {
    public static final DyeColor DEFAULT_COLOR = DyeColor.WHITE;

    private boolean insulated;
    private DyeColor color = DEFAULT_COLOR;
    private boolean switched;

    public WireBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.WIRE_BE.get(), pos, state);
    }

    public WireMaterial material() {
        return getBlockState().getBlock() instanceof WireBlock wire ? wire.material() : WireMaterial.COPPER;
    }

    public boolean isInsulated() {
        return insulated;
    }

    public DyeColor color() {
        return color;
    }

    public boolean isSwitched() {
        return switched;
    }

    public void setInsulation(boolean insulated, DyeColor color) {
        this.insulated = insulated;
        this.color = insulated ? color : DEFAULT_COLOR;
        changed();
    }

    public void setSwitched(boolean switched) {
        this.switched = switched;
        changed();
    }

    /** Switch wires only conduct while powered by redstone. */
    public boolean conducts() {
        return !switched || (level != null && level.hasNeighborSignal(worldPosition));
    }

    public boolean compatibleWith(WireBlockEntity other) {
        if (material() != other.material()) {
            return false;
        }
        if (insulated && other.insulated) {
            return color == other.color || color == DEFAULT_COLOR || other.color == DEFAULT_COLOR;
        }
        return true;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            WireBlock.refresh(level, worldPosition);
            for (Direction d : Direction.values()) {
                WireBlock.refresh(level, worldPosition.relative(d));
            }
        }
    }

    @Nullable
    public IEnergyStorage getEnergyCapability(@Nullable Direction side) {
        if (side == null || level == null || !getBlockState().getValue(WireBlock.SIDES.get(side)) || !conducts()) {
            return null;
        }
        BlockPos source = worldPosition.relative(side);
        return new IEnergyStorage() {
            @Override
            public int receiveEnergy(int max, boolean simulate) {
                return WireNetwork.get(level, worldPosition).distribute(level, source, max, simulate);
            }

            @Override
            public int extractEnergy(int max, boolean simulate) {
                return 0;
            }

            @Override
            public int getEnergyStored() {
                return 0;
            }

            @Override
            public int getMaxEnergyStored() {
                return WireNetwork.get(level, worldPosition).capacity();
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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("insulated", insulated);
        tag.putString("color", color.getSerializedName());
        tag.putBoolean("switched", switched);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        insulated = tag.getBoolean("insulated");
        color = DyeColor.byName(tag.getString("color"), DEFAULT_COLOR);
        switched = tag.getBoolean("switched");
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
        // Insulation colour is a tint: redraw the chunk.
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 8);
        }
    }
}
