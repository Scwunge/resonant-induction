package resonantinduction.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;
import resonantinduction.registry.RIRegistries;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Gutter, as the original: an open channel holding a bucket. Fluid falls through gutters (pressure 2 downward, -2 upward, so it
 * pours into whatever is below and draws from whatever is above), and levels out sideways. Once a second it drinks a fluid block
 * or pool sitting on it, if the connected gutters have room, and it collects rain.
 */
public class GutterBlockEntity extends FluidNodeBlockEntity {
    public GutterBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.GUTTER_BE.get(), pos, state, 1000);
    }

    @Override
    protected FluidNode createNode() {
        return new FluidNode(this) {
            @Override
            public int getPressure(Direction dir) {
                return dir == Direction.UP ? -2 : dir == Direction.DOWN ? 2 : 0;
            }

            @Override
            public boolean canConnect(Direction from, Object other) {
                return other instanceof FluidNode || other instanceof IFluidHandler;
            }
        };
    }

    /** Gases don't stay in an open gutter; nothing goes in or out through the open top. */
    @Override
    protected boolean canFill(@Nullable Direction side, FluidStack stack) {
        return side != Direction.UP && !stack.getFluidType().isLighterThanAir();
    }

    @Override
    protected boolean canDrain(@Nullable Direction side) {
        return side != Direction.UP;
    }

    /** This gutter and every gutter connected to it. */
    public List<GutterBlockEntity> grid() {
        List<GutterBlockEntity> out = new ArrayList<>();
        Set<GutterBlockEntity> seen = new HashSet<>();
        ArrayDeque<GutterBlockEntity> queue = new ArrayDeque<>();
        queue.add(this);
        seen.add(this);
        while (!queue.isEmpty() && out.size() < 4096) {
            GutterBlockEntity g = queue.poll();
            out.add(g);
            for (Object o : g.node.connections().values()) {
                if (o instanceof FluidNode n && n.host() instanceof GutterBlockEntity other && seen.add(other)) {
                    queue.add(other);
                }
            }
        }
        return out;
    }

    /** All the gutters' tanks as one container, for buckets and for what falls in. */
    public IFluidHandler gridHandler() {
        List<GutterBlockEntity> grid = grid();
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return grid.size();
            }

            @Override
            public FluidStack getFluidInTank(int t) {
                return grid.get(t).tank.getFluid();
            }

            @Override
            public int getTankCapacity(int t) {
                return grid.get(t).tank.getCapacity();
            }

            @Override
            public boolean isFluidValid(int t, FluidStack stack) {
                return true;
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (resource.getFluidType().isLighterThanAir()) {
                    return 0;
                }
                int left = resource.getAmount();
                for (GutterBlockEntity g : grid) {
                    if (left <= 0) {
                        break;
                    }
                    int filled = g.tank.fill(resource.copyWithAmount(left), action);
                    if (filled > 0 && action.execute()) {
                        g.onFluidChanged();
                    }
                    left -= filled;
                }
                return resource.getAmount() - left;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return drainMatching(resource, resource.getAmount(), action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                for (GutterBlockEntity g : grid) {
                    if (!g.tank.isEmpty()) {
                        return drainMatching(g.tank.getFluid(), maxDrain, action);
                    }
                }
                return FluidStack.EMPTY;
            }

            private FluidStack drainMatching(FluidStack like, int amount, FluidAction action) {
                int got = 0;
                for (GutterBlockEntity g : grid) {
                    if (got >= amount) {
                        break;
                    }
                    if (FluidStack.isSameFluidSameComponents(g.tank.getFluid(), like)) {
                        int taken = g.tank.drain(amount - got, action).getAmount();
                        if (taken > 0 && action.execute()) {
                            g.onFluidChanged();
                        }
                        got += taken;
                    }
                }
                return got == 0 ? FluidStack.EMPTY : like.copyWithAmount(got);
            }
        };
    }

    @Override
    protected void tickServer(ServerLevel level) {
        super.tickServer(level);
        if (level.getGameTime() % 20 == 0) {
            BlockPos above = worldPosition.above();
            FluidStack inBlock = WorldFluids.peek(level, above);
            if (!inBlock.isEmpty()) {
                IFluidHandler grid = gridHandler();
                if (grid.fill(inBlock, IFluidHandler.FluidAction.SIMULATE) >= inBlock.getAmount()) {
                    grid.fill(WorldFluids.drain(level, above, true), IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }

    /** Rain falling in: 10 mB a time, as the original. */
    public void fillRain() {
        if (tank.fill(new FluidStack(Fluids.WATER, 10), IFluidHandler.FluidAction.EXECUTE) > 0) {
            onFluidChanged();
        }
    }
}
