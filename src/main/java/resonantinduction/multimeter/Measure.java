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

    /** The value with a k, M or G suffix and this unit. */
    public String format(double value) {
        double abs = Math.abs(value);
        String number;
        if (abs >= 1e9) {
            number = String.format("%.2fG", value / 1e9);
        } else if (abs >= 1e6) {
            number = String.format("%.2fM", value / 1e6);
        } else if (abs >= 1e4) {
            number = String.format("%.1fk", value / 1e3);
        } else if (value == Math.rint(value)) {
            number = Long.toString((long) value);
        } else {
            number = String.format("%.2f", value);
        }
        return number + " " + unit;
    }

    public static Measure byIndex(int i) {
        Measure[] values = values();
        return values[Math.floorMod(i, values.length)];
    }
}
