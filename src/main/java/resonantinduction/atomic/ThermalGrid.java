package resonantinduction.atomic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import resonantinduction.ResonantInduction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import resonantinduction.RIConfig;
import resonantinduction.registry.RIRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Block temperatures, after the thermal grid of the original's successor: heat added at a block spreads to its neighbours (hot
 * to cold) and leaks away toward the surroundings. Water touching anything at 373 K or more boils: the steam rises to a funnel or
 * turbine up to two blocks above it, and carries heat away. Only blocks warmer than their surroundings are tracked.
 */
public final class ThermalGrid {
    public static final float AMBIENT = 295;
    /** Share of the temperature difference that flows to each neighbour a tick. */
    private static final float SPREAD = 0.02f;
    /** Share of the excess heat lost to the surroundings a tick (electromagnets shed far more, as in the original). */
    private static final float LOSS = 0.02f;
    private static final float MAGNET_LOSS = 0.6f;
    /** Blocks that shed heat fast: the electromagnets. */
    public static final TagKey<Block> HEAT_SINKS = TagKey.create(Registries.BLOCK, ResonantInduction.id("heat_sinks"));
    /** Steam boiled a tick by one water block, per kelvin over boiling. */
    private static final float STEAM_PER_KELVIN = 0.5f;
    /** Kelvin of heat each boiling water block draws off a tick, per kelvin over boiling. */
    private static final float BOIL_COOLING = 0.01f;

    private static final Map<Level, ThermalGrid> GRIDS = new WeakHashMap<>();

    private final Map<BlockPos, Float> excess = new HashMap<>();

    public static ThermalGrid get(Level level) {
        return GRIDS.computeIfAbsent(level, l -> new ThermalGrid());
    }

    public static float temperature(Level level, BlockPos pos) {
        return AMBIENT + get(level).excess.getOrDefault(pos, 0f);
    }

    public static void addTemperature(Level level, BlockPos pos, float delta) {
        ThermalGrid grid = get(level);
        float now = grid.excess.getOrDefault(pos, 0f) + delta;
        if (now > 0.01f) {
            grid.excess.put(pos.immutable(), now);
        } else {
            grid.excess.remove(pos);
        }
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ThermalGrid grid = GRIDS.get(level);
            if (grid != null && !grid.excess.isEmpty()) {
                grid.tick(level);
            }
        }
    }

    private void tick(ServerLevel level) {
        Map<BlockPos, Float> next = new HashMap<>();
        for (Map.Entry<BlockPos, Float> e : excess.entrySet()) {
            BlockPos pos = e.getKey();
            float heat = e.getValue();
            if (!level.isLoaded(pos)) {
                continue;
            }
            float t = AMBIENT + heat;
            float out = 0;
            for (Direction d : Direction.values()) {
                BlockPos adj = pos.relative(d);
                float adjHeat = excess.getOrDefault(adj, 0f);
                if (heat > adjHeat) {
                    float flow = (heat - adjHeat) * SPREAD;
                    next.merge(adj.immutable(), flow, Float::sum);
                    out += flow;
                }
                if (t >= 373 && level.getFluidState(adj).is(Fluids.WATER) && level.getFluidState(adj).isSource()) {
                    boil(level, adj, t);
                    out += BOIL_COOLING * (t - 373);
                }
            }
            float loss = level.getBlockState(pos).is(HEAT_SINKS) ? MAGNET_LOSS : LOSS;
            next.merge(pos, heat - out - heat * loss, Float::sum);
        }
        excess.clear();
        for (Map.Entry<BlockPos, Float> e : next.entrySet()) {
            if (e.getValue() > 0.5f) {
                excess.put(e.getKey(), e.getValue());
            }
        }
    }

    /** Water at {@code water} boils; the steam goes into a funnel or turbine up to two blocks above it. */
    private static void boil(ServerLevel level, BlockPos water, float temperature) {
        int steam = (int) (STEAM_PER_KELVIN * (temperature - 373) * RIConfig.get(RIConfig.FISSION_BOIL_MULTIPLIER));
        if (steam <= 0) {
            return;
        }
        for (int up = 1; up <= 2; up++) {
            IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, water.above(up), Direction.DOWN);
            if (handler != null && handler.fill(new FluidStack(RIRegistries.STEAM.get(), steam), IFluidHandler.FluidAction.EXECUTE) > 0) {
                return;
            }
        }
    }

    private ThermalGrid() {}
}
