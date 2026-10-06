package resonantinduction.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;
import resonantinduction.registry.RIRegistries;

/** Tank: sixteen buckets, shared with every tank it touches (see {@link TankNetwork}). Keeps its fluid when picked up. */
public class TankBlockEntity extends BlockEntity {
    public static final int CAPACITY = 16 * 1000;

    private final FluidTank tank = new FluidTank(CAPACITY);
    private int lightLevel;

    public TankBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.TANK_BE.get(), pos, state);
    }

    public FluidTank tank() {
        return tank;
    }

    public TankNetwork network() {
        return TankNetwork.get(level, worldPosition);
    }

    /** Sets this tank's share of the network's fluid, syncing it if it changed. */
    void setContents(FluidStack fluid) {
        if (FluidStack.matches(fluid, tank.getFluid())) {
            return;
        }
        tank.setFluid(fluid);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            updateLight();
        }
    }

    private void updateLight() {
        int light = tank.isEmpty() ? 0 : tank.getFluid().getFluidType().getLightLevel(tank.getFluid());
        if (light != lightLevel) {
            lightLevel = light;
            level.getLightEngine().checkBlock(worldPosition);
        }
    }

    public int lightLevel() {
        return lightLevel;
    }

    @Nullable
    public IFluidHandler getFluidCapability(@Nullable Direction side) {
        return level == null || level.isClientSide ? tank : network();
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        tank.setFluid(input.getOrDefault(RIRegistries.FLUID_CONTENT.get(), SimpleFluidContent.EMPTY).copy());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (!tank.isEmpty()) {
            builder.set(RIRegistries.FLUID_CONTENT.get(), SimpleFluidContent.copyOf(tank.getFluid()));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("tank");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("tank"));
        lightLevel = tank.isEmpty() ? 0 : tank.getFluid().getFluidType().getLightLevel(tank.getFluid());
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
