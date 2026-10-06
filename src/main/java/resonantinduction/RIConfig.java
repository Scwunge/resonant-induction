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

    public static final ModConfigSpec.IntValue SOLAR_OUTPUT;
    public static final ModConfigSpec.DoubleValue WIND_POWER_RATIO;
    public static final ModConfigSpec.DoubleValue WATER_POWER_RATIO;
    public static final ModConfigSpec.DoubleValue MECHANICAL_FE_RATIO;

    public static final ModConfigSpec.IntValue BATTERY_TIER1;
    public static final ModConfigSpec.IntValue BATTERY_TIER2;
    public static final ModConfigSpec.IntValue BATTERY_TIER3;
    public static final ModConfigSpec.IntValue BATTERY_MAX_OUTPUT;

    public static final ModConfigSpec.IntValue PISTON_BREAK_COUNT;

    public static final ModConfigSpec.IntValue WIRE_FE_PER_AMP;
    public static final ModConfigSpec.BooleanValue WIRE_SHOCK;

    public static final ModConfigSpec.IntValue LASER_CAPACITY;
    public static final ModConfigSpec.IntValue LASER_COST_REMOVE;
    public static final ModConfigSpec.IntValue LASER_RANGE;
    public static final ModConfigSpec.DoubleValue LASER_DAMAGE;
    public static final ModConfigSpec.IntValue LASER_BREAK_TICKS;

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

        b.push("generators");
        SOLAR_OUTPUT = b.comment("Solar Panel output in FE per tick in clear daylight. Original: 50.")
                .defineInRange("solarOutput", 50, 0, 100000);
        WIND_POWER_RATIO = b.comment("Multiplier on wind turbine power. Original: 1.").defineInRange("windPowerRatio", 1.0, 0.0, 100.0);
        WATER_POWER_RATIO = b.comment("Multiplier on water turbine power. Original: 1.").defineInRange("waterPowerRatio", 1.0, 0.0, 100.0);
        MECHANICAL_FE_RATIO = b.comment("FE per joule of rotational energy in the Electric Motor (both ways). The original used one to one.")
                .defineInRange("feMechanicalRatio", 1.0, 0.001, 1000.0);
        b.pop();

        b.push("battery");
        b.comment("The original tiers held 5e8, 8e11 and 1.4e15 joules (exponential), which does not fit Forge Energy; these keep the",
                "same order (each tier is crafted from eight of the one below).");
        BATTERY_TIER1 = b.defineInRange("tier1Capacity", 1_000_000, 1, Integer.MAX_VALUE);
        BATTERY_TIER2 = b.defineInRange("tier2Capacity", 50_000_000, 1, Integer.MAX_VALUE);
        BATTERY_TIER3 = b.defineInRange("tier3Capacity", 2_000_000_000, 1, Integer.MAX_VALUE);
        BATTERY_MAX_OUTPUT = b.comment("Most FE an output face pushes per tick (the original had no limit).")
                .defineInRange("maxOutputPerFace", 1_000_000, 1, Integer.MAX_VALUE);
        b.pop();

        b.push("processing");
        PISTON_BREAK_COUNT = b.comment("Mechanical Piston strikes (one per turn) needed to crush a block. Original: 5.")
                .defineInRange("pistonBreakCount", 5, 1, 100);
        b.pop();

        b.push("wires");
        WIRE_FE_PER_AMP = b.comment("Wire capacity in FE per tick = the metal's original amp rating times this. Copper 200 A, tin 100, iron 800,",
                        "aluminium 600, silver 700, superconductor 1000000.")
                .defineInRange("fePerAmp", 10, 1, 10000);
        WIRE_SHOCK = b.comment("Bare (uninsulated) wires that carried power in the last second hurt whatever touches them.")
                .define("bareWiresShock", true);
        b.pop();

        b.push("mining_laser");
        LASER_CAPACITY = b.comment("Mining Laser battery (FE). Original: 500000.")
                .defineInRange("capacity", 500000, 1000, Integer.MAX_VALUE);
        LASER_COST_REMOVE = b.comment("FE per tick while firing in remove mode; smelt mode uses half, damage mode a third. Original: 100.")
                .defineInRange("costPerTick", 100, 0, 100000);
        LASER_RANGE = b.comment("Beam range in blocks. Original: 50.")
                .defineInRange("range", 50, 1, 128);
        LASER_DAMAGE = b.comment("Damage per tick to whatever the beam hits (it also sets it on fire). Original: 3.3.")
                .defineInRange("damage", 3.3, 0.0, 100.0);
        LASER_BREAK_TICKS = b.comment("Ticks the beam must stay on a block to cut it out. Original: 15.")
                .defineInRange("breakTicks", 15, 1, 200);
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
