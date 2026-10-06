package resonantinduction.atomic.particle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import resonantinduction.ResonantInduction;
import resonantinduction.atomic.Radiation;
import resonantinduction.atomic.fusion.ElectromagnetBlock;
import resonantinduction.registry.RIRegistries;

import java.util.HashSet;
import java.util.Set;

/**
 * A particle in an accelerator, as the original: it speeds up along a tunnel of electromagnets (above, below and on both sides),
 * turns into the open side where a side wall ends, and loses some speed doing so. If it leaves the tunnel or meets anything (another
 * particle above all) it crashes: a fast one (over half speed) shatters into what may become dark matter; a slow one explodes. A
 * wall stops it dead first, so it only pops. It keeps the chunks it passes through loaded.
 */
public class ParticleEntity extends Entity {
    private static final double ACCELERATION = 0.0006;
    /** Chunks kept loaded for particles; particles are not saved, so tickets left from a past session are dropped. */
    public static final TicketController TICKETS = new TicketController(ResonantInduction.id("particle"),
            (level, helper) -> helper.getEntityTickets().keySet().forEach(helper::removeAllTickets));

    private BlockPos accelerator = BlockPos.ZERO;
    private Direction direction = Direction.NORTH;
    private int lastTurn = 60;
    private boolean collided;
    private final Set<Long> forced = new HashSet<>();

    public ParticleEntity(EntityType<? extends ParticleEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public ParticleEntity(Level level, BlockPos at, BlockPos accelerator, Direction direction) {
        this(RIRegistries.PARTICLE.get(), level);
        setPos(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5);
        this.accelerator = accelerator;
        this.direction = direction;
    }

    /** Whether a particle can travel through {@code pos}: open, with electromagnets above and below. */
    public static boolean canTravel(Level level, BlockPos pos) {
        return level.getBlockState(pos).isAir() && isElectromagnet(level, pos.above()) && isElectromagnet(level, pos.below());
    }

    public static boolean isElectromagnet(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(ElectromagnetBlock.ELECTROMAGNETS);
    }

    /** The speed: the sum of the motion along each axis, as the original. */
    public double velocity() {
        Vec3 m = getDeltaMovement();
        return Math.abs(m.x) + Math.abs(m.y) + Math.abs(m.z);
    }

    /** Whether it crashed fast enough to make dark matter. */
    public boolean collided() {
        return collided;
    }

    public Direction direction() {
        return direction;
    }

    @Override
    public void tick() {
        if (level().isClientSide) {
            super.tick();
            level().addParticle(ParticleTypes.PORTAL, getX(), getY(), getZ(), 0, 0, 0);
            level().addParticle(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(), 0, 0, 0);
            return;
        }
        xo = getX();
        yo = getY();
        zo = getZ();
        tickCount++;
        if (tickCount % 10 == 0) {
            level().playSound(null, getX(), getY(), getZ(), RIRegistries.ACCELERATOR_SOUND.get(), SoundSource.BLOCKS, 1f,
                    (float) (0.6 + 0.4 * velocity() / AcceleratorBlockEntity.MAX_VELOCITY));
        }
        if (!level().isLoaded(accelerator) || !(level().getBlockEntity(accelerator) instanceof AcceleratorBlockEntity tile)) {
            discard();
            return;
        }
        tile.claim(this);
        forceChunks();

        double acceleration = ACCELERATION;
        BlockPos here = blockPosition();
        if ((!isElectromagnet(level(), here.relative(direction.getClockWise())) || !isElectromagnet(level(), here.relative(direction.getCounterClockWise())))
                && lastTurn <= 0) {
            acceleration = turn();
            setDeltaMovement(Vec3.ZERO);
            lastTurn = 40;
            if (isRemoved()) {
                return;
            }
        }
        lastTurn--;
        if (!canTravel(level(), blockPosition())) {
            crash();
            return;
        }
        double max = AcceleratorBlockEntity.MAX_VELOCITY;
        Vec3 push = Vec3.atLowerCornerOf(direction.getNormal()).scale(acceleration);
        Vec3 motion = getDeltaMovement().add(push);
        setDeltaMovement(Mth.clamp(motion.x, -max, max), Mth.clamp(motion.y, -max, max), Mth.clamp(motion.z, -max, max));
        move(MoverType.SELF, getDeltaMovement());
        if (horizontalCollision || verticalCollision) {
            crash();
            return;
        }
        if (getX() == xo && getY() == yo && getZ() == zo && velocity() <= 0 && lastTurn <= 0) {
            discard();
            return;
        }
        // Anything else in its way, even another particle.
        if (level().getEntities(this, getBoundingBox().inflate(0.35)).size() > 0) {
            crash();
        }
    }

    /** Turns into the open side, centring in the block, and returns the speed it keeps, as the original. */
    private double turn() {
        BlockPos here = blockPosition();
        Direction left = direction.getCounterClockWise();
        Direction right = direction.getClockWise();
        if (level().getBlockState(here.relative(left)).isAir()) {
            direction = left;
        } else if (level().getBlockState(here.relative(right)).isAir()) {
            direction = right;
        } else {
            discard();
            return 0;
        }
        setPos(here.getX() + 0.5, here.getY() + 0.5, here.getZ() + 0.5);
        double v = velocity();
        return v - v / Mth.clamp(70 * v, 4, 30);
    }

    /** A fast particle shatters (the accelerator may find dark matter); a slow one explodes. Both irradiate those near. */
    private void crash() {
        level().playSound(null, getX(), getY(), getZ(), RIRegistries.ANTIMATTER_SOUND.get(), SoundSource.BLOCKS, 1.5f, 1f - random.nextFloat() * 0.3f);
        if (velocity() > AcceleratorBlockEntity.MAX_VELOCITY / 2) {
            collided = true;
            discard();
            return;
        }
        level().explode(this, getX(), getY(), getZ(), (float) velocity() * 2.5f, Level.ExplosionInteraction.TNT);
        Radiation.exposeAround(level(), blockPosition(), 6, 0);
        discard();
    }

    private void forceChunks() {
        ServerLevel level = (ServerLevel) level();
        int cx = SectionPos.posToSectionCoord(getX());
        int cz = SectionPos.posToSectionCoord(getZ());
        Set<Long> want = new HashSet<>();
        for (int x = -1; x <= 0; x++) {
            for (int z = -1; z <= 0; z++) {
                want.add(ChunkPos.asLong(cx + x, cz + z));
            }
        }
        if (want.equals(forced)) {
            return;
        }
        for (long c : forced) {
            if (!want.contains(c)) {
                TICKETS.forceChunk(level, this, ChunkPos.getX(c), ChunkPos.getZ(c), false, true);
            }
        }
        for (long c : want) {
            if (!forced.contains(c)) {
                TICKETS.forceChunk(level, this, ChunkPos.getX(c), ChunkPos.getZ(c), true, true);
            }
        }
        forced.clear();
        forced.addAll(want);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel level) {
            for (long c : forced) {
                TICKETS.forceChunk(level, this, ChunkPos.getX(c), ChunkPos.getZ(c), false, true);
            }
            forced.clear();
        }
        super.remove(reason);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256 * 256;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
