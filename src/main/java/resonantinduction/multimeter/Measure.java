package resonantinduction.multimeter;

import java.util.Locale;

/** The readings a multimeter can take of the block behind it (the original's graphs, minus voltage, which FE has no notion of). */
public enum Measure {
    ENERGY("FE"),
    POWER("FE/t"),
    CAPACITY("FE"),
    TORQUE("N·m"),
    SPEED("rad/s"),
    FLUID("mB"),
    TEMPERATURE("K"),
    PRESSURE("Pa");

    public final String unit;

    Measure(String unit) {
        this.unit = unit;
    }

    public String key() {
        return "multimeter.resonantinduction." + name().toLowerCase(Locale.ROOT);
    }

    public static Measure byIndex(int i) {
        Measure[] values = values();
        return values[Math.floorMod(i, values.length)];
    }
}
