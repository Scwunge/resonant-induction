package resonantinduction.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import resonantinduction.ResonantInduction;
import resonantinduction.registry.RIRegistries;

/** Description pages: every item with a "jei.resonantinduction.<id>" lang key gets one. */
@JeiPlugin
public class RIJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResonantInduction.id("jei");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RIRegistries.ITEMS.getEntries().forEach(holder -> {
            String key = "jei." + ResonantInduction.MODID + "." + BuiltInRegistries.ITEM.getKey(holder.get()).getPath();
            if (net.minecraft.locale.Language.getInstance().has(key)) {
                registration.addIngredientInfo(new ItemStack(holder.get()), VanillaTypes.ITEM_STACK, Component.translatable(key));
            }
        });
    }
}
