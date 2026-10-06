package resonantinduction.archaic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import resonantinduction.registry.RIRegistries;
import resonantinduction.resource.MaterialFluid;
import resonantinduction.resource.Materials;
import resonantinduction.resource.PoolBlock;

import java.util.Optional;

/**
 * Casting Mold: drinks a pool of molten metal sitting on it (or molten metal piped in) and casts it into ingots, 100 mB each, as
 * the original. Take the ingots out by clicking it, or with a hopper below.
 */
public class CastingMoldBlockEntity extends BlockEntity {
    public static final int PER_INGOT = 100;

    private final FluidTank tank = new FluidTank(1000, fs -> fs.getFluid() == RIRegistries.MOLTEN_METAL.get()) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final ItemStackHandler output = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            sync();
        }
    };

    public CastingMoldBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.CASTING_MOLD_BE.get(), pos, state);
    }

    public FluidTank tank() {
        return tank;
    }

    public ItemStackHandler output() {
        return output;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CastingMoldBlockEntity be) {
        be.update();
    }

    /** Drains a molten pool above if it all fits, then casts as many ingots as there's metal and room for. */
    void update() {
        BlockPos above = worldPosition.above();
        BlockState top = level.getBlockState(above);
        if (top.getBlock() instanceof PoolBlock pool && pool.kind() == PoolBlock.Kind.MOLTEN) {
            String material = PoolBlock.material(level, above);
            FluidStack drained = MaterialFluid.stack(RIRegistries.MOLTEN_METAL.get(), material, top.getValue(PoolBlock.LEVEL) * PoolBlock.MB_PER_LEVEL);
            if (Materials.firstIngot(material).isPresent() && tank.fill(drained, IFluidHandler.FluidAction.SIMULATE) == drained.getAmount()) {
                tank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                PoolBlock.lower(level, above, 8);
            }
        }
        while (tank.getFluidAmount() >= PER_INGOT) {
            Optional<ItemStack> ingot = Materials.firstIngot(MaterialFluid.material(tank.getFluid())).map(ItemStack::new);
            if (ingot.isEmpty() || !output.insertItem(0, ingot.get(), true).isEmpty()) {
                break;
            }
            output.insertItem(0, ingot.get(), false);
            tank.drain(PER_INGOT, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    @Nullable
    public IFluidHandler getFluidCapability(@Nullable Direction side) {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int t) {
                return tank.getFluid();
            }

            @Override
            public int getTankCapacity(int t) {
                return tank.getCapacity();
            }

            @Override
            public boolean isFluidValid(int t, FluidStack stack) {
                return tank.isFluidValid(stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return tank.fill(resource, action);
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return FluidStack.EMPTY;
            }
        };
    }

    /** Ingots come out; nothing goes in. */
    public IItemHandler getItemCapability(@Nullable Direction side) {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return 1;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return output.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return output.extractItem(slot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 64;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return false;
            }
        };
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
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.put("output", output.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("tank"));
        output.deserializeNBT(registries, tag.getCompound("output"));
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
