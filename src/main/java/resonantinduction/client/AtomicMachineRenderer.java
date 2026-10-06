package resonantinduction.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import resonantinduction.ResonantInduction;
import resonantinduction.atomic.machine.AtomicMachineBlock;
import resonantinduction.atomic.machine.AtomicMachineBlockEntity;
import resonantinduction.atomic.machine.CentrifugeBlockEntity;
import resonantinduction.atomic.machine.ChemicalExtractorBlockEntity;
import resonantinduction.atomic.machine.NuclearBoilerBlockEntity;

/**
 * The atomic machines' moving parts, turning while they work, as the original: the extractor's chamber and magnets, the
 * centrifuge's rotor, and the boiler's two fuel bars (opposite ways).
 */
public class AtomicMachineRenderer implements BlockEntityRenderer<AtomicMachineBlockEntity> {
    public static final ModelResourceLocation EXTRACTOR_ROTOR = part("chemical_extractor_rotor");
    public static final ModelResourceLocation CENTRIFUGE_ROTOR = part("centrifuge_rotor");
    public static final ModelResourceLocation BOILER_BAR_1 = part("nuclear_boiler_bar_1");
    public static final ModelResourceLocation BOILER_BAR_2 = part("nuclear_boiler_bar_2");

    static ModelResourceLocation part(String name) {
        return ModelResourceLocation.standalone(ResonantInduction.id("block/atomic/" + name));
    }

    public AtomicMachineRenderer(BlockEntityRendererProvider.Context context) {}

    private void draw(PoseStack pose, MultiBufferSource buffers, AtomicMachineBlockEntity be, ModelResourceLocation id, int light, int overlay) {
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()), be.getBlockState(),
                Minecraft.getInstance().getModelManager().getModel(id), 1f, 1f, 1f, light, overlay);
    }

    /** Turns about an axis through the point (x, y, z), in model pixels. */
    private static void spin(PoseStack pose, float x, float y, float z, Axis axis, float angle) {
        pose.translate(x / 16, y / 16, z / 16);
        pose.mulPose(axis.rotation(angle));
        pose.translate(-x / 16, -y / 16, -z / 16);
    }

    @Override
    public void render(AtomicMachineBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float time = be.getLevel() == null || be.timer() <= 0 ? 0 : (be.getLevel().getGameTime() + partialTick);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        ProcessRenderer.face(pose, be.getBlockState().getValue(AtomicMachineBlock.FACING));
        pose.translate(-0.5, -0.5, -0.5);
        if (be instanceof ChemicalExtractorBlockEntity) {
            // The chamber and magnets turn about their own axis, which runs along z through (5, 7).
            spin(pose, 5, 7, 8, Axis.ZP, time * 0.2f);
            draw(pose, buffers, be, EXTRACTOR_ROTOR, light, overlay);
        } else if (be instanceof CentrifugeBlockEntity) {
            spin(pose, 8, 8, 8, Axis.YP, time * 0.45f);
            draw(pose, buffers, be, CENTRIFUGE_ROTOR, light, overlay);
        } else if (be instanceof NuclearBoilerBlockEntity) {
            pose.pushPose();
            spin(pose, 5, 8, 13, Axis.YP, time * 0.1f);
            draw(pose, buffers, be, BOILER_BAR_1, light, overlay);
            pose.popPose();
            spin(pose, 11, 8, 13, Axis.YP, -time * 0.1f);
            draw(pose, buffers, be, BOILER_BAR_2, light, overlay);
        }
        pose.popPose();
    }
}
