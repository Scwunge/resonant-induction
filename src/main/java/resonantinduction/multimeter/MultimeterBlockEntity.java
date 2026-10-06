package resonantinduction.multimeter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;
import resonantinduction.api.Probeable;
import resonantinduction.registry.RIRegistries;
import resonantinduction.wire.WireBlockEntity;
import resonantinduction.wire.WireNetwork;

/**
 * Multimeter screen on the face of a block: reads the block behind it (energy, flow, capacity, fluid, and the mechanical and
 * thermal readings of blocks that offer them) and can emit redstone when a chosen reading passes a limit.
 */
public class MultimeterBlockEntity extends BlockEntity implements MenuProvider {
    public enum DetectMode {
        NONE, LESS_THAN, LESS_THAN_EQUAL, EQUAL, GREATER_THAN_EQUAL, GREATER_THAN;

        public boolean test(double value, double limit) {
            return switch (this) {
                case NONE -> false;
                case LESS_THAN -> value < limit;
                case LESS_THAN_EQUAL -> value <= limit;
                case EQUAL -> value == limit;
                case GREATER_THAN_EQUAL -> value >= limit;
                case GREATER_THAN -> value > limit;
            };
        }
    }

    private final double[] values = new double[Measure.values().length];
    private DetectMode detectMode = DetectMode.NONE;
    private Measure detectType = Measure.ENERGY;
    private Measure graphType = Measure.ENERGY;
    private double limit;
    private boolean detecting = true;
    private boolean redstoneOn;
    private int ticks;
    private int lastEnergy = -1;
    private final double[] powerHistory = new double[20];

    public MultimeterBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.MULTIMETER_BE.get(), pos, state);
    }

    public Direction facing() {
        return getBlockState().getValue(MultimeterBlock.FACING);
    }

    public double value(Measure m) {
        return values[m.ordinal()];
    }

    public DetectMode detectMode() {
        return detectMode;
    }

    public Measure detectType() {
        return detectType;
    }

    public Measure graphType() {
        return graphType;
    }

    public double limit() {
        return limit;
    }

    public boolean isDetecting() {
        return detecting;
    }

    public boolean redstoneOn() {
        return redstoneOn;
    }

    public void applySettings(DetectMode mode, Measure detect, Measure graph, double limit) {
        this.detectMode = mode;
        this.detectType = detect;
        this.graphType = graph;
        this.limit = limit;
        setChanged();
        sync();
    }

    public boolean toggleDetecting() {
        detecting = !detecting;
        setChanged();
        return detecting;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MultimeterBlockEntity be) {
        be.tick(level);
    }

    private void tick(Level level) {
        ticks++;
        if (detecting) {
            sample(level);
        }
        if (ticks % 20 == 0) {
            boolean on = detectMode.test(value(detectType), limit);
            if (on != redstoneOn) {
                redstoneOn = on;
                level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
                level.updateNeighborsAt(worldPosition.relative(facing().getOpposite()), getBlockState().getBlock());
            }
            sync();
        }
    }

    private void sample(Level level) {
        Direction side = facing();
        BlockPos target = worldPosition.relative(side.getOpposite());
        java.util.Arrays.fill(values, 0);
        if (level.getBlockEntity(target) instanceof WireBlockEntity) {
            WireNetwork network = WireNetwork.get(level, target);
            values[Measure.POWER.ordinal()] = network.transferredThisTick(level);
            values[Measure.CAPACITY.ordinal()] = network.capacity();
        } else {
            IEnergyStorage energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, target, side);
            if (energy == null) {
                energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, target, null);
            }
            if (energy != null) {
                int stored = energy.getEnergyStored();
                values[Measure.ENERGY.ordinal()] = stored;
                values[Measure.CAPACITY.ordinal()] = energy.getMaxEnergyStored();
                // Net flow, averaged over the last second.
                powerHistory[ticks % powerHistory.length] = lastEnergy < 0 ? 0 : stored - lastEnergy;
                lastEnergy = stored;
                double sum = 0;
                for (double d : powerHistory) {
                    sum += d;
                }
                values[Measure.POWER.ordinal()] = Math.round(sum / powerHistory.length * 10) / 10.0;
            } else {
                lastEnergy = -1;
            }
        }
        IFluidHandler fluids = level.getCapability(Capabilities.FluidHandler.BLOCK, target, side);
        if (fluids != null) {
            long amount = 0;
            for (int t = 0; t < fluids.getTanks(); t++) {
                amount += fluids.getFluidInTank(t).getAmount();
            }
            values[Measure.FLUID.ordinal()] = amount;
        }
        if (level.getBlockEntity(target) instanceof Probeable probe) {
            values[Measure.TORQUE.ordinal()] = probe.probeTorque(side);
            values[Measure.SPEED.ordinal()] = probe.probeAngularVelocity(side);
            values[Measure.TEMPERATURE.ordinal()] = probe.probeTemperature(side);
            values[Measure.PRESSURE.ordinal()] = probe.probePressure(side);
        }
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.resonantinduction.multimeter");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MultimeterMenu(id, inventory, worldPosition);
    }

    private void write(CompoundTag tag) {
        tag.putInt("detectMode", detectMode.ordinal());
        tag.putInt("detectType", detectType.ordinal());
        tag.putInt("graphType", graphType.ordinal());
        tag.putDouble("limit", limit);
        tag.putBoolean("detecting", detecting);
        tag.putBoolean("redstone", redstoneOn);
    }

    private void read(CompoundTag tag) {
        detectMode = DetectMode.values()[Math.floorMod(tag.getInt("detectMode"), DetectMode.values().length)];
        detectType = Measure.byIndex(tag.getInt("detectType"));
        graphType = Measure.byIndex(tag.getInt("graphType"));
        limit = tag.getDouble("limit");
        detecting = !tag.contains("detecting") || tag.getBoolean("detecting");
        redstoneOn = tag.getBoolean("redstone");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        write(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        read(tag);
        if (tag.contains("values")) {
            long[] saved = tag.getLongArray("values");
            for (int i = 0; i < Math.min(saved.length, values.length); i++) {
                values[i] = Double.longBitsToDouble(saved[i]);
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        write(tag);
        long[] bits = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            bits[i] = Double.doubleToLongBits(values[i]);
        }
        tag.putLongArray("values", bits);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
