package resonantinduction.quantum;

import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Stops entities bouncing straight back: after a teleport an entity ignores the gate it arrived on until it has stepped off,
 * and nothing teleports more than once a second (the original's player cooldown).
 */
public final class QuantumTeleports {
    private static final int COOLDOWN_TICKS = 20;
    private static final Map<UUID, Long> LAST = new HashMap<>();
    private static final Map<UUID, GlobalPos> ARRIVED_ON = new HashMap<>();

    private QuantumTeleports() {}

    static boolean canTeleport(Entity entity, GlobalPos gate) {
        if (gate.equals(ARRIVED_ON.get(entity.getUUID()))) {
            return false;
        }
        Long last = LAST.get(entity.getUUID());
        return last == null || entity.level().getGameTime() - last >= COOLDOWN_TICKS;
    }

    static void arrived(Entity entity, GlobalPos gate) {
        LAST.put(entity.getUUID(), entity.level().getGameTime());
        ARRIVED_ON.put(entity.getUUID(), gate);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0 || ARRIVED_ON.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, GlobalPos>> it = ARRIVED_ON.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, GlobalPos> e = it.next();
            ServerLevel level = server.getLevel(e.getValue().dimension());
            Entity entity = level == null ? null : level.getEntity(e.getKey());
            // Forget the arrival gate once the entity has left the space above it.
            AABB above = new AABB(e.getValue().pos()).move(0, 1, 0).expandTowards(0, 2, 0).inflate(0.5, 0, 0.5);
            if (entity == null || !entity.isAlive() || !entity.getBoundingBox().intersects(above)) {
                it.remove();
            }
        }
        if (LAST.size() > 1024) {
            long now = server.overworld().getGameTime();
            LAST.values().removeIf(t -> now - t > COOLDOWN_TICKS);
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        LAST.clear();
        ARRIVED_ON.clear();
    }
}
