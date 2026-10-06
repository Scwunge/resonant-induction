package resonantinduction.wire;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Places a flat wire on the clicked face, or a framed (centre) wire when sneaking, as the original did. */
public class WireItem extends BlockItem {
    private final Supplier<? extends Block> framed;
    private final WireMaterial material;

    public WireItem(Block flat, Supplier<? extends Block> framed, WireMaterial material, Item.Properties properties) {
        super(flat, properties);
        this.framed = framed;
        this.material = material;
    }

    public WireMaterial material() {
        return material;
    }

    @Nullable
    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        if (context.getPlayer() != null && context.getPlayer().isSecondaryUseActive()) {
            BlockState state = framed.get().getStateForPlacement(context);
            return state != null && canPlace(context, state) ? state : null;
        }
        return super.getPlacementState(context);
    }

    @Override
    public void registerBlocks(Map<Block, Item> map, Item item) {
        super.registerBlocks(map, item);
        map.put(framed.get(), item);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.resonantinduction.wire.capacity", material.capacity()).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.resonantinduction.wire.resistance", String.format("%.4f", material.resistance)).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.resonantinduction.wire.damage", material.damage).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.resonantinduction.wire.help").withStyle(ChatFormatting.GRAY));
    }
}
