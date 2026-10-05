package resonantinduction;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Gameplay settings. SERVER type, so they are per world and synced to clients. */
public final class RIConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue TESLA_CAPACITY;
    public static final ModConfigSpec.IntValue TESLA_RANGE_PER_BLOCK;
    public static final ModConfigSpec.IntValue TESLA_MAX_RANGE;
    public static final ModConfigSpec.IntValue TESLA_MAX_TARGETS;
    public static final ModConfigSpec.BooleanValue TESLA_CROSS_DIMENSION;
    public static final ModConfigSpec.DoubleValue TESLA_DAMAGE;
    public static final ModConfigSpec.BooleanValue TESLA_ATTACK_PLAYERS;
    public static final ModConfigSpec.BooleanValue TESLA_SOUNDS;

    public static final ModConfigSpec.IntValue LEVITATOR_REACH;
    public static final ModConfigSpec.IntValue LEVITATOR_PUSH_DELAY;
    public static final ModConfigSpec.IntValue LEVITATOR_ITEMS_PER_PUSH;
    public static final ModConfigSpec.DoubleValue LEVITATOR_MAX_SPEED;
    public static final ModConfigSpec.DoubleValue LEVITATOR_ACCELERATION;
    public static final ModConfigSpec.IntValue LEVITATOR_MAX_PATH;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("tesla");
        TESLA_CAPACITY = b.comment("Energy (FE) a Tesla tower buffers, and the most it sends to one target per zap. Original: 10000.")
                .defineInRange("capacity", 10000, 100, Integer.MAX_VALUE);
        TESLA_RANGE_PER_BLOCK = b.comment("Range gained per coil above the first. Range = rangePerBlock * (height - 1). Original: 4.")
                .defineInRange("rangePerBlock", 4, 1, 64);
        TESLA_MAX_RANGE = b.comment("Range cap in blocks. Original: 50.")
                .defineInRange("maxRange", 50, 1, 256);
        TESLA_MAX_TARGETS = b.comment("Most towers one tower sends to per zap, nearest first. Original: 10.")
                .defineInRange("maxTargets", 10, 1, 64);
        TESLA_CROSS_DIMENSION = b.comment("Allow linked towers in different dimensions.")
                .define("crossDimension", true);
        TESLA_DAMAGE = b.comment("Damage dealt to a mob standing in an arc (every 5th zap, when entity attack is on). 0 disables.")
                .defineInRange("damage", 4.0, 0.0, 1000.0);
        TESLA_ATTACK_PLAYERS = b.comment("Arcs can hurt players too (only when the server allows PvP).")
                .define("attackPlayers", true);
        TESLA_SOUNDS = b.comment("Play the electric shock sound on zaps.")
                .define("sounds", true);
        b.pop();

        b.push("levitator");
        LEVITATOR_REACH = b.comment("How far a levitator's beam reaches to find a levitator facing it. Original: 40.")
                .defineInRange("reach", 40, 1, 128);
        LEVITATOR_PUSH_DELAY = b.comment("Ticks between items taken out of the inventory in push mode. Original: 5.")
                .defineInRange("pushDelay", 5, 1, 200);
        LEVITATOR_ITEMS_PER_PUSH = b.comment("Items taken out per push.")
                .defineInRange("itemsPerPush", 1, 1, 64);
        LEVITATOR_MAX_SPEED = b.comment("Top speed of items in a beam, blocks per tick. Original: 0.2.")
                .defineInRange("maxSpeed", 0.2, 0.01, 2.0);
        LEVITATOR_ACCELERATION = b.comment("Acceleration of items in a beam, blocks per tick per tick. Original: 0.02.")
                .defineInRange("acceleration", 0.02, 0.001, 1.0);
        LEVITATOR_MAX_PATH = b.comment("Longest distance between two linked levitators. Original: 200.")
                .defineInRange("maxPathDistance", 200, 2, 1024);
        b.pop();
        SPEC = b.build();
    }

    private RIConfig() {}

    /** Values are only readable once the server config is loaded; use the defaults before that. */
    public static int get(ModConfigSpec.IntValue v) {
        return SPEC.isLoaded() ? v.get() : v.getDefault();
    }

    public static double get(ModConfigSpec.DoubleValue v) {
        return SPEC.isLoaded() ? v.get() : v.getDefault();
    }

    public static boolean get(ModConfigSpec.BooleanValue v) {
        return SPEC.isLoaded() ? v.get() : v.getDefault();
    }
}
