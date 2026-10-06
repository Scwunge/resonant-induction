package resonantinduction.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import resonantinduction.archaic.CastingMoldBlockEntity;

/** Shows the cast ingots on each side of the mold, like the original's side overlay. */
public class CastingMoldRenderer implements BlockEntityRenderer<CastingMoldBlockEntity> {
    public CastingMoldRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(CastingMoldBlockEntity mold, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack stack = mold.output().getStackInSlot(0);
        if (stack.isEmpty()) {
            return;
        }
        for (int side = 0; side < 4; side++) {
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(side * 90));
            pose.translate(0, 0, 0.5 + 0.01);
            pose.scale(0.5f, 0.5f, 0.5f);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                    pose, buffers, mold.getLevel(), (int) mold.getBlockPos().asLong() + side);
            pose.popPose();
        }
    }
}
