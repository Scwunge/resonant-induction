package resonantinduction.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;
import resonantinduction.RIConfig;
import resonantinduction.registry.RIRegistries;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Grate, as the original: under positive pressure (from a pump) it pours the fluid it's given out of its front face, filling the
 * space there block by block, lowest first; under negative pressure it drains the body of fluid in front of it, highest and
 * farthest first. How many blocks it works on each half second grows with the pressure. A wrench decides whether it may fill
 * higher than itself.
 */
public class GrateBlockEntity extends FluidNodeBlockEntity {
    private boolean fillOver = true;
    @Nullable
    private Pathfinder path;

    public GrateBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.GRATE_BE.get(), pos, state, 1000);
        node.maxFlowRate = tank.getCapacity();
    }

    public Direction facing() {
        return getBlockState().getValue(GrateBlock.FACING);
    }

    public String debug() {
        return "pressure=" + node.getPressure(facing()) + " tank=" + tank.getFluidAmount() + "/" + tank.getCapacity() + " connections=" + node.connections().keySet()
                + " path=" + (path != null);
    }

    public boolean fillOver() {
        return fillOver;
    }

    public void toggleFillOver() {
        fillOver = !fillOver;
        path = null;
        setChanged();
    }

    @Override
    protected boolean canFill(@Nullable Direction side, FluidStack stack) {
        return side != facing();
    }

    @Override
    protected boolean canDrain(@Nullable Direction side) {
        return side != facing();
    }

    @Override
    protected void tickServer(ServerLevel level) {
        super.tickServer(level);
        if (level.getGameTime() % 10 != 0) {
            return;
        }
        int pressure = node.getPressure(facing());
        int blockEffect = (int) Math.abs(pressure * RIConfig.get(RIConfig.GRATE_EFFECT));
        tank.setCapacity((int) Math.max(blockEffect * 1000 * RIConfig.get(RIConfig.GRATE_DRAIN_SPEED), 1000));
        node.maxFlowRate = tank.getCapacity();
        if (pressure > 0) {
            if (tank.getFluidAmount() >= 1000) {
                if (path == null) {
                    path = new Pathfinder(level, true);
                    path.startFill(tank.getFluid().getFluid());
                }
                int filled = path.tryFill(tank.getFluid(), blockEffect);
                if (filled > 0) {
                    tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                    onFluidChanged();
                }
            }
        } else if (pressure < 0) {
            int room = tank.getCapacity() - tank.getFluidAmount();
            if (room > 0) {
                if (path == null) {
                    path = new Pathfinder(level, false);
                    if (!path.startDrain()) {
                        path = null;
                    }
                }
                if (path != null && path.populateDrain(blockEffect)) {
                    FluidStack drained = path.tryDrain(room);
                    if (!drained.isEmpty()) {
                        tank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                        onFluidChanged();
                    }
                }
            }
        }
    }

    private record Node(BlockPos pos, int iterations) {}

    /** The original GratePathfinder: a flood out from the face, through blocks of the same fluid. */
    private final class Pathfinder {
        private final ServerLevel level;
        private final BlockPos start = worldPosition;
        private final Map<BlockPos, BlockPos> navigation = new HashMap<>();
        private final PriorityQueue<Node> working;
        /** Highest first, then farthest. */
        private final PriorityQueue<Node> drainNodes = new PriorityQueue<>(Collections.reverseOrder(BY_HEIGHT));
        private Fluid fluid = Fluids.EMPTY;

        private static final Comparator<Node> BY_HEIGHT = Comparator.<Node>comparingInt(n -> n.pos.getY()).thenComparingInt(Node::iterations);

        Pathfinder(ServerLevel level, boolean byHeight) {
            this.level = level;
            this.working = byHeight ? new PriorityQueue<>(BY_HEIGHT) : new PriorityQueue<>(Comparator.comparingInt(Node::iterations));
        }

        /** Still joined to the grate through blocks of the fluid? */
        boolean isConnected(BlockPos check) {
            if (check.equals(start)) {
                return true;
            }
            do {
                check = navigation.get(check);
                if (check == null) {
                    return false;
                }
                if (check.equals(start)) {
                    return true;
                }
            } while (WorldFluids.fluidAt(level, check).isSame(fluid));
            return false;
        }

        void startFill(Fluid type) {
            fluid = type;
            BlockPos front = start.relative(facing());
            navigation.put(front, start);
            working.add(new Node(front, 0));
        }

        int tryFill(FluidStack stack, int tries) {
            int filled = 0;
            int amount = stack.getAmount();
            for (int i = 0; i < tries && amount - filled >= 1000; i++) {
                Node next = working.poll();
                if (next == null || !isConnected(next.pos)) {
                    path = null;
                    return filled;
                }
                int did = WorldFluids.fill(level, next.pos, stack.copyWithAmount(amount - filled), true);
                filled += did;
                if (did > 0 || WorldFluids.fluidAt(level, next.pos).isSame(fluid)) {
                    for (Direction d : Direction.values()) {
                        BlockPos np = next.pos.relative(d);
                        if (!navigation.containsKey(np) && (fillOver || np.getY() <= start.getY()) && !level.isOutsideBuildHeight(np)) {
                            navigation.put(np, next.pos);
                            working.add(new Node(np, next.iterations + 1));
                        }
                    }
                }
            }
            return filled;
        }

        boolean startDrain() {
            BlockPos front = start.relative(facing());
            navigation.put(front, start);
            working.add(new Node(front, 0));
            fluid = WorldFluids.fluidAt(level, front);
            return fluid != Fluids.EMPTY;
        }

        boolean populateDrain(int tries) {
            for (int i = 0; i < tries; i++) {
                Node check = working.poll();
                if (check == null) {
                    return true;
                }
                if (WorldFluids.fluidAt(level, check.pos).isSame(fluid)) {
                    for (Direction d : Direction.values()) {
                        BlockPos np = check.pos.relative(d);
                        if (!navigation.containsKey(np) && WorldFluids.fluidAt(level, np).isSame(fluid)) {
                            navigation.put(np, check.pos);
                            working.add(new Node(np, check.iterations + 1));
                        }
                    }
                    if (!WorldFluids.peek(level, check.pos).isEmpty()) {
                        drainNodes.add(check);
                    }
                }
            }
            return !drainNodes.isEmpty();
        }

        FluidStack tryDrain(int target) {
            FluidStack out = FluidStack.EMPTY;
            while (!drainNodes.isEmpty()) {
                Node at = drainNodes.peek();
                if (!isConnected(at.pos)) {
                    break;
                }
                FluidStack there = WorldFluids.peek(level, at.pos);
                if (there.isEmpty() || !there.getFluid().isSame(fluid)) {
                    drainNodes.poll();
                    continue;
                }
                if (!out.isEmpty() && !FluidStack.isSameFluidSameComponents(out, there)) {
                    drainNodes.poll();
                    continue;
                }
                if (out.getAmount() + there.getAmount() > target) {
                    break;
                }
                FluidStack got = WorldFluids.drain(level, at.pos, true);
                drainNodes.poll();
                out = out.isEmpty() ? got.copy() : out.copyWithAmount(out.getAmount() + got.getAmount());
                if (out.getAmount() >= target) {
                    break;
                }
            }
            path = null;
            return out;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("fillOver", fillOver);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fillOver = !tag.contains("fillOver") || tag.getBoolean("fillOver");
    }
}
