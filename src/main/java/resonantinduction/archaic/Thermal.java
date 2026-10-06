package resonantinduction.archaic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** The original ThermalPhysics numbers for melting iron and boiling water, with the local temperature from the biome. */
public final class Thermal {
    private Thermal() {}

    /** Kelvin, roughly: 0.8 biome temperature (temperate) is 295 K. */
    public static float temperature(Level level, BlockPos pos) {
        return 273 + level.getBiome(pos).value().getBaseTemperature() * 27.5f;
    }

    /** The original's mass for {@code volume} millibuckets at {@code density} (g/cm^3): a bucket of iron is 7.9. */
    static float mass(float volume, float density) {
        return volume / 1000f * density;
    }

    /** Joules to heat and melt {@code volume} millibuckets of iron. */
    public static long meltEnergy(Level level, BlockPos pos, float volume) {
        float mass = mass(volume, 7.9f);
        float delta = 1811 - temperature(level, pos);
        return (long) (mass * 450 * delta + mass * 272000);
    }

    /** Joules to heat and boil {@code volume} millibuckets of water. */
    public static long boilEnergy(Level level, BlockPos pos, int volume) {
        float mass = mass(volume, 1f);
        float delta = 373 - temperature(level, pos);
        return (long) (mass * 4186 * delta + mass * 2260000);
    }
}
