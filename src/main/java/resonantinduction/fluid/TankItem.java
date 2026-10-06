package resonantinduction.fluid;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import resonantinduction.registry.RIRegistries;

import java.util.List;

/** A tank item remembers the fluid it was picked up with. */
public class TankItem extends BlockItem {
    public TankItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FluidStack fluid = stack.getOrDefault(RIRegistries.FLUID_CONTENT.get(), SimpleFluidContent.EMPTY).copy();
        if (fluid.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.resonantinduction.tank.empty").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.resonantinduction.tank.fluid", fluid.getHoverName(), fluid.getAmount(), TankBlockEntity.CAPACITY)
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return stack.has(RIRegistries.FLUID_CONTENT.get()) ? 1 : super.getMaxStackSize(stack);
    }
}
