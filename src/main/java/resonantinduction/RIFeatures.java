package resonantinduction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.conditions.ICondition;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Feature switches (COMMON config, read before recipes load). A disabled feature keeps its blocks registered, so worlds
 * stay intact, but its recipes are not loaded. Use them to turn off parts that another mod in a pack already covers.
 */
public final class RIFeatures {
    public static final ModConfigSpec SPEC;
    private static final Map<String, ModConfigSpec.BooleanValue> FEATURES = new LinkedHashMap<>();

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Turn features off to drop their recipes (blocks stay registered so existing worlds are safe).").push("features");
        feature(b, "tesla", "Tesla Coil and Quantum Entangler");
        feature(b, "levitator", "Electromagnetic Levitator");
        feature(b, "quantum", "Quantum Glyphs and Quantum Gates");
        feature(b, "charger", "Charger (overlaps the Electrodynamics chargers)");
        feature(b, "laser", "Mining Laser");
        feature(b, "wires", "Wires (overlaps the Electrodynamics wires)");
        feature(b, "battery", "Battery (overlaps the Electrodynamics batteries)");
        feature(b, "transformer", "Transformer");
        feature(b, "multimeter", "Multimeter (overlaps the Electrodynamics multimeter)");
        feature(b, "generators", "Solar Panel and Thermopile (overlap the Electrodynamics generators)");
        b.pop();
        SPEC = b.build();
    }

    private RIFeatures() {}

    private static void feature(ModConfigSpec.Builder b, String name, String what) {
        FEATURES.put(name, b.comment(what).define(name, true));
    }

    public static boolean enabled(String name) {
        ModConfigSpec.BooleanValue value = FEATURES.get(name);
        if (value == null) {
            return true;
        }
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    /** Recipe condition {@code {"type": "resonantinduction:feature", "feature": "charger"}}. */
    public record FeatureCondition(String feature) implements ICondition {
        public static final MapCodec<FeatureCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.fieldOf("feature").forGetter(FeatureCondition::feature)).apply(i, FeatureCondition::new));

        @Override
        public boolean test(IContext context) {
            return enabled(feature);
        }

        @Override
        public MapCodec<? extends ICondition> codec() {
            return CODEC;
        }
    }
}
