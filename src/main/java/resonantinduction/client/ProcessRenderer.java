package resonantinduction.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import resonantinduction.ResonantInduction;
import resonantinduction.mechanical.MechanicalBlockEntity;
import resonantinduction.mechanical.process.GrindingWheelBlockEntity;
import resonantinduction.mechanical.process.MechanicalPistonBlockEntity;
import resonantinduction.mechanical.process.MixerBlockEntity;

/** Moving parts of the processing machines, from the original models: piston rotor and ram, grinding wheel, mixer arms. */
public class ProcessRenderer implements BlockEntityRenderer<MechanicalBlockEntity> {
    public static final ModelResourceLocation PISTON_ROTOR = part("piston_rotor");
    public static final ModelResourceLocation PISTON_SHAFT = part("piston_shaft");
    public static final ModelResourceLocation GRINDER_WHEEL = part("grinder_wheel");
    public static final ModelResourceLocation GRINDER_TEETH = part("grinder_teeth");
    public static final ModelResourceLocation MIXER_ROTOR = part("mixer_rotor");

    static ModelResourceLocation part(String name) {
        return ModelResourceLocation.standalone(ResonantInduction.id("block/process/" + name));
    }

    public ProcessRenderer(BlockEntityRendererProvider.Context context) {}

    private void draw(PoseStack pose, MultiBufferSource buffers, MechanicalBlockEntity be, ModelResourceLocation id, int light, int overlay) {
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()), be.getBlockState(),
                Minecraft.getInstance().getModelManager().getModel(id), 1f, 1f, 1f, light, overlay);
    }

    /** Turn a model built facing north to face {@code dir}, about the block centre. */
    static void face(PoseStack pose, Direction dir) {
        switch (dir) {
            case SOUTH -> pose.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(-90));
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(90));
            case UP -> pose.mulPose(Axis.XP.rotationDegrees(90));
            case DOWN -> pose.mulPose(Axis.XP.rotationDegrees(-90));
            default -> {
            }
        }
    }

    @Override
    public void render(MechanicalBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float angle = (float) Mth.lerp(partialTick, be.node().prevAngle, be.node().angle);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        if (be instanceof MechanicalPistonBlockEntity piston) {
            face(pose, piston.facing());
            pose.pushPose();
            pose.mulPose(Axis.ZP.rotation(-angle));
            pose.translate(-0.5, -0.5, -0.5);
            draw(pose, buffers, be, PISTON_ROTOR, light, overlay);
            pose.popPose();
            boolean open = be.getLevel() != null && be.getLevel().isEmptyBlock(be.getBlockPos().relative(piston.facing()));
            double reach = open ? 0.4 * Math.sin(angle) - 0.5 : 0.06 * Math.sin(angle) - 0.03;
            pose.translate(-0.5, -0.5, -0.5 + reach);
            draw(pose, buffers, be, PISTON_SHAFT, light, overlay);
        } else if (be instanceof GrindingWheelBlockEntity wheel) {
            // The wheel model turns about its own z axis; line that up with the axle.
            switch (wheel.axle()) {
                case X -> pose.mulPose(Axis.YP.rotationDegrees(90));
                case Y -> pose.mulPose(Axis.XP.rotationDegrees(90));
                default -> {
                }
            }
            pose.scale(0.51f, 0.5f, 0.5f);
            pose.mulPose(Axis.ZP.rotation(angle));
            draw(pose, buffers, be, GRINDER_WHEEL, light, overlay);
            draw(pose, buffers, be, GRINDER_TEETH, light, overlay);
        } else if (be instanceof MixerBlockEntity) {
            pose.mulPose(Axis.YP.rotation(angle));
            pose.translate(-0.5, -0.5, -0.5);
            draw(pose, buffers, be, MIXER_ROTOR, light, overlay);
        }
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(MechanicalBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(1);
    }
}
