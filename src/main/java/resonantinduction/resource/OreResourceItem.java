package resonantinduction.resource;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import resonantinduction.registry.RIRegistries;

import java.util.List;

/**
 * Rubble, dust or refined dust of a metal. Dirty dust can also be washed by sneak-using it on a water cauldron, as the original
 * tooltip said; dust (either kind) placed on the ground makes a dust pile that a firebox below melts.
 */
public class OreResourceItem extends Item {
    public enum Form { RUBBLE, DUST, REFINED_DUST }

    private final Form form;

    public OreResourceItem(Form form, Properties properties) {
        super(properties);
        this.form = form;
    }

    public Form form() {
        return form;
    }

    @Override
    public Component getName(ItemStack stack) {
        String material = Materials.material(stack);
        Component metal = material == null ? Component.literal("?") : Materials.displayName(material);
        return Component.translatable(getDescriptionId(), metal);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockState state = level.getBlockState(context.getClickedPos());
        ItemStack stack = context.getItemInHand();
        String material = Materials.material(stack);
        if (material == null) {
            return InteractionResult.PASS;
        }
        if (form == Form.DUST && player != null && player.isSecondaryUseActive() && state.is(Blocks.WATER_CAULDRON)) {
            if (!level.isClientSide) {
                stack.consume(1, player);
                ItemStack refined = Materials.of(RIRegistries.REFINED_DUST.get(), material, 1);
                if (!player.addItem(refined)) {
                    player.drop(refined, false);
                }
                LayeredCauldronBlock.lowerFillLevel(state, level, context.getClickedPos());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (form != Form.RUBBLE && player != null && !player.isSecondaryUseActive()) {
            return DustPileBlock.placeOrGrow(context, material, form == Form.REFINED_DUST);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (form == Form.DUST) {
            tooltip.add(Component.translatable("tooltip.resonantinduction.dust").withStyle(ChatFormatting.GRAY));
        }
    }
}
