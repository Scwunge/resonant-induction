package resonantinduction.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Tanks that touch share their fluid, as the original TankNetwork: what goes in settles from the bottom up (each layer of tanks
 * filling evenly before the one above it), and comes out from the top.
 */
public final class TankNetwork implements IFluidHandler {
    private static final Map<Level, Map<BlockPos, TankNetwork>> NETWORKS = new WeakHashMap<>();

    private final Level level;
    private final List<BlockPos> members;

    private TankNetwork(Level level, List<BlockPos> members) {
        this.level = level;
        this.members = members;
    }

    public static TankNetwork get(Level level, BlockPos pos) {
        Map<BlockPos, TankNetwork> map = NETWORKS.computeIfAbsent(level, k -> new HashMap<>());
        TankNetwork network = map.get(pos);
        if (network == null) {
            network = build(level, pos);
            for (BlockPos member : network.members) {
                map.put(member, network);
            }
        }
        return network;
    }

    /** Forget the networks at and around {@code pos}; they are rebuilt on next use. */
    public static void invalidate(Level level, BlockPos pos) {
        Map<BlockPos, TankNetwork> map = NETWORKS.get(level);
        if (map == null) {
            return;
        }
        drop(map, pos);
        for (Direction d : Direction.values()) {
            drop(map, pos.relative(d));
        }
    }

    private static void drop(Map<BlockPos, TankNetwork> map, BlockPos pos) {
        TankNetwork network = map.remove(pos);
        if (network != null) {
            for (BlockPos member : network.members) {
                map.remove(member);
            }
        }
    }

    /** Joins touching tanks, leaving out any holding a different fluid from the rest. */
    private static TankNetwork build(Level level, BlockPos start) {
        List<BlockPos> members = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        open.add(start);
        seen.add(start);
        FluidStack fluid = FluidStack.EMPTY;
        while (!open.isEmpty()) {
            BlockPos pos = open.poll();
            if (!(level.getBlockEntity(pos) instanceof TankBlockEntity tank)) {
                continue;
            }
            FluidStack own = tank.tank().getFluid();
            if (!own.isEmpty()) {
                if (fluid.isEmpty()) {
                    fluid = own;
                } else if (!FluidStack.isSameFluidSameComponents(fluid, own) && !pos.equals(start)) {
                    continue;
                }
            }
            members.add(pos);
            for (Direction d : Direction.values()) {
                BlockPos next = pos.relative(d);
                if (level.isLoaded(next) && seen.add(next) && level.getBlockEntity(next) instanceof TankBlockEntity) {
                    open.add(next);
                }
            }
        }
        members.sort(Comparator.comparingInt(BlockPos::getY));
        return new TankNetwork(level, members);
    }

    private List<TankBlockEntity> tanks() {
        List<TankBlockEntity> out = new ArrayList<>(members.size());
        for (BlockPos pos : members) {
            if (level.getBlockEntity(pos) instanceof TankBlockEntity tank) {
                out.add(tank);
            }
        }
        return out;
    }

    public FluidStack fluid() {
        for (TankBlockEntity t : tanks()) {
            if (!t.tank().isEmpty()) {
                return t.tank().getFluid().copy();
            }
        }
        return FluidStack.EMPTY;
    }

    public int amount() {
        int total = 0;
        for (TankBlockEntity t : tanks()) {
            total += t.tank().getFluidAmount();
        }
        return total;
    }

    public int capacity() {
        return tanks().size() * TankBlockEntity.CAPACITY;
    }

    /** Puts {@code total} of {@code fluid} back into the tanks, bottom layer first (top first for a gas), evenly within a layer. */
    private void settle(FluidStack fluid, int total) {
        List<TankBlockEntity> tanks = tanks();
        if (fluid.getFluidType().isLighterThanAir()) {
            java.util.Collections.reverse(tanks);
        }
        int i = 0;
        while (i < tanks.size()) {
            int y = tanks.get(i).getBlockPos().getY();
            int j = i;
            while (j < tanks.size() && tanks.get(j).getBlockPos().getY() == y) {
                j++;
            }
            int count = j - i;
            int layer = Math.min(total, count * TankBlockEntity.CAPACITY);
            for (int k = i; k < j; k++) {
                int share = layer / count + (k - i < layer % count ? 1 : 0);
                tanks.get(k).setContents(share > 0 ? fluid.copyWithAmount(share) : FluidStack.EMPTY);
            }
            total -= layer;
            i = j;
        }
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int t) {
        FluidStack fluid = fluid();
        return fluid.isEmpty() ? FluidStack.EMPTY : fluid.copyWithAmount(amount());
    }

    @Override
    public int getTankCapacity(int t) {
        return capacity();
    }

    @Override
    public boolean isFluidValid(int t, FluidStack stack) {
        FluidStack fluid = fluid();
        return fluid.isEmpty() || FluidStack.isSameFluidSameComponents(fluid, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(0, resource)) {
            return 0;
        }
        int amount = amount();
        int filled = Math.min(resource.getAmount(), capacity() - amount);
        if (filled > 0 && action.execute()) {
            settle(resource, amount + filled);
        }
        return Math.max(filled, 0);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(resource, fluid())) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        FluidStack fluid = fluid();
        if (fluid.isEmpty() || maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        int amount = amount();
        int drained = Math.min(maxDrain, amount);
        if (action.execute()) {
            settle(fluid, amount - drained);
        }
        return fluid.copyWithAmount(drained);
    }
}
