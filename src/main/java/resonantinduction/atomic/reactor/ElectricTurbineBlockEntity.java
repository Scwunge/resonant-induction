package resonantinduction.atomic.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;
import resonantinduction.RIConfig;
import resonantinduction.registry.RIRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Electric Turbine, as the original: steam rising into it from below turns it, and it sends the power (FE) to whatever is beside
 * it. One turbine makes up to 5 MW (250,000 FE a tick); a wrench joins a 3x3 of them into one big turbine that makes nine times as
 * much. How much power a bucket of steam makes is set in the config.
 */
public class ElectricTurbineBlockEntity extends BlockEntity {
    public static final long MAX_POWER_PER_TICK = 5_000_000 / 20;
    private static final long TORQUE = 5000L * 500;

    private final FluidTank tank = new FluidTank(1000, fs -> fs.getFluid().isSame(RIRegistries.STEAM.get()));
    /** The centre of the big turbine this is part of, or null. */
    @Nullable
    private BlockPos primary;
    private boolean formed;
    private long produced;
    private long syncedProduced = -1;
    public float angle;
    public float prevAngle;

    public ElectricTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.ELECTRIC_TURBINE_BE.get(), pos, state);
    }

    public boolean formed() {
        return formed;
    }

    public boolean isMember() {
        return primary != null && !primary.equals(worldPosition);
    }

    public int area() {
        return formed ? 9 : 1;
    }

    public long produced() {
        return produced;
    }

    public FluidTank tank() {
        return tank;
    }

    /** The turbine that does the work for this one: itself, or the centre of its big turbine. */
    public ElectricTurbineBlockEntity master() {
        if (isMember() && level.getBlockEntity(primary) instanceof ElectricTurbineBlockEntity p && p.formed) {
            return p;
        }
        return this;
    }

    private List<BlockPos> square(BlockPos centre) {
        List<BlockPos> out = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                out.add(centre.offset(x, 0, z));
            }
        }
        return out;
    }

    /** Wrench: split the big turbine this is part of, or join the 3x3 around this one. Returns whether anything changed. */
    public boolean toggleMultiblock() {
        ElectricTurbineBlockEntity master = master();
        if (master.formed) {
            for (BlockPos p : square(master.worldPosition)) {
                if (level.getBlockEntity(p) instanceof ElectricTurbineBlockEntity t) {
                    t.primary = null;
                    t.formed = false;
                    t.tank.setCapacity(1000);
                    t.sync();
                }
            }
            return true;
        }
        if (!RIConfig.get(RIConfig.TURBINE_STACKING)) {
            return false;
        }
        for (BlockPos p : square(worldPosition)) {
            if (!(level.getBlockEntity(p) instanceof ElectricTurbineBlockEntity t) || t.master().formed) {
                return false;
            }
        }
        for (BlockPos p : square(worldPosition)) {
            ElectricTurbineBlockEntity t = (ElectricTurbineBlockEntity) level.getBlockEntity(p);
            t.primary = worldPosition;
            t.formed = p.equals(worldPosition);
            t.sync();
        }
        tank.setCapacity(9000);
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ElectricTurbineBlockEntity be) {
        be.tickServer((ServerLevel) level);
    }

    private void tickServer(ServerLevel level) {
        if (isMember()) {
            if (!(level.getBlockEntity(primary) instanceof ElectricTurbineBlockEntity p) || !p.formed) {
                primary = null;
                sync();
            }
            return;
        }
        long maxPower = MAX_POWER_PER_TICK * area();
        double perMb = RIConfig.get(RIConfig.STEAM_ENERGY) * RIConfig.get(RIConfig.TURBINE_MULTIPLIER);
        int used = (int) Math.min(tank.getFluidAmount(), perMb <= 0 ? 0 : Math.ceil(maxPower / perMb));
        tank.drain(used, IFluidHandler.FluidAction.EXECUTE);
        produced = Math.min(maxPower, (long) (used * perMb));
        long left = produced;
        for (BlockPos p : formed ? square(worldPosition) : List.of(worldPosition)) {
            for (Direction d : Direction.values()) {
                if (left <= 0) {
                    break;
                }
                BlockPos n = p.relative(d);
                if (level.getBlockEntity(n) instanceof ElectricTurbineBlockEntity) {
                    continue;
                }
                IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, n, d.getOpposite());
                if (target != null && target.canReceive()) {
                    left -= target.receiveEnergy((int) Math.min(Integer.MAX_VALUE, left), false);
                }
            }
        }
        if (produced > 0 && level.getGameTime() % 26 == 0) {
            level.playSound(null, worldPosition, RIRegistries.TURBINE_SOUND.get(), SoundSource.BLOCKS, Math.min(1f, (float) produced / maxPower), 1f);
        }
        if (Math.abs(produced - syncedProduced) > maxPower / 50 || (produced == 0) != (syncedProduced == 0)) {
            syncedProduced = produced;
            sync();
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, ElectricTurbineBlockEntity be) {
        be.prevAngle = be.angle;
        // The original's angular velocity, power over torque, as a turn a tick.
        be.angle += (float) (be.produced * 20.0 / (TORQUE * be.area()) / 20.0);
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    /** Steam goes in from below, into the big turbine's centre if it's part of one. */
    @Nullable
    public IFluidHandler getFluidCapability(@Nullable Direction side) {
        if (side != null && side != Direction.DOWN) {
            return null;
        }
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int t) {
                return master().tank.getFluid();
            }

            @Override
            public int getTankCapacity(int t) {
                return master().tank.getCapacity();
            }

            @Override
            public boolean isFluidValid(int t, FluidStack stack) {
                return master().tank.isFluidValid(stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                ElectricTurbineBlockEntity m = master();
                int filled = m.tank.fill(resource, action);
                if (filled > 0 && action.execute()) {
                    m.setChanged();
                }
                return filled;
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

    public AABB renderBox() {
        return new AABB(worldPosition).inflate(formed ? 1.5 : 0.5);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putBoolean("formed", formed);
        tag.putLong("produced", produced);
        if (primary != null) {
            tag.put("primary", NbtUtils.writeBlockPos(primary));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        formed = tag.getBoolean("formed");
        tank.setCapacity(formed ? 9000 : 1000);
        tank.readFromNBT(registries, tag.getCompound("tank"));
        produced = tag.getLong("produced");
        primary = NbtUtils.readBlockPos(tag, "primary").orElse(null);
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
