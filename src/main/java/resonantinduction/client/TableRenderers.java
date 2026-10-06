package resonantinduction.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import resonantinduction.archaic.EngineeringTableBlock;
import resonantinduction.archaic.EngineeringTableBlockEntity;
import resonantinduction.archaic.GridFace;
import resonantinduction.archaic.ImprinterBlockEntity;

/** Items laid on the 3x3 tops of the Engineering Table and Imprinter, and what they'd give, shown on the sides. */
public final class TableRenderers {
    private TableRenderers() {}

    static void flat(PoseStack pose, MultiBufferSource buffers, BlockEntity be, ItemStack stack, double x, double y, double z, float scale, int light, int seed) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.XP.rotationDegrees(90));
        pose.scale(scale, scale, scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, be.getLevel(), seed);
        pose.popPose();
    }

    /** The light in the space next to a block, for things drawn on its faces (inside a solid block it's dark). */
    static int lightAt(BlockEntity be, Direction side, int fallback) {
        return be.getLevel() == null ? fallback : LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(side));
    }

    static void sides(PoseStack pose, MultiBufferSource buffers, BlockEntity be, ItemStack stack, double height, int light) {
        if (stack.isEmpty()) {
            return;
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            int faceLight = lightAt(be, side, light);
            pose.pushPose();
            pose.translate(0.5, height, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-side.toYRot()));
            pose.translate(0, 0, 0.505);
            pose.scale(0.4f, 0.4f, 0.4f);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, faceLight, OverlayTexture.NO_OVERLAY, pose, buffers,
                    be.getLevel(), (int) be.getBlockPos().asLong() + side.ordinal());
            pose.popPose();
        }
    }

    public static class EngineeringTable implements BlockEntityRenderer<EngineeringTableBlockEntity> {
        public EngineeringTable(BlockEntityRendererProvider.Context context) {}

        @Override
        public void render(EngineeringTableBlockEntity table, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            Direction front = table.getBlockState().getValue(EngineeringTableBlock.FACING);
            for (int i = 0; i < 9; i++) {
                ItemStack s = table.grid().get(i);
                if (!s.isEmpty()) {
                    double[] c = GridFace.centre(i, front);
                    flat(pose, buffers, table, s, c[0], 0.91, c[1], 0.25f, light, (int) table.getBlockPos().asLong() + i);
                }
            }
            sides(pose, buffers, table, table.output(), 0.5, light);
        }
    }

    /** The placer's stack on its four sides (not front or back), as the original. */
    public static class Placer implements BlockEntityRenderer<resonantinduction.logistic.PlacerBlockEntity> {
        public Placer(BlockEntityRendererProvider.Context context) {}

        @Override
        public void render(resonantinduction.logistic.PlacerBlockEntity placer, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            ItemStack stack = placer.inventory().getStackInSlot(0);
            if (stack.isEmpty()) {
                return;
            }
            Direction facing = placer.facing();
            for (Direction side : Direction.values()) {
                if (side.getAxis() == facing.getAxis()) {
                    continue;
                }
                pose.pushPose();
                pose.translate(0.5, 0.5, 0.5);
                pose.mulPose(side.getRotation());
                pose.translate(0, 0.505, 0);
                pose.mulPose(Axis.XP.rotationDegrees(-90));
                pose.scale(0.4f, 0.4f, 0.4f);
                Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, lightAt(placer, side, light), OverlayTexture.NO_OVERLAY, pose,
                        buffers, placer.getLevel(), (int) placer.getBlockPos().asLong() + side.ordinal());
                pose.popPose();
            }
        }
    }

    public static class Imprinter implements BlockEntityRenderer<ImprinterBlockEntity> {
        public Imprinter(BlockEntityRendererProvider.Context context) {}

        @Override
        public void render(ImprinterBlockEntity imprinter, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            for (int i = 0; i < 9; i++) {
                ItemStack s = imprinter.inventory().getStackInSlot(i);
                if (!s.isEmpty()) {
                    flat(pose, buffers, imprinter, s, (i % 3 + 0.5) / 3, 1.01, (i / 3 + 0.5) / 3, 0.25f, lightAt(imprinter, Direction.UP, light),
                            (int) imprinter.getBlockPos().asLong() + i);
                }
            }
            sides(pose, buffers, imprinter, imprinter.inventory().getStackInSlot(ImprinterBlockEntity.IMPRINT_SLOT), 0.5, light);
        }
    }
}
