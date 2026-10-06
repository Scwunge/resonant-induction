package resonantinduction.resource;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import resonantinduction.registry.RIRegistries;

/** Furnace (and blast furnace) recipes for dirty and refined dust: each smelts into one ingot of its metal. */
public final class DustSmeltingRecipe {
    private DustSmeltingRecipe() {}

    static ItemStack ingotFor(ItemStack input) {
        String material = Materials.material(input);
        return material == null ? ItemStack.EMPTY : Materials.firstIngot(material).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    static Ingredient dusts() {
        return Ingredient.of(RIRegistries.DUST.get(), RIRegistries.REFINED_DUST.get());
    }

    public static class Smelting extends SmeltingRecipe {
        public Smelting(int cookingTime) {
            super("", CookingBookCategory.MISC, dusts(), new ItemStack(Items.IRON_INGOT), 0.7f, cookingTime);
        }

        @Override
        public boolean matches(SingleRecipeInput input, Level level) {
            return super.matches(input, level) && !ingotFor(input.item()).isEmpty();
        }

        @Override
        public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
            return ingotFor(input.item());
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return RIRegistries.DUST_SMELTING.get();
        }
    }

    public static class Blasting extends BlastingRecipe {
        public Blasting(int cookingTime) {
            super("", CookingBookCategory.MISC, dusts(), new ItemStack(Items.IRON_INGOT), 0.7f, cookingTime);
        }

        @Override
        public boolean matches(SingleRecipeInput input, Level level) {
            return super.matches(input, level) && !ingotFor(input.item()).isEmpty();
        }

        @Override
        public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
            return ingotFor(input.item());
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return RIRegistries.DUST_BLASTING.get();
        }
    }

    public static <T extends net.minecraft.world.item.crafting.Recipe<?>> RecipeSerializer<T> serializer(java.util.function.IntFunction<T> factory, java.util.function.ToIntFunction<T> time, int defaultTime) {
        MapCodec<T> codec = RecordCodecBuilder.mapCodec(i -> i.group(
                com.mojang.serialization.Codec.INT.optionalFieldOf("cookingtime", defaultTime).forGetter(time::applyAsInt)).apply(i, factory::apply));
        StreamCodec<RegistryFriendlyByteBuf, T> stream = ByteBufCodecs.VAR_INT.<RegistryFriendlyByteBuf>cast().map(factory::apply, time::applyAsInt);
        return new RecipeSerializer<>() {
            @Override
            public MapCodec<T> codec() {
                return codec;
            }

            @Override
            public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
                return stream;
            }
        };
    }
}
