package resonantinduction.atomic.reactor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import resonantinduction.registry.RIRegistries;

import java.util.List;

/** The thermometer item: sneak-use it on a block to track that block's temperature; use it in the air to clear that. */
public class ThermometerItem extends BlockItem {
    public ThermometerItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isSecondaryUseActive()) {
            if (!context.getLevel().isClientSide) {
                context.getItemInHand().set(RIRegistries.LINK_TARGET.get(), GlobalPos.of(context.getLevel().dimension(), context.getClickedPos()));
                player.displayClientMessage(Component.translatable("message.resonantinduction.thermometer.tracking", context.getClickedPos().toShortString()), true);
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        stack.remove(RIRegistries.LINK_TARGET.get());
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("message.resonantinduction.thermometer.cleared"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        GlobalPos track = stack.get(RIRegistries.LINK_TARGET.get());
        if (track != null) {
            tooltip.add(Component.translatable("tooltip.resonantinduction.thermometer.tracking", track.pos().toShortString()).withStyle(ChatFormatting.DARK_AQUA));
        } else {
            tooltip.add(Component.translatable("tooltip.resonantinduction.thermometer.not_tracking").withStyle(ChatFormatting.DARK_RED));
        }
    }
}
