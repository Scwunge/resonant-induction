package resonantinduction.tesla;

import net.minecraft.util.StringRepresentable;

/** Which model a coil in a tower shows. */
public enum TeslaPart implements StringRepresentable {
    /** Lowest coil (also a lone coil). */
    BOTTOM,
    MIDDLE,
    TOP;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static TeslaPart of(boolean teslaBelow, boolean teslaAbove) {
        if (teslaBelow && teslaAbove) {
            return MIDDLE;
        }
        return teslaBelow ? TOP : BOTTOM;
    }
}
