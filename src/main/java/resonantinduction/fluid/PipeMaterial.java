package resonantinduction.fluid;

import net.minecraft.util.StringRepresentable;

/** Pipe materials with the original flow rates (mB a tick, which is also what a pipe holds) and pressure limits. */
public enum PipeMaterial implements StringRepresentable {
    CERAMIC(5, 5, 0xB3866F),
    BRONZE(25, 25, 0xD49568),
    PLASTIC(50, 30, 0xDAF4F7),
    IRON(100, 50, 0x5C6362),
    STEEL(100, 100, 0x888888),
    FIBERGLASS(1000, 200, 0x9F9691);

    public final int maxFlowRate;
    public final int maxPressure;
    public final int color;

    PipeMaterial(int maxFlowRate, int maxPressure, int color) {
        this.maxFlowRate = maxFlowRate;
        this.maxPressure = maxPressure;
        this.color = color;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }
}
