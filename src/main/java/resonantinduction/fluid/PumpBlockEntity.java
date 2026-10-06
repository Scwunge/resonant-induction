package resonantinduction.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;
import resonantinduction.mechanical.MechanicalBlockEntity;
import resonantinduction.mechanical.process.MachineBlock;
import resonantinduction.registry.RIRegistries;


/**
 * Pump, as the original. Turned from any side, it draws fluid in at the back and pushes it out of the front, as fast as
 * {@code speed x 1000} mB a tick; and it pressurises the pipes on both ends (out the front, suction at the back) by its torque
 * over 8000, at least 2.
 */
public class PumpBlockEntity extends MechanicalBlockEntity implements FluidNodeProvider, FluidNode.Host {
    private static final FluidTank NO_TANK = new FluidTank(0);

    private final FluidNode pressureNode = new FluidNode(this) {
        @Override
        public int getPressure(Direction dir) {
            if (node.getPower() > 0) {
                int p = (int) Math.max(Math.abs(node.getTorque() / 8000d), 2);
                if (dir == facing()) {
                    return p;
                } else if (dir == facing().getOpposite()) {
                    return -p;
                }
            }
            return 0;
        }

        @Override
        public int getMaxFlowRate() {
            return (int) Math.abs(node.getAngularVelocity() * 1000);
        }

        @Override
        public boolean canConnect(Direction from, Object other) {
            return super.canConnect(from, other) && from.getAxis() == facing().getAxis();
        }
    };
    private boolean fluidRecache = true;

    public PumpBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.PUMP_BE.get(), pos, state);
    }

    public Direction facing() {
        return getBlockState().getValue(MachineBlock.FACING);
    }

    @Override
    protected boolean canMesh(Direction dir, MechanicalBlockEntity other) {
        return true;
    }

    @Override
    @Nullable
    public FluidNode getFluidNode(@Nullable Direction from) {
        return pressureNode;
    }

    public FluidNode pressureNode() {
        return pressureNode;
    }

    @Override
    public FluidTank pressureTank() {
        return NO_TANK;
    }

    @Override
    public void onFluidChanged() {}

    @Override
    public void markRecache() {
        super.markRecache();
        fluidRecache = true;
    }

    @Override
    protected void tickServer() {
        super.tickServer();
        ServerLevel server = (ServerLevel) level;
        if (fluidRecache || server.getGameTime() % 40 == 0) {
            fluidRecache = false;
            pressureNode.connections().clear();
            for (Direction dir : new Direction[] {facing(), facing().getOpposite()}) {
                if (server.getBlockEntity(worldPosition.relative(dir)) instanceof FluidNodeProvider provider) {
                    FluidNode other = provider.getFluidNode(dir.getOpposite());
                    if (other != null && pressureNode.canConnect(dir, other) && other.canConnect(dir.getOpposite(), pressureNode)) {
                        pressureNode.connections().put(dir, other);
                    }
                }
            }
        }
        if (node.getPower() <= 0) {
            return;
        }
        // Draw from whatever is behind and pour it into whatever is in front.
        Direction out = facing();
        IFluidHandler in = server.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(out.getOpposite()), out);
        IFluidHandler to = server.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(out), out.getOpposite());
        int rate = pressureNode.getMaxFlowRate();
        if (in == null || to == null || rate <= 0) {
            return;
        }
        FluidStack offer = in.drain(rate, IFluidHandler.FluidAction.SIMULATE);
        if (!offer.isEmpty()) {
            int filled = to.fill(offer, IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) {
                in.drain(offer.copyWithAmount(filled), IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    /** Fluid poured in at the back goes straight through to the front; nothing can be drained from it. */
    @Nullable
    public IFluidHandler getFluidCapability(@Nullable Direction side) {
        if (side != facing().getOpposite()) {
            return null;
        }
        return new IFluidHandler() {
            private IFluidHandler out() {
                return level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(facing()), facing().getOpposite());
            }

            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int t) {
                return FluidStack.EMPTY;
            }

            @Override
            public int getTankCapacity(int t) {
                return 0;
            }

            @Override
            public boolean isFluidValid(int t, FluidStack stack) {
                return true;
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                IFluidHandler out = out();
                return out == null ? 0 : out.fill(resource, action);
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return FluidStack.EMPTY;
            }
        };
    }

    @Override
    public double probePressure(Direction side) {
        return pressureNode.getPressure(side);
    }
}
