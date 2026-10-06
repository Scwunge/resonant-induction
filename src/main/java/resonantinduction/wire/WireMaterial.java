package resonantinduction.wire;

import net.minecraft.util.StringRepresentable;
import resonantinduction.RIConfig;

import java.util.Locale;

/** Wire metals with the original numbers. Capacity in FE per tick is maxAmps times the configured multiplier. */
public enum WireMaterial implements StringRepresentable {
    /** General purpose. */
    COPPER(1.68f, 5, 200, 0xB87333),
    /** Low shock, cheap. */
    TIN(3.1f, 1, 100, 0x848482),
    /** High capacity. */
    IRON(3f, 3, 800, 0x616669),
    /** High shock. */
    ALUMINUM(2.6f, 10, 600, 0xD7CDB5),
    /** Low resistance. */
    SILVER(1.59f, 5, 700, 0xC0C0C0),
    /** Over-powered. */
    SUPERCONDUCTOR(0f, 10, 1_000_000, 0xFFFF01);

    /** Ohms, scaled down by 100 for balance as in the original. */
    public final float resistance;
    /** Damage dealt by a bare, live wire. */
    public final int damage;
    public final int maxAmps;
    public final int color;

    WireMaterial(float resistance, int damage, int maxAmps, int color) {
        this.resistance = resistance / 100f;
        this.damage = damage;
        this.maxAmps = maxAmps;
        this.color = color;
    }

    public int capacity() {
        long fe = (long) maxAmps * RIConfig.get(RIConfig.WIRE_FE_PER_AMP);
        return (int) Math.min(Integer.MAX_VALUE, fe);
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
