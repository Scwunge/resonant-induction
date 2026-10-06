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
import resonantinduction.archaic.CrateBlockEntity;

/** The crate's item and count on each side (or "Empty"), like the original's side overlay. */
public class CrateRenderer implements BlockEntityRenderer<CrateBlockEntity> {
    private final Font font;

    public CrateRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(CrateBlockEntity crate, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack sample = crate.sample();
        Component label = sample.isEmpty() ? Component.translatable("tooltip.resonantinduction.empty") : Component.literal(String.valueOf(crate.count()));
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (crate.getLevel() != null && crate.getLevel().getBlockState(crate.getBlockPos().relative(side)).isSolidRender(crate.getLevel(), crate.getBlockPos().relative(side))) {
                continue;
            }
            int faceLight = crate.getLevel() == null ? light : LightTexture.pack(
                    crate.getLevel().getBrightness(net.minecraft.world.level.LightLayer.BLOCK, crate.getBlockPos().relative(side)),
                    crate.getLevel().getBrightness(net.minecraft.world.level.LightLayer.SKY, crate.getBlockPos().relative(side)));
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-side.toYRot()));
            pose.translate(0, 0, 0.502);
            if (!sample.isEmpty()) {
                pose.pushPose();
                pose.translate(0, 0.06, 0);
                pose.scale(0.5f, 0.5f, 0.01f);
                Minecraft.getInstance().getItemRenderer().renderStatic(sample, ItemDisplayContext.GUI, faceLight, OverlayTexture.NO_OVERLAY,
                        pose, buffers, crate.getLevel(), (int) crate.getBlockPos().asLong());
                pose.popPose();
            }
            pose.translate(0, -0.3, 0.001);
            pose.scale(0.012f, -0.012f, 0.012f);
            font.drawInBatch(label, -font.width(label) / 2f, 0, 0xFFFFFFFF, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, faceLight);
            pose.popPose();
        }
    }
}
