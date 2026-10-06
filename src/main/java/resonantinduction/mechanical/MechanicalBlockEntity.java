package resonantinduction.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import resonantinduction.api.Probeable;

/**
 * A block with a mechanical node. Subclasses say which neighbours they mesh with ({@link #canMesh}); connections are made only
 * when both sides agree, and are rebuilt whenever a neighbour changes. Speed is synced to clients for animation.
 */
public abstract class MechanicalBlockEntity extends BlockEntity implements MechanicalNode.Owner, Probeable {
    protected final MechanicalNode node = new MechanicalNode(this);
    private boolean recache = true;
    private double syncedVelocity = Double.NaN;
    private int ticks;

    protected MechanicalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public MechanicalNode node() {
        return node;
    }

    /** The node other blocks see from {@code from} (the side of this block they touch), or null if none there. */
    @Nullable
    public MechanicalNode getNode(@Nullable Direction from) {
        return node;
    }

    /** May this block connect to {@code other} in {@code dir} (the direction from this block toward it)? */
    protected abstract boolean canMesh(Direction dir, MechanicalBlockEntity other);

    /** Neighbours to look at; by default the six adjacent blocks. */
    protected void collectConnections() {
        for (Direction dir : Direction.values()) {
            tryConnect(dir, worldPosition.relative(dir));
        }
    }

    protected void tryConnect(Direction dir, BlockPos pos) {
        if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof MechanicalBlockEntity other && other != this) {
            MechanicalNode otherNode = other.getNode(dir.getOpposite());
            if (otherNode != null && otherNode != node && canMesh(dir, other) && other.canMesh(dir.getOpposite(), this)) {
                node.connections().put(otherNode, dir);
            }
        }
    }

    public void markRecache() {
        recache = true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MechanicalBlockEntity be) {
        be.tickServer();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, MechanicalBlockEntity be) {
        be.node.clientTick();
    }

    protected void tickServer() {
        ticks++;
        if (recache || ticks % 40 == 0) {
            recache = false;
            node.connections().clear();
            collectConnections();
        }
        node.serverTick();
        double v = node.angularVelocity;
        if (Double.isNaN(syncedVelocity) || (Math.abs(v - syncedVelocity) > Math.max(0.05, Math.abs(syncedVelocity) * 0.1)) || (ticks % 100 == 0 && v != syncedVelocity)) {
            syncedVelocity = v;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    @Override
    public double probeTorque(Direction side) {
        return node.getTorque();
    }

    @Override
    public double probeAngularVelocity(Direction side) {
        return node.getAngularVelocity();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        node.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        node.load(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        node.save(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
