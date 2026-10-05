package resonantinduction.item;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import resonantinduction.registry.RIRegistries;
import resonantinduction.tesla.TeslaBlockEntity;

import java.util.List;

/**
 * Links two Tesla towers, even in different dimensions. Use on one tower to mark it, then on another to link both.
 * Sneak-use on a tower unlinks it; sneak-use in the air forgets the mark.
 */
public class QuantumEntanglerItem extends Item {
    public QuantumEntanglerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof TeslaBlockEntity coil)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        TeslaBlockEntity tower = coil.primary();
        GlobalPos here = GlobalPos.of(level.dimension(), tower.getBlockPos());
        GlobalPos marked = stack.get(RIRegistries.LINK_TARGET.get());

        if (player != null && player.isSecondaryUseActive()) {
            tower.unlink();
            stack.remove(RIRegistries.LINK_TARGET.get());
            message(player, Component.translatable("message.resonantinduction.entangler.unlinked"));
        } else if (marked == null || marked.equals(here)) {
            stack.set(RIRegistries.LINK_TARGET.get(), here);
            message(player, Component.translatable("message.resonantinduction.entangler.marked", here.pos().getX(), here.pos().getY(), here.pos().getZ()));
        } else {
            stack.remove(RIRegistries.LINK_TARGET.get());
            if (tower.linkTo(marked)) {
                message(player, Component.translatable("message.resonantinduction.entangler.linked",
                        marked.pos().getX(), marked.pos().getY(), marked.pos().getZ(), marked.dimension().location().toString()));
            } else {
                message(player, Component.translatable("message.resonantinduction.entangler.failed"));
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive() && stack.has(RIRegistries.LINK_TARGET.get())) {
            if (!level.isClientSide) {
                stack.remove(RIRegistries.LINK_TARGET.get());
                message(player, Component.translatable("message.resonantinduction.entangler.cleared"));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    private static void message(Player player, Component text) {
        if (player != null) {
            player.displayClientMessage(text, true);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(RIRegistries.LINK_TARGET.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        GlobalPos marked = stack.get(RIRegistries.LINK_TARGET.get());
        if (marked != null) {
            tooltip.add(Component.translatable("tooltip.resonantinduction.entangler.marked",
                    marked.pos().getX(), marked.pos().getY(), marked.pos().getZ(), marked.dimension().location().toString()));
        } else {
            tooltip.add(Component.translatable("tooltip.resonantinduction.entangler.empty"));
        }
    }
}
