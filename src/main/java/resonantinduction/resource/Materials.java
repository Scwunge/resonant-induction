package resonantinduction.resource;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import resonantinduction.registry.RIRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The original Resource Generator, done with tags: every metal that has both a {@code c:ores/<name>} and a
 * {@code c:ingots/<name>} tag gets rubble, dust and refined dust (one item each, carrying the metal as a component).
 */
public final class Materials {
    private Materials() {}

    public static TagKey<Item> ingotTag(String material) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ingots/" + material));
    }

    public static TagKey<Item> oreItemTag(String material) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ores/" + material));
    }

    public static TagKey<Block> oreBlockTag(String material) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores/" + material));
    }

    /** Metals that have both an ore and an ingot tag with something in them (only valid once tags are loaded). */
    public static List<String> all() {
        List<String> out = new ArrayList<>();
        BuiltInRegistries.ITEM.getTags().forEach(pair -> {
            ResourceLocation id = pair.getFirst().location();
            if (id.getNamespace().equals("c") && id.getPath().startsWith("ores/")) {
                String name = id.getPath().substring(5);
                if (!name.contains("/") && pair.getSecond().size() > 0 && firstIngot(name).isPresent()) {
                    out.add(name);
                }
            }
        });
        out.sort(String::compareTo);
        return out;
    }

    public static Optional<Item> firstIngot(String material) {
        Optional<HolderSet.Named<Item>> set = BuiltInRegistries.ITEM.getTag(ingotTag(material));
        if (set.isEmpty() || set.get().size() == 0) {
            return Optional.empty();
        }
        // Prefer vanilla, then whatever comes first.
        for (Holder<Item> h : set.get()) {
            if (BuiltInRegistries.ITEM.getKey(h.value()).getNamespace().equals("minecraft")) {
                return Optional.of(h.value());
            }
        }
        return Optional.of(set.get().get(0).value());
    }

    /** The metal of an ore block, if it is one with an ingot. */
    @Nullable
    public static String materialOfOre(ItemStack stack) {
        for (var tag : stack.getTags().toList()) {
            ResourceLocation id = tag.location();
            if (id.getNamespace().equals("c") && id.getPath().startsWith("ores/")) {
                String name = id.getPath().substring(5);
                if (!name.contains("/") && firstIngot(name).isPresent()) {
                    return name;
                }
            }
        }
        return null;
    }

    @Nullable
    public static String material(ItemStack stack) {
        return stack.get(RIRegistries.MATERIAL.get());
    }

    public static ItemStack of(Item item, String material, int count) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(RIRegistries.MATERIAL.get(), material);
        return stack;
    }

    public static Component displayName(String material) {
        String fallback = material.isEmpty() ? "" : Character.toUpperCase(material.charAt(0)) + material.substring(1).replace('_', ' ');
        return Component.translatableWithFallback("material.resonantinduction." + material.toLowerCase(Locale.ROOT), fallback);
    }
}
