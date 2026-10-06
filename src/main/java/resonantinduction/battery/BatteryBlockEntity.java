package resonantinduction.battery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;
import resonantinduction.RIConfig;
import resonantinduction.registry.RIRegistries;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * One battery cell. Touching batteries (any tier) share their energy, rebalanced by capacity every five ticks as in the
 * original. Each face is set to input, output or nothing (wrench, or sneak-use with an empty hand); outputs push energy out.
 */
public class BatteryBlockEntity extends BlockEntity {
    public static final byte IO_NONE = 0;
    public static final byte IO_INPUT = 1;
    public static final byte IO_OUTPUT = 2;

    private int energy;
    private final byte[] io = {IO_INPUT, IO_INPUT, IO_INPUT, IO_INPUT, IO_INPUT, IO_INPUT};
    private boolean redistribute = true;
    private int ticks;

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.BATTERY_BE.get(), pos, state);
    }

    public static int capacityForTier(int tier) {
        return switch (tier) {
            case 0 -> RIConfig.get(RIConfig.BATTERY_TIER1);
            case 1 -> RIConfig.get(RIConfig.BATTERY_TIER2);
            default -> RIConfig.get(RIConfig.BATTERY_TIER3);
        };
    }

    public int tier() {
        return getBlockState().getValue(BatteryBlock.TIER);
    }

    public int capacity() {
        return capacityForTier(tier());
    }

    public int energy() {
        return energy;
    }

    public void setEnergy(int energy) {
        this.energy = Math.max(0, Math.min(energy, capacity()));
        redistribute = true;
        setChanged();
    }

    public byte io(Direction side) {
        return io[side.ordinal()];
    }

    /** Cycles a face input, output, none. Returns the new mode. */
    public byte cycleIo(Direction side) {
        io[side.ordinal()] = (byte) ((io[side.ordinal()] + 1) % 3);
        setChanged();
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
        return io[side.ordinal()];
    }

    @Nullable
    public IEnergyStorage getEnergyCapability(@Nullable Direction side) {
        if (side == null) {
            return storage(true, true);
        }
        return switch (io(side)) {
            case IO_INPUT -> storage(true, false);
            case IO_OUTPUT -> storage(false, true);
            default -> null;
        };
    }

    private IEnergyStorage storage(boolean in, boolean out) {
        return new IEnergyStorage() {
            @Override
            public int receiveEnergy(int max, boolean simulate) {
                if (!in) {
                    return 0;
                }
                int accepted = Math.max(0, Math.min(max, capacity() - energy));
                if (!simulate && accepted > 0) {
                    energy += accepted;
                    redistribute = true;
                    setChanged();
                }
                return accepted;
            }

            @Override
            public int extractEnergy(int max, boolean simulate) {
                if (!out) {
                    return 0;
                }
                int extracted = Math.max(0, Math.min(max, energy));
                if (!simulate && extracted > 0) {
                    energy -= extracted;
                    redistribute = true;
                    setChanged();
                }
                return extracted;
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
                return out;
            }

            @Override
            public boolean canReceive() {
                return in;
            }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BatteryBlockEntity be) {
        be.tick((ServerLevel) level);
    }

    private void tick(ServerLevel level) {
        ticks++;
        for (Direction side : Direction.values()) {
            if (energy <= 0) {
                break;
            }
            if (io(side) != IO_OUTPUT) {
                continue;
            }
            BlockPos target = worldPosition.relative(side);
            if (level.getBlockEntity(target) instanceof BatteryBlockEntity) {
                continue;
            }
            IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, target, side.getOpposite());
            if (storage != null && storage.canReceive()) {
                int sent = storage.receiveEnergy(Math.min(energy, RIConfig.get(RIConfig.BATTERY_MAX_OUTPUT)), false);
                if (sent > 0) {
                    energy -= sent;
                    redistribute = true;
                    setChanged();
                }
            }
        }
        if (redistribute && ticks % 5 == 0) {
            redistribute = false;
            redistribute(level);
        }
    }

    /** All batteries touching this one, this one included. */
    public List<BatteryBlockEntity> group() {
        List<BatteryBlockEntity> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        open.add(worldPosition);
        while (!open.isEmpty() && out.size() < 4096) {
            BlockPos pos = open.poll();
            if (!seen.add(pos) || !level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof BatteryBlockEntity battery)) {
                continue;
            }
            out.add(battery);
            for (Direction d : Direction.values()) {
                open.add(pos.relative(d));
            }
        }
        return out;
    }

    /** Shares the group's energy out by capacity and updates every cell's charge display. */
    public void redistribute(Level level) {
        List<BatteryBlockEntity> group = group();
        long total = 0;
        long totalCapacity = 0;
        for (BatteryBlockEntity b : group) {
            total += b.energy;
            totalCapacity += b.capacity();
        }
        long remaining = total;
        for (int i = 0; i < group.size(); i++) {
            BatteryBlockEntity b = group.get(i);
            long share = i == group.size() - 1 ? remaining : Math.round((double) total * b.capacity() / Math.max(1, totalCapacity));
            share = Math.max(0, Math.min(share, Math.min(remaining, b.capacity())));
            b.energy = (int) share;
            remaining -= share;
            b.redistribute = false;
            b.setChanged();
            b.updateLevel();
        }
    }

    private void updateLevel() {
        int lit = (int) Math.round(8.0 * energy / Math.max(1, capacity()));
        BlockState state = getBlockState();
        if (state.getValue(BatteryBlock.LEVEL) != lit) {
            level.setBlock(worldPosition, state.setValue(BatteryBlock.LEVEL, lit), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", energy);
        tag.putByteArray("io", io);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy = tag.getInt("energy");
        byte[] saved = tag.getByteArray("io");
        if (saved.length == 6) {
            System.arraycopy(saved, 0, io, 0, 6);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putByteArray("io", io);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
