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
import resonantinduction.archaic.HotPlateBlockEntity;

/** Lays the four stacks flat on the plate, one per quarter, like the original's top overlay. */
public class HotPlateRenderer implements BlockEntityRenderer<HotPlateBlockEntity> {
    public HotPlateRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(HotPlateBlockEntity plate, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        for (int i = 0; i < HotPlateBlockEntity.SLOTS; i++) {
            ItemStack stack = plate.inventory().getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            pose.pushPose();
            pose.translate((i / 2) * 0.5 + 0.25, 0.21, (i % 2) * 0.5 + 0.25);
            pose.mulPose(Axis.XP.rotationDegrees(90));
            pose.scale(0.4f, 0.4f, 0.4f);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                    pose, buffers, plate.getLevel(), (int) plate.getBlockPos().asLong() + i);
            pose.popPose();
        }
    }
}
