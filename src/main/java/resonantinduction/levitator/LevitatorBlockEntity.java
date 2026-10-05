package resonantinduction.levitator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;
import resonantinduction.RIConfig;
import resonantinduction.item.Linkable;
import resonantinduction.network.RINetwork;
import resonantinduction.registry.RIRegistries;
import resonantinduction.tesla.TeslaBlockEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Electromagnetic Levitator. Sits on the face of an inventory and points its beam away from it.
 * <ul>
 * <li>Pull mode (default): item entities that reach the levitator are put into the inventory.</li>
 * <li>Push mode (sneak-use with an empty hand): items are taken out of the inventory one at a time and carried along the
 * beam to a levitator facing this one, up to {@code reach} blocks away.</li>
 * <li>Linked (Quantum Entangler): a pushing levitator finds a path through open space to its partner, which must be
 * pulling, and carries items along it around corners.</li>
 * </ul>
 * A redstone signal turns it off. Dye sets the beam colour.
 */
public class LevitatorBlockEntity extends BlockEntity implements Linkable {
    /** Pull items into the inventory (true) or push them out along the beam (false). */
    private boolean input = true;
    private DyeColor color = TeslaBlockEntity.DEFAULT_COLOR;
    @Nullable
    private BlockPos link;

    private int ticks;
    private int pushDelay;
    private int beamLength;
    private int pathCooldown;
    @Nullable
    private List<BlockPos> path;

    public LevitatorBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.LEVITATOR_BE.get(), pos, state);
    }

    public Direction facing() {
        return getBlockState().getValue(LevitatorBlock.FACING);
    }

    @Nullable
    private IItemHandler inventory() {
        Direction facing = facing();
        return level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(facing.getOpposite()), facing);
    }

    /** Blocks items can float through: anything without collision except lava. */
    public static boolean canBePath(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty() && !state.getFluidState().is(FluidTags.LAVA);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LevitatorBlockEntity be) {
        be.tick((ServerLevel) level);
    }

    private void tick(ServerLevel level) {
        ticks++;
        pushDelay = Math.max(0, pushDelay - 1);
        pathCooldown--;

        IItemHandler inventory = inventory();
        boolean active = inventory != null && !level.hasNeighborSignal(worldPosition);
        if (getBlockState().getValue(LevitatorBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, getBlockState().setValue(LevitatorBlock.ACTIVE, active), 3);
        }
        if (!active) {
            return;
        }

        if (input) {
            absorb(level, inventory);
            return;
        }

        if (pushDelay == 0) {
            ItemStack taken = takeTop(inventory, RIConfig.get(RIConfig.LEVITATOR_ITEMS_PER_PUSH));
            if (!taken.isEmpty()) {
                Vec3 c = Vec3.atCenterOf(worldPosition);
                ItemEntity item = new ItemEntity(level, c.x, c.y, c.z, taken, 0, 0, 0);
                item.setPickUpDelay(10);
                level.addFreshEntity(item);
                pushDelay = RIConfig.get(RIConfig.LEVITATOR_PUSH_DELAY);
            }
        }

        LevitatorBlockEntity partner = partner();
        if (partner != null) {
            if (partner.input) {
                followPath(level, partner);
            }
            return;
        }

        if (ticks % 20 == 1) {
            beamLength = findFacingLevitator(level);
        }
        if (beamLength > 0) {
            Direction facing = facing();
            BlockPos end = worldPosition.relative(facing, beamLength);
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(Vec3.atLowerCornerOf(worldPosition), Vec3.atLowerCornerOf(end).add(1, 1, 1)))) {
                move(item, facing, worldPosition);
            }
            if (ticks % 4 == 0) {
                RINetwork.sendZap(level, beamStart(), Vec3.atCenterOf(end), color);
            }
        }
    }

    private Vec3 beamStart() {
        Direction f = facing();
        return Vec3.atCenterOf(worldPosition).subtract(f.getStepX() / 3.0, f.getStepY() / 3.0, f.getStepZ() / 3.0);
    }

    /** Distance to a levitator facing this one along the beam, or 0 if there is none in reach. */
    private int findFacingLevitator(Level level) {
        Direction facing = facing();
        int reach = RIConfig.get(RIConfig.LEVITATOR_REACH);
        for (int i = 1; i <= reach; i++) {
            BlockPos pos = worldPosition.relative(facing, i);
            if (!level.isLoaded(pos)) {
                return 0;
            }
            if (level.getBlockEntity(pos) instanceof LevitatorBlockEntity other && other.facing() == facing.getOpposite()) {
                return i;
            }
            if (!canBePath(level, pos)) {
                return 0;
            }
        }
        return 0;
    }

    private void absorb(ServerLevel level, IItemHandler inventory) {
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(worldPosition))) {
            if (!item.isAlive()) {
                continue;
            }
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(inventory, item.getItem().copy(), false);
            if (remainder.isEmpty()) {
                item.discard();
            } else {
                item.setItem(remainder);
            }
        }
    }

    private static ItemStack takeTop(IItemHandler inventory, int count) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack taken = inventory.extractItem(slot, count, false);
            if (!taken.isEmpty()) {
                return taken;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Carries an item along {@code dir}, held on the line through the middle of {@code lock}, as the original did. */
    private static void move(ItemEntity item, Direction dir, BlockPos lock) {
        double max = RIConfig.get(RIConfig.LEVITATOR_MAX_SPEED);
        double acc = RIConfig.get(RIConfig.LEVITATOR_ACCELERATION);
        Vec3 m = item.getDeltaMovement();
        double cx = lock.getX() + 0.5;
        double cy = lock.getY() + 0.5;
        double cz = lock.getZ() + 0.5;
        switch (dir) {
            case UP -> {
                item.setPos(cx, item.getY(), cz);
                item.setDeltaMovement(0, Math.min(max, m.y + 0.04 + acc), 0);
            }
            case DOWN -> {
                item.setPos(cx, item.getY(), cz);
                item.setDeltaMovement(0, Math.max(-max, m.y - acc), 0);
            }
            case NORTH -> {
                item.setPos(cx, cy, item.getZ());
                item.setDeltaMovement(0, 0, Math.max(-max, m.z - acc));
            }
            case SOUTH -> {
                item.setPos(cx, cy, item.getZ());
                item.setDeltaMovement(0, 0, Math.min(max, m.z + acc));
            }
            case WEST -> {
                item.setPos(item.getX(), cy, cz);
                item.setDeltaMovement(Math.max(-max, m.x - acc), 0, 0);
            }
            case EAST -> {
                item.setPos(item.getX(), cy, cz);
                item.setDeltaMovement(Math.min(max, m.x + acc), 0, 0);
            }
        }
        item.hasImpulse = true;
        item.setPickUpDelay(5);
    }

    // ---- linked pathfinding ----

    private void followPath(ServerLevel level, LevitatorBlockEntity partner) {
        if (path != null && ticks % 10 == 0 && !pathStillOpen(level)) {
            path = null;
        }
        if (path == null) {
            if (pathCooldown > 0) {
                return;
            }
            path = findPath(level, worldPosition, partner.worldPosition);
            pathCooldown = 40;
            if (path == null) {
                return;
            }
        }
        for (int i = 0; i < path.size() - 1; i++) {
            BlockPos cell = path.get(i);
            Direction dir = Direction.getNearest(path.get(i + 1).getX() - cell.getX(), path.get(i + 1).getY() - cell.getY(), path.get(i + 1).getZ() - cell.getZ());
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(cell))) {
                move(item, dir, cell);
            }
        }
        if (ticks % 5 == 0) {
            drawPath(level);
        }
    }

    private boolean pathStillOpen(Level level) {
        for (int i = 1; i < path.size() - 1; i++) {
            if (!canBePath(level, path.get(i))) {
                return false;
            }
        }
        return true;
    }

    /** One arc per straight run of the path. */
    private void drawPath(ServerLevel level) {
        int runStart = 0;
        for (int i = 1; i < path.size(); i++) {
            boolean last = i == path.size() - 1;
            boolean turns = !last && !path.get(i + 1).subtract(path.get(i)).equals(path.get(i).subtract(path.get(i - 1)));
            if (last || turns) {
                RINetwork.sendZap(level, Vec3.atCenterOf(path.get(runStart)), Vec3.atCenterOf(path.get(i)), color);
                runStart = i;
            }
        }
    }

    /**
     * Breadth-first search through open blocks from {@code start} to {@code goal}. Like the original A* search it gives up on
     * nodes more than twice the straight distance from the start.
     */
    @Nullable
    static List<BlockPos> findPath(Level level, BlockPos start, BlockPos goal) {
        double direct = Math.sqrt(start.distSqr(goal));
        if (direct > RIConfig.get(RIConfig.LEVITATOR_MAX_PATH)) {
            return null;
        }
        double limitSq = Math.max(4, direct * 2) * Math.max(4, direct * 2);
        Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        open.add(start);
        cameFrom.put(start, start);
        int budget = 50_000;
        while (!open.isEmpty() && budget-- > 0) {
            BlockPos node = open.poll();
            if (node.equals(goal)) {
                List<BlockPos> out = new ArrayList<>();
                for (BlockPos p = goal; !p.equals(start); p = cameFrom.get(p)) {
                    out.add(p);
                }
                out.add(start);
                java.util.Collections.reverse(out);
                return out;
            }
            for (Direction d : Direction.values()) {
                BlockPos next = node.relative(d);
                if (cameFrom.containsKey(next) || next.distSqr(start) > limitSq || !level.isLoaded(next)) {
                    continue;
                }
                if (next.equals(goal) || canBePath(level, next)) {
                    cameFrom.put(next, node);
                    open.add(next);
                }
            }
        }
        return null;
    }

    // ---- linking ----

    @Nullable
    private LevitatorBlockEntity partner() {
        if (link == null || !level.isLoaded(link)) {
            return null;
        }
        return level.getBlockEntity(link) instanceof LevitatorBlockEntity other && worldPosition.equals(other.link) ? other : null;
    }

    @Override
    public Linkable linkOwner() {
        return this;
    }

    @Override
    public boolean linkTo(GlobalPos target) {
        if (level == null || !target.dimension().equals(level.dimension()) || target.pos().equals(worldPosition)
                || Math.sqrt(target.pos().distSqr(worldPosition)) > RIConfig.get(RIConfig.LEVITATOR_MAX_PATH)) {
            return false;
        }
        if (!(level.getBlockEntity(target.pos()) instanceof LevitatorBlockEntity other)) {
            return false;
        }
        unlink();
        other.unlink();
        link = other.worldPosition;
        other.link = worldPosition;
        path = null;
        other.path = null;
        setChanged();
        other.setChanged();
        return true;
    }

    @Override
    public void unlink() {
        if (link != null && level != null && level.isLoaded(link) && level.getBlockEntity(link) instanceof LevitatorBlockEntity other
                && worldPosition.equals(other.link)) {
            other.link = null;
            other.path = null;
            other.setChanged();
        }
        link = null;
        path = null;
        setChanged();
    }

    @Nullable
    public BlockPos getLink() {
        return link;
    }

    // ---- settings ----

    public boolean isInput() {
        return input;
    }

    public boolean toggleMode() {
        input = !input;
        path = null;
        setChanged();
        return input;
    }

    public void setColor(DyeColor color) {
        this.color = color;
        setChanged();
    }

    public Component status() {
        Component mode = Component.translatable(input ? "message.resonantinduction.levitator.pull" : "message.resonantinduction.levitator.push");
        Component linkText = link == null ? Component.translatable("message.resonantinduction.tesla.unlinked")
                : Component.translatable("message.resonantinduction.levitator.linked_to", link.getX(), link.getY(), link.getZ());
        return Component.translatable("message.resonantinduction.levitator.status", mode, linkText);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("suck", input);
        tag.putString("color", color.getSerializedName());
        if (link != null) {
            tag.putLong("link", link.asLong());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        input = !tag.contains("suck") || tag.getBoolean("suck");
        color = DyeColor.byName(tag.getString("color"), TeslaBlockEntity.DEFAULT_COLOR);
        link = tag.contains("link") ? BlockPos.of(tag.getLong("link")) : null;
    }
}
