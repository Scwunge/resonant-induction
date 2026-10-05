package resonantinduction.tesla;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Every loaded Tesla coil on the server, per dimension, so towers can find each other without scanning blocks. */
public final class TeslaGrid {
    private static final Map<ResourceKey<Level>, Set<BlockPos>> COILS = new HashMap<>();

    private TeslaGrid() {}

    public static void add(Level level, BlockPos pos) {
        COILS.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(pos.immutable());
    }

    public static void remove(Level level, BlockPos pos) {
        Set<BlockPos> set = COILS.get(level.dimension());
        if (set != null) {
            set.remove(pos);
        }
    }

    public static Set<BlockPos> get(Level level) {
        return COILS.getOrDefault(level.dimension(), Collections.emptySet());
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        COILS.clear();
    }
}
