package resonantinduction.archaic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import resonantinduction.registry.RIRegistries;

import java.util.List;

/** A crate item remembers what was in it, and a full one is heavy: carrying it slows you, more the fuller it is. */
public class CrateItem extends BlockItem {
    public CrateItem(CrateBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CrateContents contents = stack.get(RIRegistries.CRATE_CONTENTS.get());
        int capacity = CrateBlockEntity.slots(((CrateBlock) getBlock()).tier()) * 64;
        if (contents != null && contents.count() > 0) {
            tooltip.add(contents.item().getHoverName().copy().withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.resonantinduction.crate.amount", contents.count(), capacity).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.resonantinduction.crate.capacity", capacity).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return stack.has(RIRegistries.CRATE_CONTENTS.get()) ? 1 : super.getMaxStackSize(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        CrateContents contents = stack.get(RIRegistries.CRATE_CONTENTS.get());
        if (!level.isClientSide && contents != null && entity instanceof Player player && !player.isCreative() && level.getGameTime() % 5 == 0) {
            int capacity = CrateBlockEntity.slots(((CrateBlock) getBlock()).tier()) * 64;
            int amplifier = Math.min(4, (int) (5f * contents.count() / capacity));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, amplifier, false, false));
        }
    }
}
