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
import resonantinduction.ResonantInduction;
import resonantinduction.battery.BatteryBlockEntity;

/** Draws the input/output plugs on a battery's sides (blue in, orange out), from the original model's connector parts. */
public class BatteryRenderer implements BlockEntityRenderer<BatteryBlockEntity> {
    public static final ModelResourceLocation CONNECTOR_IN = ModelResourceLocation.standalone(ResonantInduction.id("block/battery/connector_in"));
    public static final ModelResourceLocation CONNECTOR_OUT = ModelResourceLocation.standalone(ResonantInduction.id("block/battery/connector_out"));

    public BatteryRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(BatteryBlockEntity battery, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            byte io = battery.io(side);
            if (io == BatteryBlockEntity.IO_NONE) {
                continue;
            }
            BakedModel model = Minecraft.getInstance().getModelManager().getModel(io == BatteryBlockEntity.IO_INPUT ? CONNECTOR_IN : CONNECTOR_OUT);
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            // The connector parts sit on the north face.
            pose.mulPose(Axis.YP.rotationDegrees(switch (side) {
                case EAST -> -90;
                case SOUTH -> 180;
                case WEST -> 90;
                default -> 0;
            }));
            pose.translate(-0.5, -0.5, -0.5);
            Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()),
                    battery.getBlockState(), model, 1f, 1f, 1f, light, overlay);
            pose.popPose();
        }
    }
}
