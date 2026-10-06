package resonantinduction.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * A set of connected wires. Energy pushed into any wire is shared out to every machine the network touches (except the
 * one it came from), limited per tick by the capacity of the weakest wire, like Universal Electricity's networks.
 * Networks are built lazily and thrown away whenever a wire or its surroundings change.
 */
public final class WireNetwork {
    private static final Map<Level, Map<BlockPos, WireNetwork>> NETWORKS = new WeakHashMap<>();

    private final Set<BlockPos> members;
    private final int capacity;
    private List<Acceptor> acceptors;
    private long tick = -1;
    private int transferred;
    private long lastActive = -100;

    private record Acceptor(BlockPos pos, Direction side) {}

    private WireNetwork(Set<BlockPos> members, int capacity) {
        this.members = members;
        this.capacity = capacity;
    }

    public static WireNetwork get(Level level, BlockPos pos) {
        Map<BlockPos, WireNetwork> map = NETWORKS.computeIfAbsent(level, k -> new HashMap<>());
        WireNetwork network = map.get(pos);
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
        Map<BlockPos, WireNetwork> map = NETWORKS.get(level);
        if (map == null) {
            return;
        }
        drop(map, pos);
        for (Direction d : Direction.values()) {
            drop(map, pos.relative(d));
        }
    }

    private static void drop(Map<BlockPos, WireNetwork> map, BlockPos pos) {
        WireNetwork network = map.remove(pos);
        if (network != null) {
            for (BlockPos member : network.members) {
                map.remove(member);
            }
        }
    }

    private static WireNetwork build(Level level, BlockPos start) {
        Set<BlockPos> members = new HashSet<>();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        open.add(start);
        int capacity = Integer.MAX_VALUE;
        while (!open.isEmpty()) {
            BlockPos pos = open.poll();
            if (!members.add(pos) || !(level.getBlockEntity(pos) instanceof WireBlockEntity wire)) {
                continue;
            }
            capacity = Math.min(capacity, wire.material().capacity());
            for (Direction d : Direction.values()) {
                BlockPos next = pos.relative(d);
                if (!members.contains(next) && level.isLoaded(next) && WireBlock.wiresConnect(level, pos, d)) {
                    open.add(next);
                }
            }
        }
        return new WireNetwork(members, capacity == Integer.MAX_VALUE ? 0 : capacity);
    }

    private List<Acceptor> acceptors(Level level) {
        if (acceptors == null) {
            acceptors = new ArrayList<>();
            for (BlockPos pos : members) {
                for (Direction d : Direction.values()) {
                    if (WireBlock.connectsToMachine(level, pos, d)) {
                        acceptors.add(new Acceptor(pos.relative(d), d.getOpposite()));
                    }
                }
            }
        }
        return acceptors;
    }

    /** Shares {@code amount} out among the network's machines, never back into {@code source}. */
    public int distribute(Level level, BlockPos source, int amount, boolean simulate) {
        long now = level.getGameTime();
        if (now != tick) {
            tick = now;
            transferred = 0;
        }
        int budget = Math.min(amount, capacity - transferred);
        if (budget <= 0) {
            return 0;
        }
        List<IEnergyStorage> targets = new ArrayList<>();
        for (Acceptor a : acceptors(level)) {
            if (a.pos.equals(source) || !level.isLoaded(a.pos)) {
                continue;
            }
            IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, a.pos, a.side);
            if (storage != null && storage.canReceive() && !targets.contains(storage)) {
                targets.add(storage);
            }
        }
        int remaining = budget;
        // Even split first, then hand leftovers to whoever still has room.
        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            for (int i = 0; i < targets.size() && remaining > 0; i++) {
                int share = pass == 0 ? Math.max(1, remaining / (targets.size() - i)) : remaining;
                remaining -= targets.get(i).receiveEnergy(Math.min(share, remaining), simulate);
            }
        }
        int sent = budget - remaining;
        if (!simulate && sent > 0) {
            transferred += sent;
            lastActive = now;
        }
        return sent;
    }

    /** Carried power in the last second (bare wires shock while live). */
    public boolean isLive(Level level) {
        return level.getGameTime() - lastActive < 20;
    }

    public int capacity() {
        return capacity;
    }

    public int transferredThisTick(Level level) {
        return level.getGameTime() == tick ? transferred : 0;
    }

    public int size() {
        return members.size();
    }
}
