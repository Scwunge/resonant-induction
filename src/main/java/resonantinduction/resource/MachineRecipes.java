package resonantinduction.resource;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import resonantinduction.registry.RIRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * The original MachineRecipes for the crusher (mechanical piston), grinder (grinding wheel and millstone) and mixer, with the
 * same vanilla extras, plus the per-metal chain: ore to rubble, rubble to two dirty dust, dirty dust to refined dust.
 */
public final class MachineRecipes {
    private MachineRecipes() {}

    public record Output(ItemStack stack, float chance) {
        public static Output of(Item item, int count) {
            return new Output(new ItemStack(item, count), 1);
        }
    }

    /** Ore blocks crush to rubble; stone to cobblestone, cobblestone to gravel, a chest to seven planks. */
    public static List<Output> crusher(ItemStack input) {
        List<Output> out = new ArrayList<>();
        if (input.is(Items.STONE)) {
            out.add(Output.of(Items.COBBLESTONE, 1));
        } else if (input.is(Items.COBBLESTONE)) {
            out.add(Output.of(Items.GRAVEL, 1));
        } else if (input.is(Items.CHEST)) {
            out.add(Output.of(Items.OAK_PLANKS, 7));
        } else {
            String material = Materials.materialOfOre(input);
            if (material != null) {
                out.add(new Output(Materials.of(RIRegistries.RUBBLE.get(), material, 1), 1));
            }
        }
        return out;
    }

    /** Rubble grinds to two dirty dust; cobblestone, gravel and glass to sand. */
    public static List<Output> grinder(ItemStack input) {
        List<Output> out = new ArrayList<>();
        if (input.is(Items.COBBLESTONE) || input.is(Items.GRAVEL) || input.is(Items.GLASS)) {
            out.add(Output.of(Items.SAND, 1));
        } else if (input.is(RIRegistries.RUBBLE.get())) {
            String material = Materials.material(input);
            if (material != null) {
                out.add(new Output(Materials.of(RIRegistries.DUST.get(), material, 1), 1));
                out.add(new Output(Materials.of(RIRegistries.DUST.get(), material, 1), 1));
            }
        }
        return out;
    }

    /** Dirty dust washes to refined dust. */
    @Nullable
    public static ItemStack mixer(ItemStack input) {
        if (input.is(RIRegistries.DUST.get())) {
            String material = Materials.material(input);
            if (material != null) {
                return Materials.of(RIRegistries.REFINED_DUST.get(), material, 1);
            }
        }
        return null;
    }

    public static boolean canCrush(ItemStack input) {
        return !crusher(input).isEmpty();
    }

    public static boolean canGrind(ItemStack input) {
        return !grinder(input).isEmpty();
    }
}
