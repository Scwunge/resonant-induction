package resonantinduction.archaic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import resonantinduction.RIConfig;
import resonantinduction.registry.RIRegistries;
import resonantinduction.resource.DustPileBlock;
import resonantinduction.resource.MaterialBlockEntity;
import resonantinduction.resource.PoolBlock;

/**
 * Firebox, as the original: burns fuel items (or a bucket of lava, or, the electric one, power) and heats the block above it.
 * Over it a dust pile melts into a pool of molten metal (refined dust gives twice as much), water boils away, and a Hot Plate
 * cooks. A lit firebox keeps fire burning on top when there's room.
 */
public class FireboxBlockEntity extends BlockEntity {
    /** Heat per second, as the original's POWER (watts). */
    private static final long POWER = 100000;

    private final ItemStackHandler fuel = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getBurnTime(null) > 0;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final FluidTank lava = new FluidTank(1000, fs -> fs.getFluid().isSame(Fluids.LAVA)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private int energy;
    private int burnTime;
    private long heat;
    private int boiled;

    public FireboxBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.FIREBOX_BE.get(), pos, state);
    }

    public boolean electric() {
        return ((FireboxBlock) getBlockState().getBlock()).electric();
    }

    public boolean isBurning() {
        return burnTime > 0;
    }

    public ItemStackHandler fuel() {
        return fuel;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FireboxBlockEntity be) {
        be.tick((ServerLevel) level);
    }

    private void tick(ServerLevel level) {
        boolean wasBurning = burnTime > 0;
        if (lava.getFluidAmount() >= 1000 && burnTime == 0) {
            lava.drain(1000, IFluidHandler.FluidAction.EXECUTE);
            burnTime += 20000;
        } else if (electric() && energy >= RIConfig.get(RIConfig.ELECTRIC_FIREBOX_USE)) {
            energy -= RIConfig.get(RIConfig.ELECTRIC_FIREBOX_USE);
            burnTime += 2;
        } else if (burnTime == 0 && !fuel.getStackInSlot(0).isEmpty()) {
            ItemStack stack = fuel.getStackInSlot(0);
            int time = stack.getBurnTime(null);
            if (time > 0) {
                burnTime += time;
                ItemStack remainder = stack.getCraftingRemainingItem();
                fuel.extractItem(0, 1, false);
                if (!remainder.isEmpty() && fuel.getStackInSlot(0).isEmpty()) {
                    fuel.setStackInSlot(0, remainder);
                }
            }
        }

        BlockPos above = worldPosition.above();
        BlockState top = level.getBlockState(above);
        if (burnTime > 0) {
            if (top.isAir()) {
                level.setBlockAndUpdate(above, BaseFireBlock.getState(level, above));
            }
            heat += POWER / 20;
            boolean used = false;
            if (top.getBlock() instanceof DustPileBlock pile && level.getBlockEntity(above) instanceof MaterialBlockEntity be) {
                used = true;
                int layers = top.getValue(DustPileBlock.LAYERS);
                if (heat >= Thermal.meltEnergy(level, worldPosition, layers / 8f * 1000)) {
                    int volume = pile.refined() ? layers : Math.max(1, layers / 2);
                    String material = be.material();
                    PoolBlock.place(level, above, PoolBlock.Kind.MOLTEN, material, volume);
                    heat = 0;
                }
            } else if (top.getFluidState().is(Fluids.WATER) && top.getFluidState().isSource()) {
                used = true;
                if (heat >= Thermal.boilEnergy(level, worldPosition, 100)) {
                    boiled += 100;
                    heat = 0;
                    if (boiled >= 1000) {
                        boiled = 0;
                        level.setBlockAndUpdate(above, Blocks.AIR.defaultBlockState());
                    }
                }
            } else if (level.getBlockEntity(above) instanceof HotPlateBlockEntity) {
                used = true;
            }
            if (!used) {
                heat = 0;
            }
            if (--burnTime == 0 && top.is(Blocks.FIRE)) {
                level.setBlockAndUpdate(above, Blocks.AIR.defaultBlockState());
            }
            setChanged();
        }
        if (wasBurning != (burnTime > 0)) {
            level.setBlock(worldPosition, getBlockState().setValue(FireboxBlock.LIT, burnTime > 0), 3);
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
            public FluidStack getFluidInTank(int tank) {
                return lava.getFluid();
            }

            @Override
            public int getTankCapacity(int tank) {
                return lava.getCapacity();
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return lava.isFluidValid(stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return lava.fill(resource, action);
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

    @Nullable
    public IEnergyStorage getEnergyCapability(@Nullable Direction side) {
        if (!electric() || side == Direction.UP) {
            return null;
        }
        return new IEnergyStorage() {
            private int capacity() {
                return RIConfig.get(RIConfig.ELECTRIC_FIREBOX_USE) * 10;
            }

            @Override
            public int receiveEnergy(int max, boolean simulate) {
                int accepted = Math.max(0, Math.min(max, capacity() - energy));
                if (!simulate && accepted > 0) {
                    energy += accepted;
                    setChanged();
                }
                return accepted;
            }

            @Override
            public int extractEnergy(int max, boolean simulate) {
                return 0;
            }

            @Override
            public int getEnergyStored() {
                return energy;
            }

            @Override
            public int getMaxEnergyStored() {
                return capacity();
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

    public ItemStackHandler getItemCapability(@Nullable Direction side) {
        return fuel;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("fuel", fuel.serializeNBT(registries));
        tag.put("lava", lava.writeToNBT(registries, new CompoundTag()));
        tag.putInt("energy", energy);
        tag.putInt("burnTime", burnTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuel.deserializeNBT(registries, tag.getCompound("fuel"));
        lava.readFromNBT(registries, tag.getCompound("lava"));
        energy = tag.getInt("energy");
        burnTime = tag.getInt("burnTime");
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
