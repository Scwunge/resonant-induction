package resonantinduction.mechanical.turbine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import resonantinduction.RIConfig;
import resonantinduction.mechanical.MechanicalBlockEntity;
import resonantinduction.mechanical.MechanicalNode;
import resonantinduction.registry.RIRegistries;

/**
 * Wind and water turbines, ported from the original. Each tick the turbine gathers "power" from its surroundings; its own speed
 * is power / torque, and its node is eased a tenth of the way toward that torque and speed. The rotation leaves through the
 * back (the face it was placed against). A wrench on the middle of a 3x3 of matching turbines joins them into one big turbine
 * (nine times the power, other torque), and again splits it.
 */
public class TurbineBlockEntity extends MechanicalBlockEntity {
    public static final int RADIUS = 1;
    private static final long DEFAULT_TORQUE = 5000;

    /** Centre of the big turbine this belongs to, or null when standing alone. */
    @Nullable
    private BlockPos primary;
    private long power;
    private long torque = DEFAULT_TORQUE;
    private float turbineVelocity;
    private int ticks;
    // Wind sampling, as the original: 224 rays of open air around the turbine, refreshed one per second.
    private final byte[] openBlockCache = new byte[224];
    private int checkCount;
    private float efficiency;
    private long windPower;
    private int waterTicks;

    public TurbineBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.TURBINE_BE.get(), pos, state);
    }

    public TurbineBlock block() {
        return (TurbineBlock) getBlockState().getBlock();
    }

    public Direction facing() {
        return getBlockState().getValue(TurbineBlock.FACING);
    }

    public boolean isConstructed() {
        return primary != null && primary.equals(worldPosition);
    }

    public boolean isPrimary() {
        return primary == null || primary.equals(worldPosition);
    }

    @Nullable
    public BlockPos primaryPos() {
        return primary;
    }

    @Nullable
    private TurbineBlockEntity primaryEntity() {
        if (primary == null || primary.equals(worldPosition)) {
            return this;
        }
        return level.getBlockEntity(primary) instanceof TurbineBlockEntity t ? t : null;
    }

    // ---- node ----

    @Nullable
    @Override
    public MechanicalNode getNode(@Nullable Direction from) {
        TurbineBlockEntity p = primaryEntity();
        return p == null ? null : p.node;
    }

    @Override
    protected boolean canMesh(Direction dir, MechanicalBlockEntity other) {
        return isPrimary() && dir == facing().getOpposite() && !(other instanceof TurbineBlockEntity);
    }

    @Override
    public boolean inverseRotation(Direction dir, MechanicalNode with) {
        return dir == facing().getOpposite();
    }

    @Override
    public float ratio(Direction dir, MechanicalNode with) {
        return isConstructed() ? RADIUS - 0.5f : 0.5f;
    }

    @Override
    public double torqueLoad() {
        return 2;
    }

    @Override
    public double angularVelocityLoad() {
        return 2;
    }

    // ---- power ----

    private long maxPower() {
        boolean vertical = facing().getAxis() == Direction.Axis.Y;
        long max = block().kind() == TurbineBlock.Kind.WIND ? (vertical ? 10000 : 3000) : (vertical ? 10000 : 2500);
        return isConstructed() ? max * area() : max;
    }

    public static int area() {
        return (int) (((RADIUS + 0.5) * 2) * ((RADIUS + 0.5) * 2));
    }

    @Override
    protected void tickServer() {
        ticks++;
        ServerLevel server = (ServerLevel) level;
        TurbineBlockEntity p = primaryEntity();
        if (p == null) {
            primary = null;
            setChanged();
            p = this;
        }
        if (block().kind() == TurbineBlock.Kind.WIND) {
            if (block().tier() == 0 && facing().getAxis() != Direction.Axis.Y && server.isThundering() && server.random.nextFloat() < 0.00000008f) {
                // Storms tear apart wooden wind turbines, as in the original.
                Block.popResource(server, worldPosition, new ItemStack(Items.WHITE_WOOL, 1 + server.random.nextInt(2)));
                Block.popResource(server, worldPosition, new ItemStack(Items.STICK, 3 + server.random.nextInt(8)));
                server.removeBlock(worldPosition, false);
                return;
            }
            if (isPrimary() && facing().getAxis() != Direction.Axis.Y) {
                if (ticks % 20 == 0) {
                    computeWindPower(server);
                }
                p.power += windPower;
            }
            torque = isConstructed() ? (long) (DEFAULT_TORQUE / (9d / RADIUS)) : DEFAULT_TORQUE / 12;
        } else {
            gatherWaterPower(server, p);
            torque = isConstructed() ? (long) (DEFAULT_TORQUE / (1d / RADIUS)) : DEFAULT_TORQUE / 12;
        }

        if (isPrimary()) {
            turbineVelocity = (float) ((double) power / torque);
            if (power > 0) {
                long t = node.torque < 0 ? -Math.abs(torque) : torque;
                float v = node.angularVelocity < 0 ? -Math.abs(turbineVelocity) : turbineVelocity;
                node.apply((t - node.getTorque()) / 10, (v - node.getAngularVelocity()) / 10);
            }
            super.tickServer();
        }
        power = 0;
    }

    private void computeWindPower(ServerLevel level) {
        int checkSize = 10;
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();
        int height = y + checkCount / 28;
        int deviation = checkCount % 7;
        Direction dir;
        BlockPos.MutableBlockPos check;
        switch (checkCount / 7 % 4) {
            case 0 -> {
                dir = Direction.NORTH;
                check = new BlockPos.MutableBlockPos(x - 3 + deviation, height, z - 4);
            }
            case 1 -> {
                dir = Direction.WEST;
                check = new BlockPos.MutableBlockPos(x - 4, height, z - 3 + deviation);
            }
            case 2 -> {
                dir = Direction.SOUTH;
                check = new BlockPos.MutableBlockPos(x - 3 + deviation, height, z + 4);
            }
            default -> {
                dir = Direction.EAST;
                check = new BlockPos.MutableBlockPos(x + 4, height, z - 3 + deviation);
            }
        }
        byte open = 0;
        while (open < checkSize && level.isLoaded(check) && level.isEmptyBlock(check)) {
            check.move(dir);
            open++;
        }
        efficiency = efficiency - openBlockCache[checkCount] + open;
        openBlockCache[checkCount] = open;
        checkCount = (checkCount + 1) % (openBlockCache.length - 1);

        float multiblock = isConstructed() ? (RADIUS + 0.5f) * 2 : 1;
        int tier = block().tier();
        float material = tier == 0 ? 1.1f : tier == 1 ? 0.9f : 1;
        var biome = level.getBiome(worldPosition);
        boolean bonus = biome.is(BiomeTags.IS_OCEAN) || biome.is(Biomes.PLAINS) || biome.is(BiomeTags.IS_RIVER);
        float wind = level.random.nextFloat() / 8 + (y / 256f) * (bonus ? 1.2f : 1) + level.getRainLevel(1.5f);
        double ratio = RIConfig.get(RIConfig.WIND_POWER_RATIO);
        windPower = (long) Math.min(material * multiblock * wind * efficiency * ratio, maxPower() * ratio);
    }

    private void gatherWaterPower(ServerLevel level, TurbineBlockEntity p) {
        Direction facing = facing();
        double ratio = RIConfig.get(RIConfig.WATER_POWER_RATIO);
        long waterPower = (long) ((maxPower() / (2 - block().tier() + 1)) * ratio);
        if (facing.getAxis() == Direction.Axis.Y) {
            // Vertical: water falling through from above, one source block a second.
            if (waterTicks > 0) {
                p.power += waterPower;
                waterTicks--;
            }
            if (ticks % 20 == 0) {
                BlockPos above = worldPosition.above();
                BlockPos below = worldPosition.below();
                FluidState water = level.getFluidState(above);
                if (water.is(Fluids.WATER) && water.isSource() && level.isEmptyBlock(below)) {
                    waterTicks = 20;
                    level.setBlockAndUpdate(above, Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(below, Fluids.FLOWING_WATER.getFlowing(7, true).createLegacyBlock());
                }
            }
            return;
        }
        // Horizontal: a waterwheel turned by water flowing past its sides.
        for (Direction dir : Direction.values()) {
            if (dir.getAxis() == facing.getAxis()) {
                continue;
            }
            BlockPos check = worldPosition.relative(dir);
            FluidState fluid = level.getFluidState(check);
            if (!fluid.is(Fluids.WATER) && !fluid.is(Fluids.FLOWING_WATER)) {
                continue;
            }
            Vec3 flow = fluid.getFlow(level, check);
            if ((facing.getStepZ() > 0 && flow.x < 0) || (facing.getStepZ() < 0 && flow.x > 0)
                    || (facing.getStepX() > 0 && flow.z > 0) || (facing.getStepX() < 0 && flow.z < 0)) {
                torque = -torque;
            }
            int levelLeft = 8 - fluid.getAmount();
            double strength = (7 - Math.min(7, levelLeft)) / 7.0;
            if (facing.getStepX() != 0) {
                p.power += (long) Math.abs(waterPower * flow.z * strength);
            }
            if (facing.getStepZ() != 0) {
                p.power += (long) Math.abs(waterPower * flow.x * strength);
            }
        }
    }

    // ---- multiblock ----

    /** The 3x3 around this block in the plane the turbine faces. */
    private Iterable<BlockPos> footprint() {
        Direction f = facing();
        java.util.List<BlockPos> out = new java.util.ArrayList<>();
        for (int a = -RADIUS; a <= RADIUS; a++) {
            for (int b = -RADIUS; b <= RADIUS; b++) {
                out.add(switch (f.getAxis()) {
                    case X -> worldPosition.offset(0, a, b);
                    case Y -> worldPosition.offset(a, 0, b);
                    case Z -> worldPosition.offset(a, b, 0);
                });
            }
        }
        return out;
    }

    /** Wrench: join a 3x3 around this centre, or split the big turbine this belongs to. */
    public boolean toggleConstruct() {
        if (primary != null) {
            TurbineBlockEntity p = primaryEntity();
            if (p != null) {
                p.deconstruct();
            }
            return true;
        }
        for (BlockPos pos : footprint()) {
            if (!(level.getBlockEntity(pos) instanceof TurbineBlockEntity t) || t.getBlockState().getBlock() != getBlockState().getBlock()
                    || t.facing() != facing() || t.primary != null) {
                return false;
            }
        }
        for (BlockPos pos : footprint()) {
            TurbineBlockEntity t = (TurbineBlockEntity) level.getBlockEntity(pos);
            t.primary = worldPosition.immutable();
            t.markRecache();
            t.setChanged();
            level.sendBlockUpdated(pos, t.getBlockState(), t.getBlockState(), 3);
            level.updateNeighborsAt(pos, t.getBlockState().getBlock());
        }
        return true;
    }

    public void deconstruct() {
        if (!isConstructed()) {
            return;
        }
        for (BlockPos pos : footprint()) {
            if (level.getBlockEntity(pos) instanceof TurbineBlockEntity t && worldPosition.equals(t.primary)) {
                t.primary = null;
                t.markRecache();
                t.setChanged();
                level.sendBlockUpdated(pos, t.getBlockState(), t.getBlockState(), 3);
                level.updateNeighborsAt(pos, t.getBlockState().getBlock());
            }
        }
    }

    /** Called when this block is broken: the big turbine falls apart. */
    public void onBroken() {
        TurbineBlockEntity p = primaryEntity();
        if (p != null && primary != null) {
            p.deconstruct();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (primary != null) {
            tag.putLong("primary", primary.asLong());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        primary = tag.contains("primary") ? BlockPos.of(tag.getLong("primary")) : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (primary != null) {
            tag.putLong("primary", primary.asLong());
        }
        return tag;
    }
}
