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
    public static final ModConfigSpec.IntValue ELECTRIC_FIREBOX_USE;
    public static final ModConfigSpec.DoubleValue GRATE_EFFECT;
    public static final ModConfigSpec.BooleanValue RADIOACTIVE_ORES;
    public static final ModConfigSpec.DoubleValue ATOMIC_ENERGY_SCALE;
    public static final ModConfigSpec.BooleanValue REACTOR_MELTDOWNS;
    public static final ModConfigSpec.DoubleValue STEAM_ENERGY;
    public static final ModConfigSpec.BooleanValue TOXIC_WASTE;
    public static final ModConfigSpec.BooleanValue ANTIMATTER_EXPLOSIONS;
    public static final ModConfigSpec.DoubleValue FULMINATION_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue ANTIMATTER_ENERGY_SCALE;
    public static final ModConfigSpec.DoubleValue TURBINE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue FISSION_BOIL_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue TURBINE_STACKING;
    public static final ModConfigSpec.IntValue URANIUM_HEXAFLUORIDE_RATIO;
    public static final ModConfigSpec.IntValue WATER_PER_DEUTERIUM;
    public static final ModConfigSpec.IntValue DEUTERIUM_PER_TRITIUM;
    public static final ModConfigSpec.DoubleValue DARK_MATTER_CHANCE;
    public static final ModConfigSpec.IntValue QUANTUM_ASSEMBLER_MODE;
    public static final ModConfigSpec.IntValue PLASMA_HEAT_AMOUNT;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> QUANTUM_ASSEMBLER_RECIPES;
    public static final ModConfigSpec.DoubleValue GRATE_DRAIN_SPEED;

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
        ELECTRIC_FIREBOX_USE = b.comment("FE the Electric Firebox uses per tick of burning (it buffers ten ticks' worth). Original: 10000 joules.")
                .defineInRange("electricFireboxUse", 10000, 1, 1_000_000);
        b.pop();

        b.push("fluids");
        GRATE_EFFECT = b.comment("How many blocks a Grate works on each half second, per point of pressure. Original: 5.")
                .defineInRange("grateEffectMultiplier", 5.0, 0.1, 100.0);
        GRATE_DRAIN_SPEED = b.comment("Buckets a Grate can hold per block it works on (it always holds at least one). Original: 0.01.")
                .defineInRange("grateDrainSpeedMultiplier", 0.01, 0.0, 10.0);
        b.pop();

        b.push("atomic");
        RADIOACTIVE_ORES = b.comment("Uranium ore irradiates those who stand on or near it.").define("radioactiveOres", true);
        ATOMIC_ENERGY_SCALE = b.comment("FE the atomic machines use per joule the original used (the Chemical Extractor 5000 a tick, the Nuclear Boiler",
                        "50000, the Centrifuge 500000). Lower it to make them cheaper to run.")
                .defineInRange("energyScale", 1.0, 0.0001, 1000.0);
        TOXIC_WASTE = b.comment("Fission reactors make toxic waste.").define("toxicWaste", true);
        REACTOR_MELTDOWNS = b.comment("A reactor cell kept at 2000 K or more for 50 seconds melts down (explodes, leaving radioactive waste).")
                .define("reactorMeltdowns", true);
        STEAM_ENERGY = b.comment("FE an Electric Turbine makes from each mB of steam (before turbineOutputMultiplier).")
                .defineInRange("steamEnergy", 100.0, 0.0, 100000.0);
        ANTIMATTER_EXPLOSIONS = b.comment("Dropped antimatter explodes when it expires (the original's ban_antimatter_power flag, inverted).").define("antimatterExplosions", true);
        FULMINATION_MULTIPLIER = b.comment("Multiplier on the Fulmination Generator's output. Original: 1.").defineInRange("fulminationOutputMultiplier", 1.0, 0.0, 1000.0);
        ANTIMATTER_ENERGY_SCALE = b.comment("FE per original joule for the Fulmination Generator and the Quantum Assembler. Their original figures (a buffer",
                        "and a tick of work of 10^13 J) are far past what FE can carry, so they are scaled down together, keeping the original's",
                        "balance: a milligram of antimatter going off fills a generator, which powers one tick of the assembler's two-minute job.")
                .defineInRange("antimatterEnergyScale", 0.000001, 0.0000000001, 0.0001);
        TURBINE_MULTIPLIER = b.comment("Multiplier on the Electric Turbine's output. Original: 1.").defineInRange("turbineOutputMultiplier", 1.0, 0.0, 1000.0);
        FISSION_BOIL_MULTIPLIER = b.comment("Multiplier on the steam a reactor cell boils. Original: 1.").defineInRange("fissionBoilVolumeMultiplier", 1.0, 0.0, 1000.0);
        TURBINE_STACKING = b.comment("Electric turbines can be stacked into bigger ones.").define("allowTurbineStacking", true);
        URANIUM_HEXAFLUORIDE_RATIO = b.comment("mB of uranium hexafluoride a yellowcake boils into. Original: 200.").defineInRange("uraniumHexafluorideRatio", 200, 1, 10000);
        WATER_PER_DEUTERIUM = b.comment("mB of water per mB of deuterium extracted. Original: 4.").defineInRange("waterPerDeuterium", 4, 1, 1000);
        DEUTERIUM_PER_TRITIUM = b.comment("mB of deuterium per mB of tritium extracted. Original: 4.").defineInRange("deuteriumPerTritium", 4, 1, 1000);
        DARK_MATTER_CHANCE = b.comment("Chance a particle collision makes dark matter. Original: 0.2.").defineInRange("darkMatterSpawnChance", 0.2, 0.0, 1.0);
        QUANTUM_ASSEMBLER_MODE = b.comment("What the Quantum Assembler can copy: 0 nothing, 1 items, 2 items and blocks. Original: 1.").defineInRange("quantumAssemblerGenerateMode", 1, 0, 2);
        PLASMA_HEAT_AMOUNT = b.comment("mB each of deuterium and tritium the Plasma Heater turns into plasma a tick. Original: 100.")
                .defineInRange("plasmaHeatAmount", 100, 1, 10000);
        QUANTUM_ASSEMBLER_RECIPES = b.comment("More items the Quantum Assembler can copy whatever the mode, by id (e.g. \"minecraft:diamond_block\").")
                .defineListAllowEmpty("quantumAssemblerRecipes", java.util.List.of(), () -> "", o -> o instanceof String);
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

    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        return SPEC.isLoaded() ? v.get() : v.getDefault();
    }
}
