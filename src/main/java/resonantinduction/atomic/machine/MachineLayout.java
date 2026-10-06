package resonantinduction.atomic.machine;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Where things go on an atomic machine's screen (in pixels from the top left of the panel): its slots, fluid gauges, the work
 * bar, the energy bar and some lines of text. The player's inventory goes under {@code height - 82}.
 */
public record MachineLayout(int height, List<SlotAt> slots, List<GaugeAt> gauges, int barX, int barY, int energyX, int energyY, List<Line> lines) {
    /** A machine slot. Output slots take nothing from the player; the battery slot is drawn with a mark. */
    public record SlotAt(int index, int x, int y, Kind kind) {}

    public enum Kind { NORMAL, OUTPUT, BATTERY, FLUID }

    /** A fluid gauge for the machine's tank {@code tank}, 18 wide and 49 high. */
    public record GaugeAt(int tank, int x, int y) {}

    public record Line(Component text, int x, int y) {}

    public static SlotAt slot(int index, int x, int y) {
        return new SlotAt(index, x, y, Kind.NORMAL);
    }

    public static SlotAt output(int index, int x, int y) {
        return new SlotAt(index, x, y, Kind.OUTPUT);
    }

    public static SlotAt battery(int x, int y) {
        return new SlotAt(AtomicMachineBlockEntity.BATTERY_SLOT, x, y, Kind.BATTERY);
    }

    public static SlotAt fluid(int index, int x, int y) {
        return new SlotAt(index, x, y, Kind.FLUID);
    }
}
