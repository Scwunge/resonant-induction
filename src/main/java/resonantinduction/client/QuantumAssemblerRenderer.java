package resonantinduction.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import resonantinduction.ResonantInduction;
import resonantinduction.atomic.particle.QuantumAssemblerBlockEntity;

import java.util.List;
import java.util.function.Predicate;

/**
 * The Quantum Assembler, as the original: three sets of arms turning at different speeds round the item being copied, which
 * floats in the middle, while it works.
 */
public class QuantumAssemblerRenderer implements BlockEntityRenderer<QuantumAssemblerBlockEntity> {
    public static final TechneModel MODEL = new TechneModel(ResonantInduction.id("models/techne/quantum_assembler.tcn"));
    public static final ResourceLocation TEXTURE = ResonantInduction.id("textures/block/model/quantum_assembler.png");
    private static final List<String> HANDS = List.of("Back Arm Upper", "Back Arm Lower", "Right Arm Upper", "Right Arm Lower", "Front Arm Upper",
            "Front Arm Lower", "Left Arm Upper", "Left Arm Lower", "Resonance Crystal");
    private static final List<String> ARMS = List.of("Middle Rotor Focus Lazer", "Middle Rotor Uppper Arm", "Middle Rotor Lower Arm", "Middle Rotor Arm Base",
            "Middle Rotor");
    private static final List<String> LARGE_ARMS = List.of("Bottom Rotor Upper Arm", "Bottom Rotor Lower Arm", "Bottom Rotor Arm Base", "Bottom Rotor",
            "Bottom Rotor Resonator Arm");

    public QuantumAssemblerRenderer(BlockEntityRendererProvider.Context context) {}

    /** Draws the machine with its arms turned by {@code time} ticks of work. */
    static void draw(PoseStack pose, MultiBufferSource buffers, float time, int light, int overlay) {
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        pose.pushPose();
        TechneModel.enterBlockSpace(pose);
        part(pose, buffer, light, overlay, -3 * time, HANDS::contains);
        part(pose, buffer, light, overlay, 2 * time, ARMS::contains);
        part(pose, buffer, light, overlay, -time, LARGE_ARMS::contains);
        MODEL.render(pose, buffer, light, overlay, n -> !HANDS.contains(n) && !ARMS.contains(n) && !LARGE_ARMS.contains(n));
        pose.popPose();
    }

    private static void part(PoseStack pose, VertexConsumer buffer, int light, int overlay, float degrees, Predicate<String> names) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(degrees));
        MODEL.render(pose, buffer, light, overlay, names);
        pose.popPose();
    }

    @Override
    public void render(QuantumAssemblerBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        boolean working = be.timer() > 0;
        float time = working && be.getLevel() != null ? be.getLevel().getGameTime() + partialTick : 0;
        draw(pose, buffers, time, light, overlay);
        ItemStack target = be.target();
        if (working && !target.isEmpty()) {
            pose.pushPose();
            pose.translate(0.5, 0.4 + Mth.sin(time / 10f) * 0.1f + 0.1f, 0.5);
            pose.mulPose(Axis.YP.rotation(time / 20f));
            Minecraft.getInstance().getItemRenderer().renderStatic(target.copyWithCount(1), ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, pose,
                    buffers, be.getLevel(), 0);
            pose.popPose();
        }
    }

    /** The item form: the machine, still. */
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
