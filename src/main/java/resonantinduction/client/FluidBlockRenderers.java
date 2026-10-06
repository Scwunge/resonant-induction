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
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import resonantinduction.ResonantInduction;
import resonantinduction.fluid.GutterBlock;
import resonantinduction.fluid.GutterBlockEntity;
import resonantinduction.fluid.PipeBlock;
import resonantinduction.fluid.PipeBlockEntity;
import resonantinduction.fluid.PumpBlockEntity;
import resonantinduction.fluid.TankBlock;
import resonantinduction.fluid.TankBlockEntity;

/** Fluid inside gutters, tanks and pipes, and the pump's turning fins. */
public final class FluidBlockRenderers {
    private FluidBlockRenderers() {}

    private static int bit(Direction d) {
        return 1 << d.ordinal();
    }

    /** The water line in a gutter, open toward the gutters beside it, as the original (shown once it's a tenth full). */
    public static class Gutter implements BlockEntityRenderer<GutterBlockEntity> {
        public Gutter(BlockEntityRendererProvider.Context context) {}

        @Override
        public void render(GutterBlockEntity gutter, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            FluidStack fluid = gutter.tank().getFluid();
            float fill = (float) fluid.getAmount() / gutter.tank().getCapacity();
            if (fill <= 0.1f) {
                return;
            }
            BlockState state = gutter.getBlockState();
            float t = 0.1f;
            boolean n = state.getValue(GutterBlock.SIDES.get(Direction.NORTH));
            boolean s = state.getValue(GutterBlock.SIDES.get(Direction.SOUTH));
            boolean w = state.getValue(GutterBlock.SIDES.get(Direction.WEST));
            boolean e = state.getValue(GutterBlock.SIDES.get(Direction.EAST));
            boolean down = state.getValue(GutterBlock.DOWN);
            int skip = (down ? bit(Direction.DOWN) : 0) | (n ? bit(Direction.NORTH) : 0) | (s ? bit(Direction.SOUTH) : 0)
                    | (w ? bit(Direction.WEST) : 0) | (e ? bit(Direction.EAST) : 0);
            float y0 = down ? 0 : 2 / 16f;
            FluidRender.box(pose, buffers, fluid, w ? 0 : t, y0, n ? 0 : t, e ? 1 : 1 - t, y0 + (0.99f - y0) * fill, s ? 1 : 1 - t, light, skip);
        }
    }

    /** A tank's share of the fluid; faces against neighbouring tanks are left out so joined tanks read as one. */
    public static class Tank implements BlockEntityRenderer<TankBlockEntity> {
        public Tank(BlockEntityRendererProvider.Context context) {}

        @Override
        public void render(TankBlockEntity tank, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            FluidStack fluid = tank.tank().getFluid();
            if (fluid.isEmpty()) {
                return;
            }
            float fill = Math.min(1f, (float) fluid.getAmount() / tank.tank().getCapacity());
            BlockState state = tank.getBlockState();
            int skip = 0;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (state.getValue(TankBlock.SIDES.get(d))) {
                    skip |= bit(d);
                }
            }
            boolean below = state.getValue(TankBlock.SIDES.get(Direction.DOWN));
            boolean above = state.getValue(TankBlock.SIDES.get(Direction.UP));
            if (below) {
                skip |= bit(Direction.DOWN);
            }
            if (above && fill >= 1f) {
                skip |= bit(Direction.UP);
            }
            float e = 0.005f;
            float x0 = (skip & bit(Direction.WEST)) != 0 ? 0 : e;
            float x1 = (skip & bit(Direction.EAST)) != 0 ? 1 : 1 - e;
            float z0 = (skip & bit(Direction.NORTH)) != 0 ? 0 : e;
            float z1 = (skip & bit(Direction.SOUTH)) != 0 ? 1 : 1 - e;
            float y0 = below ? 0 : e;
            float y1 = fill >= 1f && above ? 1 : y0 + (1 - e - y0) * fill;
            if (fluid.getFluidType().isLighterThanAir()) {
                y0 = 1 - (1 - e) * fill;
                y1 = 1 - e;
            }
            FluidRender.box(pose, buffers, fluid, x0, y0, z0, x1, y1, z1, light, skip);
        }
    }

    /** Fluid in the pipe's core and out along each connected arm, as deep as the pipe is full. */
    public static class Pipe implements BlockEntityRenderer<PipeBlockEntity> {
        public Pipe(BlockEntityRendererProvider.Context context) {}

        @Override
        public void render(PipeBlockEntity pipe, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            FluidStack fluid = pipe.tank().getFluid();
            if (fluid.isEmpty()) {
                return;
            }
            float fill = Math.min(1f, (float) fluid.getAmount() / Math.max(1, pipe.tank().getCapacity()));
            float lo = 5.5f / 16;
            float hi = 10.5f / 16;
            float top = lo + (hi - lo) * fill;
            BlockState state = pipe.getBlockState();
            FluidRender.box(pose, buffers, fluid, lo, lo, lo, hi, top, hi, light, 0);
            for (Direction d : Direction.values()) {
                if (!state.getValue(PipeBlock.SIDES.get(d))) {
                    continue;
                }
                float x0 = d == Direction.WEST ? 0 : d == Direction.EAST ? hi : lo;
                float x1 = d == Direction.WEST ? lo : d == Direction.EAST ? 1 : hi;
                float z0 = d == Direction.NORTH ? 0 : d == Direction.SOUTH ? hi : lo;
                float z1 = d == Direction.NORTH ? lo : d == Direction.SOUTH ? 1 : hi;
                float y0 = d == Direction.DOWN ? 0 : d == Direction.UP ? hi : lo;
                float y1 = d == Direction.DOWN ? lo : d == Direction.UP ? 1 : top;
                if (d.getAxis() == Direction.Axis.Y && fill < 1f) {
                    continue;
                }
                FluidRender.box(pose, buffers, fluid, x0, y0, z0, x1, y1, z1, light, bit(d.getOpposite()));
            }
        }
    }

    public static final ModelResourceLocation PUMP_FIN = ModelResourceLocation.standalone(ResonantInduction.id("block/pump_fin"));
    public static final ModelResourceLocation PUMP_INNER_FIN = ModelResourceLocation.standalone(ResonantInduction.id("block/pump_inner_fin"));

    /** The pump's twelve fins and twelve inner fins, turning with its axle. */
    public static class Pump implements BlockEntityRenderer<PumpBlockEntity> {
        public Pump(BlockEntityRendererProvider.Context context) {}

        @Override
        public void render(PumpBlockEntity pump, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            float angle = (float) Mth.lerp(partialTick, pump.node().prevAngle, pump.node().angle);
            var renderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
            var fin = Minecraft.getInstance().getModelManager().getModel(PUMP_FIN);
            var inner = Minecraft.getInstance().getModelManager().getModel(PUMP_INNER_FIN);
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            ProcessRenderer.face(pose, pump.facing());
            for (int i = 0; i < 12; i++) {
                for (int kind = 0; kind < 2; kind++) {
                    pose.pushPose();
                    pose.mulPose(Axis.ZP.rotation(angle));
                    pose.mulPose(Axis.ZP.rotationDegrees(i * 30 + (kind == 1 ? 15 : 0)));
                    pose.translate(-0.5, -0.5, -0.5);
                    renderer.renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()), pump.getBlockState(), kind == 0 ? fin : inner,
                            1f, 1f, 1f, light, overlay);
                    pose.popPose();
                }
            }
            pose.popPose();
        }
    }
}
