package resonantinduction.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import resonantinduction.ResonantInduction;
import resonantinduction.mechanical.MechanicalBlockEntity;
import resonantinduction.mechanical.gear.GearBlockEntity;
import resonantinduction.mechanical.shaft.ShaftBlockEntity;

/**
 * Spinning gears and shafts. The original's OBJ parts are baked as standalone models lying flat (gears on the floor, shafts
 * upright) and turned here by the node's angle.
 */
public class MechanicalRenderer<T extends MechanicalBlockEntity> implements BlockEntityRenderer<T> {
    public static final String[] TIERS = {"wood", "stone", "metal", "creative"};

    public static ModelResourceLocation model(String name) {
        return ModelResourceLocation.standalone(ResonantInduction.id("block/mechanical/" + name));
    }

    public MechanicalRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(T be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float angle = (float) Mth.lerp(partialTick, be.node().prevAngle, be.node().angle);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        String name;
        if (be instanceof GearBlockEntity gear) {
            // Turn the floor-lying gear onto its face, then spin it about its own axis.
            orientUp(pose, gear.attach().getOpposite());
            pose.mulPose(Axis.YP.rotation(angle));
            name = "gear_small_" + TIERS[Math.min(gear.tier(), 3)];
        } else if (be instanceof ShaftBlockEntity shaft) {
            switch (shaft.axis()) {
                case X -> pose.mulPose(Axis.ZP.rotationDegrees(90));
                case Z -> pose.mulPose(Axis.XP.rotationDegrees(90));
                default -> {
                }
            }
            pose.mulPose(Axis.YP.rotation(angle));
            name = "shaft_" + TIERS[Math.min(shaft.tier(), 2)];
        } else {
            pose.popPose();
            return;
        }
        pose.translate(-0.5, -0.5, -0.5);
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(model(name));
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()),
                be.getBlockState(), model, 1f, 1f, 1f, light, overlay);
        pose.popPose();
    }

    /** Rotate so the model's +Y points along {@code up}. */
    static void orientUp(PoseStack pose, Direction up) {
        switch (up) {
            case DOWN -> pose.mulPose(Axis.XP.rotationDegrees(180));
            case NORTH -> pose.mulPose(Axis.XP.rotationDegrees(-90));
            case SOUTH -> pose.mulPose(Axis.XP.rotationDegrees(90));
            case WEST -> pose.mulPose(Axis.ZP.rotationDegrees(90));
            case EAST -> pose.mulPose(Axis.ZP.rotationDegrees(-90));
            default -> {
            }
        }
    }
}
