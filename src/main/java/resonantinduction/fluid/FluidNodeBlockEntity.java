package resonantinduction.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;
import resonantinduction.api.Probeable;

import java.util.EnumMap;
import java.util.Map;

/**
 * A block with a {@link FluidNode} and its tank. Connections are rebuilt when a neighbour changes (and every two seconds); the
 * fluid is synced to clients for rendering when it changes noticeably.
 */
public abstract class FluidNodeBlockEntity extends BlockEntity implements FluidNode.Host, FluidNodeProvider, Probeable {
    protected final FluidTank tank;
    protected final FluidNode node;
    private final Map<Direction, BlockCapabilityCache<IFluidHandler, Direction>> caches = new EnumMap<>(Direction.class);
    private boolean recache = true;
    private int ticks;
    private FluidStack synced = FluidStack.EMPTY;
    private int connectionMask = -1;

    protected FluidNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state);
        this.tank = new FluidTank(capacity) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
        this.node = createNode();
    }

    protected FluidNode createNode() {
        return new FluidNode(this);
    }

    public FluidNode node() {
        return node;
    }

    @Override
    @Nullable
    public FluidNode getFluidNode(@Nullable Direction from) {
        return node;
    }

    @Override
    public FluidTank pressureTank() {
        return tank;
    }

    public FluidTank tank() {
        return tank;
    }

    @Override
    public void onFluidChanged() {
        setChanged();
    }

    public void markRecache() {
        recache = true;
    }

    /** Bit {@code d.ordinal()} set for each side with a connection, as last synced (for rendering). */
    public int connectionMask() {
        int mask = 0;
        for (Direction d : node.connections().keySet()) {
            mask |= 1 << d.ordinal();
        }
        return level != null && level.isClientSide ? Math.max(connectionMask, 0) : mask;
    }

    protected void recacheConnections(ServerLevel level) {
        node.connections().clear();
        for (Direction dir : Direction.values()) {
            BlockPos pos = worldPosition.relative(dir);
            if (!level.isLoaded(pos)) {
                continue;
            }
            if (level.getBlockEntity(pos) instanceof FluidNodeProvider provider) {
                FluidNode other = provider.getFluidNode(dir.getOpposite());
                if (other != null && other != node && node.canConnect(dir, other) && other.canConnect(dir.getOpposite(), node)) {
                    node.connections().put(dir, other);
                }
                continue;
            }
            IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, dir.getOpposite());
            if (handler != null && node.canConnect(dir, handler)) {
                node.connections().put(dir, caches.computeIfAbsent(dir,
                        d -> BlockCapabilityCache.create(Capabilities.FluidHandler.BLOCK, level, pos, d.getOpposite())));
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FluidNodeBlockEntity be) {
        be.tickServer((ServerLevel) level);
    }

    protected void tickServer(ServerLevel level) {
        ticks++;
        if (recache || ticks % 40 == 0) {
            recache = false;
            recacheConnections(level);
        }
        node.update();
        int mask = connectionMask();
        FluidStack now = tank.getFluid();
        boolean typeChanged = !FluidStack.isSameFluidSameComponents(now, synced);
        int delta = Math.abs(now.getAmount() - synced.getAmount());
        if (mask != connectionMask || typeChanged || delta > tank.getCapacity() / 20 || (delta > 0 && ticks % 20 == 0)) {
            connectionMask = mask;
            synced = now.copy();
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    /** Whether fluid may go in (or come out) on {@code side}; null is any side. */
    protected boolean canFill(@Nullable Direction side, FluidStack stack) {
        return true;
    }

    protected boolean canDrain(@Nullable Direction side) {
        return true;
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
                return canFill(side, stack) && tank.isFluidValid(stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (resource.isEmpty() || !canFill(side, resource)) {
                    return 0;
                }
                int filled = tank.fill(resource, action);
                if (filled > 0 && action.execute()) {
                    onFluidChanged();
                }
                return filled;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                if (!canDrain(side) || !FluidStack.isSameFluidSameComponents(resource, tank.getFluid())) {
                    return FluidStack.EMPTY;
                }
                return drain(resource.getAmount(), action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                if (!canDrain(side)) {
                    return FluidStack.EMPTY;
                }
                FluidStack drained = tank.drain(maxDrain, action);
                if (!drained.isEmpty() && action.execute()) {
                    onFluidChanged();
                }
                return drained;
            }
        };
    }

    @Override
    public double probePressure(Direction side) {
        return node.getPressure(side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        node.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("tank"));
        node.load(tag);
        if (tag.contains("connections")) {
            connectionMask = tag.getInt("connections");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        node.save(tag);
        tag.putInt("connections", connectionMask());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
