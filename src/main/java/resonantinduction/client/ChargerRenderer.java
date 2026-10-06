package resonantinduction.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import resonantinduction.charger.ChargerBlock;
import resonantinduction.charger.ChargerBlockEntity;

/** Shows the item lying on the pad, and its charge when you look at the charger, like the original. */
public class ChargerRenderer implements BlockEntityRenderer<ChargerBlockEntity> {
    private final Font font;

    public ChargerRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(ChargerBlockEntity charger, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack stack = charger.getItem();
        if (stack.isEmpty()) {
            return;
        }
        Direction facing = charger.getBlockState().getValue(ChargerBlock.FACING);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(facing.getRotation());
        pose.translate(0, -0.22, 0);
        pose.mulPose(Axis.XP.rotationDegrees(90));
        pose.scale(0.5f, 0.5f, 0.5f);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, pose, buffers, charger.getLevel(), (int) charger.getBlockPos().asLong());
        pose.popPose();

        HitResult hit = Minecraft.getInstance().hitResult;
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK && blockHit.getBlockPos().equals(charger.getBlockPos())) {
            IEnergyStorage energy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (energy != null) {
                Component text = Component.translatable("tooltip.resonantinduction.energy", energy.getEnergyStored(), energy.getMaxEnergyStored());
                pose.pushPose();
                pose.translate(0.5, 0.5 + facing.getStepY() * 0.4 + 0.4, 0.5);
                pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
                pose.scale(0.02f, -0.02f, 0.02f);
                float x = -font.width(text) / 2f;
                font.drawInBatch(text, x, 0, 0xFFFFFFFF, false, pose.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, 0x40000000, LightTexture.FULL_BRIGHT);
                pose.popPose();
            }
        }
    }
}
