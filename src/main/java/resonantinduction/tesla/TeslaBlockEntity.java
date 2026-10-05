package resonantinduction.tesla;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;
import resonantinduction.RIConfig;
import resonantinduction.item.Linkable;
import resonantinduction.network.RINetwork;
import resonantinduction.registry.RIRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * One coil of a Tesla tower. Coils stack vertically; the lowest coil (the primary) holds the tower's energy and settings
 * and does all the work. Range grows with height: {@code rangePerBlock * (height - 1)}, capped at {@code maxRange}.
 * <p>
 * Energy is kept in two buffers so it cannot bounce between towers: {@code charge} is filled from cables and beamed
 * out; {@code received} is filled by other towers and pushed into machines next to the primary coil.
 * <p>
 * Controls (as in the original): dye sets the frequency colour (the default colour talks to every colour), an empty hand
 * toggles receiving, redstone dust toggles hurting mobs, a redstone signal on the primary coil stops it sending, and a
 * Quantum Entangler links two towers so they send only to each other, across dimensions.
 */
public class TeslaBlockEntity extends BlockEntity implements Linkable {
    public static final DyeColor DEFAULT_COLOR = DyeColor.LIGHT_BLUE;

    private int charge;
    private int received;
    private DyeColor color = DEFAULT_COLOR;
    private boolean canReceive = true;
    private boolean attackEntities = true;
    @Nullable
    private GlobalPos link;

    private int ticks;
    private int zapCounter;

    /** Exposed on the primary coil only, on every side but the top: cables fill {@code charge}, machines drain {@code received}. */
    private final IEnergyStorage energyView = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int max, boolean simulate) {
            int accepted = Math.max(0, Math.min(max, capacity() - charge));
            if (!simulate && accepted > 0) {
                charge += accepted;
                setChanged();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int max, boolean simulate) {
            int extracted = Math.max(0, Math.min(max, received));
            if (!simulate && extracted > 0) {
                received -= extracted;
                setChanged();
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, (long) charge + received);
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, 2L * capacity());
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

    public TeslaBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.TESLA_BE.get(), pos, state);
    }

    public static int capacity() {
        return RIConfig.get(RIConfig.TESLA_CAPACITY);
    }

    @Nullable
    public IEnergyStorage getEnergyCapability(@Nullable Direction side) {
        return side != Direction.UP && isPrimary() ? energyView : null;
    }

    // ---- tower structure ----

    private boolean isCoil(BlockPos pos) {
        return level != null && level.getBlockState(pos).getBlock() instanceof TeslaBlock;
    }

    public boolean isPrimary() {
        return !isCoil(worldPosition.below());
    }

    /** The lowest coil of this tower. */
    public TeslaBlockEntity primary() {
        BlockPos pos = worldPosition;
        while (isCoil(pos.below())) {
            pos = pos.below();
        }
        return pos.equals(worldPosition) || !(level.getBlockEntity(pos) instanceof TeslaBlockEntity be) ? this : be;
    }

    /** Number of coils from this one upward (the full tower height when called on the primary). */
    public int height() {
        int h = 1;
        while (isCoil(worldPosition.above(h))) {
            h++;
        }
        return h;
    }

    public BlockPos top() {
        return worldPosition.above(height() - 1);
    }

    public int range() {
        return Math.min(RIConfig.get(RIConfig.TESLA_RANGE_PER_BLOCK) * (height() - 1), RIConfig.get(RIConfig.TESLA_MAX_RANGE));
    }

    // ---- ticking ----

    public static void serverTick(Level level, BlockPos pos, BlockState state, TeslaBlockEntity be) {
        be.tick((ServerLevel) level);
    }

    private void tick(ServerLevel level) {
        if (!isPrimary()) {
            handOverToPrimary();
            return;
        }
        pushToNeighbours(level);
        ticks++;
        if (charge > 0 && ticks % (4 + level.random.nextInt(2)) == 0 && !level.hasNeighborSignal(worldPosition)) {
            zap(level);
        }
    }

    /** A coil that stopped being the bottom one (a coil was placed under it) passes its energy and link down. */
    private void handOverToPrimary() {
        if (charge == 0 && received == 0 && link == null) {
            return;
        }
        TeslaBlockEntity primary = primary();
        if (primary == this) {
            return;
        }
        int moved = Math.min(charge, capacity() - primary.charge);
        primary.charge += Math.max(0, moved);
        charge -= Math.max(0, moved);
        moved = Math.min(received, capacity() - primary.received);
        primary.received += Math.max(0, moved);
        received -= Math.max(0, moved);
        if (link != null && primary.link == null) {
            primary.link = link;
        }
        link = null;
        primary.setChanged();
        setChanged();
    }

    private void pushToNeighbours(ServerLevel level) {
        if (received <= 0) {
            return;
        }
        int before = received;
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP || received <= 0) {
                continue;
            }
            BlockPos target = worldPosition.relative(dir);
            if (isCoil(target)) {
                continue;
            }
            IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, target, dir.getOpposite());
            if (storage != null && storage.canReceive()) {
                received -= Math.max(0, storage.receiveEnergy(received, false));
            }
        }
        if (received != before) {
            setChanged();
        }
    }

    private void zap(ServerLevel level) {
        int cap = capacity();
        BlockPos top = top();
        Vec3 from = Vec3.atCenterOf(top);
        float volume = Math.min(1f, charge / (float) cap);
        boolean sent = false;

        if (link != null) {
            // Quantum link: everything goes to the partner tower, wherever it is.
            TeslaBlockEntity partner = resolve(level, link, false);
            if (partner != null) {
                int accepted = partner.receiveWireless(Math.min(charge, cap));
                if (accepted > 0) {
                    charge -= accepted;
                    sent = true;
                    RINetwork.sendZap(level, from, from.add(0, 48, 0), color);
                    Vec3 partnerTop = Vec3.atCenterOf(partner.top());
                    RINetwork.sendZap((ServerLevel) partner.level, partnerTop, partnerTop.add(0, 48, 0), partner.color);
                }
            }
        } else {
            List<TeslaBlockEntity> targets = findTargets(level, top);
            int count = Math.min(targets.size(), RIConfig.get(RIConfig.TESLA_MAX_TARGETS));
            if (count > 0) {
                int share = Math.max(1, Math.min(charge / count, cap));
                for (int i = 0; i < count && charge > 0; i++) {
                    TeslaBlockEntity target = targets.get(i);
                    int accepted = target.receiveWireless(Math.min(share, charge));
                    if (accepted <= 0) {
                        continue;
                    }
                    charge -= accepted;
                    sent = true;
                    int h = target.height();
                    Vec3 to = Vec3.atCenterOf(target.top()).add(0, level.random.nextDouble() * h / 3.0 - h / 3.0, 0);
                    RINetwork.sendZap(level, from, to, color);
                    if (attackEntities && zapCounter % 5 == 0) {
                        electrocute(level, from, to);
                    }
                }
            }
        }

        if (sent) {
            setChanged();
            if (zapCounter % 5 == 0 && RIConfig.get(RIConfig.TESLA_SOUNDS)) {
                // Pitch by colour as in the original (old dye index = 15 - wool id).
                float pitch = 1.3f - 0.5f * ((15 - color.getId()) / 16f);
                level.playSound(null, top, RIRegistries.ELECTRIC_SHOCK.get(), SoundSource.BLOCKS, volume, pitch);
            }
        }
        zapCounter++;
    }

    /** Towers in range that will take energy from this one, nearest first. */
    private List<TeslaBlockEntity> findTargets(ServerLevel level, BlockPos top) {
        int range = range();
        List<TeslaBlockEntity> out = new ArrayList<>();
        if (range <= 0) {
            return out;
        }
        double rangeSq = (double) range * range;
        Set<TeslaBlockEntity> seen = new HashSet<>();
        for (BlockPos pos : new ArrayList<>(TeslaGrid.get(level))) {
            if (pos.distSqr(worldPosition) >= rangeSq || !level.isLoaded(pos)) {
                continue;
            }
            if (!(level.getBlockEntity(pos) instanceof TeslaBlockEntity coil)) {
                continue;
            }
            TeslaBlockEntity other = coil.primary();
            if (other == this || !seen.add(other)) {
                continue;
            }
            // A single coil has no range of its own and only links; towers need two coils to receive beams.
            if (other.height() <= 1 || !other.acceptsFrom(this)) {
                continue;
            }
            out.add(other);
        }
        out.sort(Comparator.comparingDouble(t -> t.worldPosition.distSqr(top)));
        return out;
    }

    private boolean acceptsFrom(TeslaBlockEntity source) {
        return canReceive && received < capacity() && colorsMatch(color, source.color);
    }

    public static boolean colorsMatch(DyeColor a, DyeColor b) {
        return a == b || a == DEFAULT_COLOR || b == DEFAULT_COLOR;
    }

    /** Energy arriving through the air. Returns how much was taken. */
    public int receiveWireless(int amount) {
        int accepted = Math.max(0, Math.min(amount, capacity() - received));
        if (accepted > 0) {
            received += accepted;
            setChanged();
        }
        return accepted;
    }

    /** Hurts the first living thing standing in the arc. */
    private void electrocute(ServerLevel level, Vec3 from, Vec3 to) {
        double damage = RIConfig.get(RIConfig.TESLA_DAMAGE);
        if (damage <= 0) {
            return;
        }
        boolean hitPlayers = RIConfig.get(RIConfig.TESLA_ATTACK_PLAYERS) && level.getServer().isPvpAllowed();
        LivingEntity victim = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(1), LivingEntity::isAlive)) {
            if (e instanceof Player && !hitPlayers) {
                continue;
            }
            Optional<Vec3> hit = e.getBoundingBox().inflate(0.3).clip(from, to);
            if (hit.isPresent() && hit.get().distanceToSqr(from) < best) {
                best = hit.get().distanceToSqr(from);
                victim = e;
            }
        }
        if (victim != null) {
            DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(RIRegistries.ELECTROCUTION));
            if (victim.hurt(source, (float) damage)) {
                RINetwork.sendZap(level, from, victim.position().add(0, victim.getBbHeight() / 2, 0), color);
            }
        }
    }

    // ---- linking ----

    /**
     * Finds the tower at {@code pos}. With {@code loadChunk} the chunk is loaded if needed (used once when linking);
     * transfers never load chunks, so a remote tower needs its chunk loaded (a player or chunk loader nearby).
     */
    @Nullable
    private TeslaBlockEntity resolve(ServerLevel from, GlobalPos pos, boolean loadChunk) {
        if (!pos.dimension().equals(from.dimension()) && !RIConfig.get(RIConfig.TESLA_CROSS_DIMENSION)) {
            return null;
        }
        ServerLevel level = from.getServer().getLevel(pos.dimension());
        if (level == null || (!loadChunk && !level.isLoaded(pos.pos()))) {
            return null;
        }
        if (loadChunk) {
            level.getChunkAt(pos.pos());
        }
        return level.getBlockEntity(pos.pos()) instanceof TeslaBlockEntity be ? be.primary() : null;
    }

    @Override
    public Linkable linkOwner() {
        return primary();
    }

    /** Links this tower and the one at {@code target} both ways, unlinking any previous partners. */
    @Override
    public boolean linkTo(GlobalPos target) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        TeslaBlockEntity other = resolve(server, target, true);
        if (other == null || other == this) {
            return false;
        }
        unlink();
        other.unlink();
        link = GlobalPos.of(other.level.dimension(), other.worldPosition);
        other.link = GlobalPos.of(level.dimension(), worldPosition);
        setChanged();
        other.setChanged();
        return true;
    }

    @Override
    public void unlink() {
        if (link != null && level instanceof ServerLevel server) {
            TeslaBlockEntity other = resolve(server, link, true);
            if (other != null && other.link != null && other.link.pos().equals(worldPosition)) {
                other.link = null;
                other.setChanged();
            }
        }
        link = null;
        setChanged();
    }

    @Nullable
    public GlobalPos getLink() {
        return link;
    }

    // ---- settings ----

    public DyeColor getColor() {
        return color;
    }

    public void setColor(DyeColor color) {
        this.color = color;
        setChanged();
    }

    public boolean canReceive() {
        return canReceive;
    }

    public boolean toggleReceive() {
        canReceive = !canReceive;
        setChanged();
        return canReceive;
    }

    public boolean toggleAttack() {
        attackEntities = !attackEntities;
        setChanged();
        return attackEntities;
    }

    public int getCharge() {
        return charge;
    }

    public int getReceived() {
        return received;
    }

    public Component status() {
        Component linkText = link == null ? Component.translatable("message.resonantinduction.tesla.unlinked")
                : Component.translatable("message.resonantinduction.tesla.linked_to", link.pos().getX(), link.pos().getY(), link.pos().getZ(), link.dimension().location().toString());
        return Component.translatable("message.resonantinduction.tesla.status", height(), range(), charge, received, capacity(),
                Component.translatable("color.minecraft." + color.getName()), canReceive, attackEntities, linkText);
    }

    // ---- lifecycle and saving ----

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            TeslaGrid.add(level, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            TeslaGrid.remove(level, worldPosition);
        }
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if (level != null && !level.isClientSide) {
            TeslaGrid.remove(level, worldPosition);
        }
        super.onChunkUnloaded();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("charge", charge);
        tag.putInt("received", received);
        tag.putString("color", color.getSerializedName());
        tag.putBoolean("canReceive", canReceive);
        tag.putBoolean("attackEntities", attackEntities);
        if (link != null) {
            GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, link).result().ifPresent(t -> tag.put("link", t));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        charge = tag.getInt("charge");
        received = tag.getInt("received");
        color = DyeColor.byName(tag.getString("color"), DEFAULT_COLOR);
        canReceive = !tag.contains("canReceive") || tag.getBoolean("canReceive");
        attackEntities = !tag.contains("attackEntities") || tag.getBoolean("attackEntities");
        link = tag.contains("link") ? GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("link")).result().orElse(null) : null;
    }
}
