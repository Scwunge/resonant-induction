package resonantinduction.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import resonantinduction.ResonantInduction;
import resonantinduction.atomic.fusion.PlasmaHeaterBlockEntity;
import resonantinduction.multimeter.Measure;

import java.util.ArrayList;
import java.util.List;

/** The Plasma Heater: its rotor spins with the energy stored, and looking at it shows what it holds, as the original. */
public class PlasmaHeaterRenderer implements BlockEntityRenderer<PlasmaHeaterBlockEntity> {
    public static final TechneModel MODEL = new TechneModel(ResonantInduction.id("models/techne/plasma_heater.tcn"));
    public static final ResourceLocation TEXTURE = ResonantInduction.id("textures/block/model/plasma_heater.png");
    private static final List<String> ROTOR = List.of("rrot", "srot");
    private final Font font;

    public PlasmaHeaterRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    static void draw(PoseStack pose, MultiBufferSource buffers, float rotation, int light, int overlay) {
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        pose.pushPose();
        TechneModel.enterBlockSpace(pose);
        MODEL.render(pose, buffer, light, overlay, n -> !ROTOR.contains(n));
        pose.mulPose(Axis.YP.rotation(rotation));
        MODEL.render(pose, buffer, light, overlay, ROTOR::contains);
        pose.popPose();
    }

    @Override
    public void render(PlasmaHeaterBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        draw(pose, buffers, Mth.lerp(partialTick, be.prevRotation, be.rotation), light, overlay);
        HitResult hit = Minecraft.getInstance().hitResult;
        if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK && block.getBlockPos().equals(be.getBlockPos())) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("tooltip.resonantinduction.energy_short", Measure.ENERGY.format(be.energy())));
            for (FluidTank tank : List.of(be.deuterium(), be.tritium(), be.plasma())) {
                if (!tank.isEmpty()) {
                    lines.add(Component.translatable("tooltip.resonantinduction.fluid_short", tank.getFluid().getHoverName(), tank.getFluidAmount()));
                }
            }
            tags(pose, buffers, lines, light);
        }
    }

    /** Floating labels above the block, facing the camera. */
    private void tags(PoseStack pose, MultiBufferSource buffers, List<Component> lines, int light) {
        pose.pushPose();
        pose.translate(0.5, 1.5 + lines.size() * 0.25, 0.5);
        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        pose.scale(0.025f, -0.025f, 0.025f);
        int background = (int) (Minecraft.getInstance().options.getBackgroundOpacity(0.25f) * 255) << 24;
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            font.drawInBatch(line, -font.width(line) / 2f, i * 10, 0xFFFFFFFF, false, pose.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, background, light);
        }
        pose.popPose();
    }

    /** The item form: the heater, still. */
    public static class Item extends BlockEntityWithoutLevelRenderer {
        public Item(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
            super(dispatcher, models);
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            draw(pose, buffers, 0, light, overlay);
        }
    }
}
