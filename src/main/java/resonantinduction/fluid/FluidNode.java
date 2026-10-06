package resonantinduction.fluid;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.EnumMap;
import java.util.Map;

/**
 * The original FluidPressureNode. Each block with one keeps a small tank and a pressure; every tick it takes on the average pressure
 * of the nodes around it (losing one on the way), then pushes fluid from higher pressure to lower: {@code (difference x flow rate)}
 * mB, or, at equal pressure, half the difference in fill, so connected tanks level out. Next to a plain fluid container (not a
 * node), it pushes into it at positive pressure and pulls out of it at negative pressure.
 */
public class FluidNode {
    /** The block that owns a node: where its fluid lives. */
    public interface Host {
        FluidTank pressureTank();

        /** Called after fluid has moved in or out, for syncing. */
        void onFluidChanged();
    }

    protected final Host host;
    public int maxFlowRate = 20;
    public int maxPressure = 100;
    private int pressure;
    /** Neighbours by direction: another {@link FluidNode}, or a {@link BlockCapabilityCache} of a plain container. */
    private final Map<Direction, Object> connections = new EnumMap<>(Direction.class);

    public FluidNode(Host host) {
        this.host = host;
    }

    public Host host() {
        return host;
    }

    public Map<Direction, Object> connections() {
        return connections;
    }

    public boolean connected(Direction dir) {
        return connections.containsKey(dir);
    }

    /** Pressure seen from {@code dir}. */
    public int getPressure(Direction dir) {
        return pressure;
    }

    public int getMaxFlowRate() {
        return maxFlowRate;
    }

    /** May this node connect toward {@code from} to {@code other}: a node, or (for some blocks) a plain container? */
    public boolean canConnect(Direction from, Object other) {
        return other instanceof FluidNode;
    }

    public void setPressure(int value) {
        pressure = value > 0 ? Math.min(maxPressure, value) : Math.max(-maxPressure, value);
    }

    public void update() {
        updatePressure();
        distribute();
    }

    protected void updatePressure() {
        int total = 0;
        int count = 0;
        int min = 0;
        int max = 0;
        for (Map.Entry<Direction, Object> e : connections.entrySet()) {
            if (e.getValue() instanceof FluidNode other) {
                int p = other.getPressure(e.getKey().getOpposite());
                min = Math.min(p, min);
                max = Math.max(p, max);
                total += p;
                count++;
            }
        }
        if (count == 0) {
            setPressure(0);
            return;
        }
        // Pressure is lost along the way.
        if (min < 0) {
            min++;
        }
        if (max > 0) {
            max--;
        }
        setPressure(Math.max(min, Math.min(max, total / count + Integer.signum(total))));
    }

    @SuppressWarnings("unchecked")
    protected void distribute() {
        FluidTank tankA = host.pressureTank();
        for (Map.Entry<Direction, Object> e : connections.entrySet()) {
            Direction dir = e.getKey();
            if (e.getValue() instanceof FluidNode other) {
                int pressureA = getPressure(dir);
                int pressureB = other.getPressure(dir.getOpposite());
                if (pressureA < pressureB || tankA.isEmpty()) {
                    continue;
                }
                FluidTank tankB = other.host.pressureTank();
                int amountA = tankA.getFluidAmount();
                int amountB = tankB.getFluidAmount();
                int quantity = Math.max(pressureA > pressureB ? (pressureA - pressureB) * getMaxFlowRate() : 0, Math.min((amountA - amountB) / 2, getMaxFlowRate()));
                quantity = Math.min(Math.min(quantity, tankB.getCapacity() - amountB), amountA);
                if (quantity > 0) {
                    move(tankA, tankB, quantity);
                    host.onFluidChanged();
                    other.host.onFluidChanged();
                }
            } else if (e.getValue() instanceof BlockCapabilityCache<?, ?> cache) {
                IFluidHandler handler = ((BlockCapabilityCache<IFluidHandler, Direction>) cache).getCapability();
                if (handler == null) {
                    continue;
                }
                int p = getPressure(dir);
                int transfer = Math.abs(p) * getMaxFlowRate();
                if (transfer <= 0) {
                    continue;
                }
                if (p > 0 && !tankA.isEmpty()) {
                    FluidStack offer = tankA.drain(transfer, IFluidHandler.FluidAction.SIMULATE);
                    int filled = handler.fill(offer, IFluidHandler.FluidAction.EXECUTE);
                    if (filled > 0) {
                        tankA.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                        host.onFluidChanged();
                    }
                } else if (p < 0) {
                    FluidStack offer = tankA.isEmpty() ? handler.drain(transfer, IFluidHandler.FluidAction.SIMULATE)
                            : handler.drain(tankA.getFluid().copyWithAmount(transfer), IFluidHandler.FluidAction.SIMULATE);
                    if (!offer.isEmpty()) {
                        int filled = tankA.fill(offer, IFluidHandler.FluidAction.EXECUTE);
                        if (filled > 0) {
                            handler.drain(offer.copyWithAmount(filled), IFluidHandler.FluidAction.EXECUTE);
                            host.onFluidChanged();
                        }
                    }
                }
            }
        }
    }

    /** Moves up to {@code amount} from one tank to another (if the second takes that fluid). */
    static int move(FluidTank from, FluidTank to, int amount) {
        FluidStack offer = from.drain(amount, IFluidHandler.FluidAction.SIMULATE);
        int filled = to.fill(offer, IFluidHandler.FluidAction.EXECUTE);
        if (filled > 0) {
            from.drain(filled, IFluidHandler.FluidAction.EXECUTE);
        }
        return filled;
    }

    public void save(CompoundTag tag) {
        tag.putInt("pressure", pressure);
    }

    public void load(CompoundTag tag) {
        pressure = tag.getInt("pressure");
    }
}
