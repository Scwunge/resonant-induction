package resonantinduction.charger;

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
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import resonantinduction.registry.RIRegistries;

/**
 * Charger pad: holds one chargeable item and passes the energy it is given straight into it (it has no buffer of its own,
 * like the original). Takes power from every side but its face.
 */
public class ChargerBlockEntity extends BlockEntity {
    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isChargeable(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            sync();
        }
    };

    private final IEnergyStorage energy = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int max, boolean simulate) {
            IEnergyStorage item = itemEnergy();
            if (item == null) {
                return 0;
            }
            int received = item.receiveEnergy(max, simulate);
            if (!simulate && received > 0) {
                setChanged();
                // Throttle client updates of the charge readout.
                if (level != null && level.getGameTime() % 10 == 0) {
                    sync();
                }
            }
            return received;
        }

        @Override
        public int extractEnergy(int max, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            IEnergyStorage item = itemEnergy();
            return item == null ? 0 : item.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            IEnergyStorage item = itemEnergy();
            return item == null ? 0 : item.getMaxEnergyStored();
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

    public ChargerBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.CHARGER_BE.get(), pos, state);
    }

    public static boolean isChargeable(ItemStack stack) {
        IEnergyStorage storage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return storage != null && storage.canReceive();
    }

    @Nullable
    private IEnergyStorage itemEnergy() {
        ItemStack stack = inventory.getStackInSlot(0);
        return stack.isEmpty() ? null : stack.getCapability(Capabilities.EnergyStorage.ITEM);
    }

    public ItemStack getItem() {
        return inventory.getStackInSlot(0);
    }

    public void setItem(ItemStack stack) {
        inventory.setStackInSlot(0, stack);
    }

    @Nullable
    public IEnergyStorage getEnergyCapability(@Nullable Direction side) {
        return side == getBlockState().getValue(ChargerBlock.FACING) ? null : energy;
    }

    public IItemHandler getItemCapability(@Nullable Direction side) {
        return inventory;
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
