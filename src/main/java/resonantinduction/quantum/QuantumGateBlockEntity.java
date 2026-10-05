package resonantinduction.quantum;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import resonantinduction.registry.RIRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * A Quantum Gate: one block split into eight corner slots, each holding a glyph (one of {@link #GLYPHS} kinds). With all
 * eight filled the gate has a frequency, {@code sum(GLYPHS^slot * glyph)}. Gates on the same frequency:
 * <ul>
 * <li>teleport entities standing on them (players by sneaking on it or sneak-using it) to a random other gate of that
 * frequency, in any dimension;</li>
 * <li>share one item slot and a one-bucket fluid tank, reachable from every side by pipes and hoppers.</li>
 * </ul>
 */
public class QuantumGateBlockEntity extends BlockEntity {
    public static final int GLYPHS = 4;
    public static final int SLOTS = 8;

    private final byte[] glyphs = new byte[SLOTS];
    private int registeredFrequency = -1;
    private int ticks;

    public QuantumGateBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.QUANTUM_GATE_BE.get(), pos, state);
        Arrays.fill(glyphs, (byte) -1);
    }

    /** Slot index for a point inside the block (coordinates 0..1): x half = 4, z half = 2, y half = 1. */
    public static int slotAt(double x, double y, double z) {
        return (x >= 0.5 ? 4 : 0) | (z >= 0.5 ? 2 : 0) | (y >= 0.5 ? 1 : 0);
    }

    public static AABB slotBox(int slot) {
        double x = (slot & 4) != 0 ? 0.5 : 0;
        double z = (slot & 2) != 0 ? 0.5 : 0;
        double y = (slot & 1) != 0 ? 0.5 : 0;
        return new AABB(x, y, z, x + 0.5, y + 0.5, z + 0.5);
    }

    public int glyph(int slot) {
        return glyphs[slot];
    }

    /** Bit mask of filled slots. */
    public int mask() {
        int mask = 0;
        for (int i = 0; i < SLOTS; i++) {
            if (glyphs[i] >= 0) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    public int glyphCount() {
        return Integer.bitCount(mask());
    }

    public boolean setGlyph(int slot, int glyph) {
        if (glyphs[slot] >= 0) {
            return false;
        }
        glyphs[slot] = (byte) glyph;
        changed();
        return true;
    }

    /** -1 until all eight slots hold a glyph. */
    public int frequency() {
        int frequency = 0;
        int weight = 1;
        for (int i = 0; i < SLOTS; i++) {
            if (glyphs[i] < 0) {
                return -1;
            }
            frequency += weight * glyphs[i];
            weight *= GLYPHS;
        }
        return frequency;
    }

    public List<Integer> glyphList() {
        List<Integer> out = new ArrayList<>();
        for (byte g : glyphs) {
            if (g >= 0) {
                out.add((int) g);
            }
        }
        return out;
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.invalidateCapabilities(worldPosition);
            updateRegistration();
        }
    }

    private GlobalPos globalPos() {
        return GlobalPos.of(level.dimension(), worldPosition);
    }

    private void updateRegistration() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        int frequency = frequency();
        QuantumGateData data = QuantumGateData.get(server.getServer());
        if (registeredFrequency != -1 && registeredFrequency != frequency) {
            data.removeGate(registeredFrequency, globalPos());
        }
        if (frequency != -1) {
            data.addGate(frequency, globalPos());
        }
        registeredFrequency = frequency;
    }

    /** Called when the block is broken or replaced (not when its chunk unloads: unloaded gates stay reachable). */
    public void unregister() {
        if (level instanceof ServerLevel server && registeredFrequency != -1) {
            QuantumGateData.get(server.getServer()).removeGate(registeredFrequency, globalPos());
            registeredFrequency = -1;
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        updateRegistration();
    }

    // ---- ticking and teleporting ----

    public static void serverTick(Level level, BlockPos pos, BlockState state, QuantumGateBlockEntity be) {
        if (++be.ticks % 10 != 0 || be.frequency() == -1) {
            return;
        }
        AABB top = new AABB(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 1.5, pos.getZ() + 1);
        for (Entity entity : level.getEntities((Entity) null, top, e -> e.isAlive() && !e.isPassenger() && !e.isSpectator())) {
            // Players only go when they sneak, as in the original.
            if (entity instanceof Player player && !player.isShiftKeyDown()) {
                continue;
            }
            be.transport(entity);
        }
    }

    /** Sends {@code entity} to a random other gate on this frequency. */
    public boolean transport(Entity entity) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        int frequency = frequency();
        if (frequency == -1 || !QuantumTeleports.canTeleport(entity, globalPos())) {
            return false;
        }
        QuantumGateData data = QuantumGateData.get(server.getServer());
        List<GlobalPos> candidates = new ArrayList<>(data.gates(frequency));
        candidates.remove(globalPos());
        while (!candidates.isEmpty()) {
            GlobalPos target = candidates.remove(server.random.nextInt(candidates.size()));
            ServerLevel targetLevel = server.getServer().getLevel(target.dimension());
            if (targetLevel == null) {
                continue;
            }
            targetLevel.getChunkAt(target.pos());
            if (!(targetLevel.getBlockEntity(target.pos()) instanceof QuantumGateBlockEntity gate) || gate.frequency() != frequency) {
                // Stale entry (the gate was removed while its chunk was unloaded, or changed).
                data.removeGate(frequency, target);
                continue;
            }
            double x = target.pos().getX() + 0.5;
            double y = target.pos().getY() + 2;
            double z = target.pos().getZ() + 0.5;
            server.playSound(null, entity.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1f, 1f);
            if (entity.teleportTo(targetLevel, x, y, z, Set.of(), entity.getYRot(), entity.getXRot())) {
                targetLevel.playSound(null, target.pos().above(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1f, 1f);
                QuantumTeleports.arrived(entity, target);
                return true;
            }
            return false;
        }
        return false;
    }

    // ---- shared storage ----

    @Nullable
    public IItemHandler getItemCapability(@Nullable Direction side) {
        int frequency = frequency();
        return frequency != -1 && level instanceof ServerLevel server ? QuantumGateData.get(server.getServer()).inventory(frequency) : null;
    }

    @Nullable
    public IFluidHandler getFluidCapability(@Nullable Direction side) {
        int frequency = frequency();
        return frequency != -1 && level instanceof ServerLevel server ? QuantumGateData.get(server.getServer()).tank(frequency) : null;
    }

    // ---- saving and sync ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByteArray("glyphs", glyphs);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        byte[] saved = tag.getByteArray("glyphs");
        Arrays.fill(glyphs, (byte) -1);
        System.arraycopy(saved, 0, glyphs, 0, Math.min(saved.length, SLOTS));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putByteArray("glyphs", glyphs);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
