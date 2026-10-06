package resonantinduction.battery;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import resonantinduction.registry.RIRegistries;

import java.util.ArrayList;
import java.util.List;

/** A battery as an item: keeps its tier and charge, and can be charged and drained like any FE item. */
public class BatteryItem extends BlockItem {
    public BatteryItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static int tier(ItemStack stack) {
        return Math.max(0, Math.min(2, stack.getOrDefault(RIRegistries.BATTERY_TIER.get(), 0)));
    }

    public static int capacity(ItemStack stack) {
        return BatteryBlockEntity.capacityForTier(tier(stack));
    }

    /** Creative tab entries: each tier empty and full, as in the original. */
    public List<ItemStack> variants() {
        List<ItemStack> out = new ArrayList<>();
        for (int tier = 0; tier <= 2; tier++) {
            ItemStack empty = new ItemStack(this);
            empty.set(RIRegistries.BATTERY_TIER.get(), tier);
            out.add(empty);
            ItemStack full = empty.copy();
            full.set(RIRegistries.ENERGY.get(), BatteryBlockEntity.capacityForTier(tier));
            out.add(full);
        }
        return out;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * stack.getOrDefault(RIRegistries.ENERGY.get(), 0) / Math.max(1, capacity(stack)));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x40FF40;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.resonantinduction.tier", tier(stack) + 1).withStyle(ChatFormatting.GRAY));
        int energy = stack.getOrDefault(RIRegistries.ENERGY.get(), 0);
        int capacity = capacity(stack);
        ChatFormatting color = energy <= capacity / 3 ? ChatFormatting.DARK_RED : energy > capacity * 2 / 3 ? ChatFormatting.DARK_GREEN : ChatFormatting.GOLD;
        tooltip.add(Component.translatable("tooltip.resonantinduction.energy", energy, capacity).withStyle(color));
    }
}
