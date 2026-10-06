package resonantinduction.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import resonantinduction.ResonantInduction;
import resonantinduction.atomic.reactor.ElectricTurbineBlockEntity;
import resonantinduction.atomic.reactor.ReactorCellBlockEntity;
import resonantinduction.atomic.reactor.ThermometerBlock;

import java.util.List;

/** The reactor's moving and live parts: fuel glowing in the cell column, the thermometer's reading, the turbine's blades. */
public final class ReactorRenderers {
    private ReactorRenderers() {}

    /** The fuel rod inside a column of cells, as tall as the column and shrinking as it burns, as the original. */
    public static class Cell implements BlockEntityRenderer<ReactorCellBlockEntity> {
        public Cell(BlockEntityRendererProvider.Context context) {}

        @Override
        public void render(ReactorCellBlockEntity cell, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            if (cell.getLevel() == null || !cell.isPrimary()) {
                return;
            }
            ItemStack rod = cell.inventory().getStackInSlot(0);
            if (rod.isEmpty()) {
                return;
            }
            float height = cell.height() * (float) (rod.getMaxDamage() - rod.getDamageValue()) / rod.getMaxDamage() * 0.9f;
            var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResonantInduction.id("block/model/fissile_material"));
            FluidRender.spriteBox(pose, buffers.getBuffer(RenderType.cutout()), sprite, 0xFFFFFFFF, 0.3f, 0.05f, 0.3f, 0.7f, 0.05f + height, 0.7f,
                    LightTexture.FULL_BRIGHT, 0);
        }

        @Override
        public AABB getRenderBoundingBox(ReactorCellBlockEntity cell) {
            return new AABB(cell.getBlockPos()).expandTowards(0, 8, 0);
        }
    }

    /** The temperature on each side, red at or over the warning level, as the original. */
    public static class Thermometer implements BlockEntityRenderer<ThermometerBlock.Tile> {
        private final Font font;

        public Thermometer(BlockEntityRendererProvider.Context context) {
            this.font = context.getFont();
        }

        @Override
        public void render(ThermometerBlock.Tile t, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            Component text = Component.literal((int) t.detected() + " K");
            Component limit = Component.literal("/" + t.threshold());
            int color = t.detected() >= t.threshold() ? 0xFFFF4040 : 0xFF40FF40;
            for (Direction side : Direction.Plane.HORIZONTAL) {
                int faceLight = t.getLevel() == null ? light : LevelRenderer.getLightColor(t.getLevel(), t.getBlockPos().relative(side));
                pose.pushPose();
                pose.translate(0.5, 0.5, 0.5);
                pose.mulPose(Axis.YP.rotationDegrees(-side.toYRot()));
                pose.translate(0, 0, 0.502);
                pose.scale(0.012f, -0.012f, 0.012f);
                font.drawInBatch(text, -font.width(text) / 2f, -8, color, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, faceLight);
                font.drawInBatch(limit, -font.width(limit) / 2f, 4, 0xFFFFFFFF, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, faceLight);
                pose.popPose();
            }
        }
    }

    record Part(ModelResourceLocation model, float[] angles, int spin) {}

    static ModelResourceLocation part(String name) {
        return ModelResourceLocation.standalone(ResonantInduction.id("block/atomic/" + name));
    }

    /** The turbine models: pieces drawn at each of the original's angles about the centre line, spinning one way or the other. */
    public static final List<Part> SMALL = List.of(
            new Part(part("turbine_small_static"), new float[] {0}, 0),
            new Part(part("turbine_small_blade_a"), new float[] {120, 60, 0}, 1),
            new Part(part("turbine_small_shield_a"), new float[] {-180, 60, -60, -120, 0, 120}, 1),
            new Part(part("turbine_small_blade_b"), new float[] {90, 150, 30}, -1),
            new Part(part("turbine_small_shield_b"), new float[] {-30, 90, 30, -90, 150, -150}, -1));
    public static final List<Part> LARGE = List.of(
            new Part(part("turbine_large_static"), new float[] {0}, 0),
            new Part(part("turbine_large_shroud"), new float[] {0, 60, -60, -120, 120}, 0),
            new Part(part("turbine_large_ring"), new float[] {0, 120, 60, -60, -120}, 0),
            new Part(part("turbine_large_rod"), new float[] {30, 150, 90}, 0),
            new Part(part("turbine_large_blade"), new float[] {0, 30, 60, 90, 120, 150}, 1),
            new Part(part("turbine_large_blade_large"), new float[] {120, 150, 0, 30, 60, 90}, 1),
            new Part(part("turbine_large_blade_medium"), new float[] {120, 150, 0, 30, 60, 90}, -1));

    public static class Turbine implements BlockEntityRenderer<ElectricTurbineBlockEntity> {
        public Turbine(BlockEntityRendererProvider.Context context) {}

        @Override
        public void render(ElectricTurbineBlockEntity t, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            if (t.isMember()) {
                return;
            }
            float spin = Mth.lerp(partialTick, t.prevAngle, t.angle);
            var renderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
            for (Part part : t.formed() ? LARGE : SMALL) {
                BakedModel model = Minecraft.getInstance().getModelManager().getModel(part.model());
                for (float angle : part.angles()) {
                    pose.pushPose();
                    pose.translate(0.5, 0, 0.5);
                    pose.mulPose(Axis.YP.rotation(spin * part.spin()));
                    pose.mulPose(Axis.YP.rotationDegrees(-angle));
                    pose.translate(-0.5, 0, -0.5);
                    renderer.renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()), t.getBlockState(), model, 1f, 1f, 1f, light, overlay);
                    pose.popPose();
                }
            }
        }

        @Override
        public AABB getRenderBoundingBox(ElectricTurbineBlockEntity t) {
            return t.renderBox();
        }
    }
}
