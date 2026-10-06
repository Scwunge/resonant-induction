package resonantinduction.archaic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import resonantinduction.registry.RIRegistries;

import java.util.ArrayList;
import java.util.List;

/** Imprint: a list of items (made in the Imprinter) that filters, sorters, detectors and manipulators act on. */
public class ImprintItem extends Item {
    public ImprintItem(Properties properties) {
        super(properties);
    }

    public static List<ItemStack> filters(ItemStack imprint) {
        return imprint.getOrDefault(RIRegistries.IMPRINT_ITEMS.get(), List.of());
    }

    public static void setFilters(ItemStack imprint, List<ItemStack> stacks) {
        List<ItemStack> copies = new ArrayList<>();
        for (ItemStack s : stacks) {
            if (!s.isEmpty() && copies.stream().noneMatch(c -> ItemStack.isSameItem(c, s))) {
                copies.add(s.copyWithCount(1));
            }
        }
        imprint.set(RIRegistries.IMPRINT_ITEMS.get(), copies);
    }

    /** Same item (ignoring count and components), as the original's isItemEqual. */
    public static boolean isFiltering(ItemStack imprint, ItemStack stack) {
        if (imprint.isEmpty() || stack.isEmpty()) {
            return false;
        }
        for (ItemStack f : filters(imprint)) {
            if (ItemStack.isSameItem(f, stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        List<ItemStack> filters = filters(stack);
        if (filters.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.resonantinduction.no_imprint").withStyle(ChatFormatting.GRAY));
        }
        for (ItemStack f : filters) {
            tooltip.add(f.getHoverName().copy().withStyle(ChatFormatting.GRAY));
        }
    }
}
