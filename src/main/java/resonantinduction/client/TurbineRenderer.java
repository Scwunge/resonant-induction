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
import resonantinduction.mechanical.turbine.TurbineBlock;
import resonantinduction.mechanical.turbine.TurbineBlockEntity;

import java.util.ArrayList;
import java.util.List;

/** Wind turbines and waterwheels, drawn from the original OBJ parts with the original transforms. Only the centre block draws. */
public class TurbineRenderer implements BlockEntityRenderer<TurbineBlockEntity> {
    static final String[] TIER_TEX = {"oak", "cobble", "iron"};

    public static ModelResourceLocation part(String model, String group, String texture) {
        return ModelResourceLocation.standalone(ResonantInduction.id("block/turbine/" + model + "_" + group.toLowerCase() + "_" + texture));
    }

    /** Every (model, group, texture) the renderer uses, to register as standalone models. */
    public static List<ModelResourceLocation> allParts() {
        List<ModelResourceLocation> out = new ArrayList<>();
        for (String t : TIER_TEX) {
            for (String g : new String[]{"SmallBlade", "LargeBladeArm", "LargeHub"}) {
                out.add(part("wind", g, t));
            }
            for (String g : new String[]{"turbine_centre", "turbine_blades", "small_turbine_blades", "small_waterwheel", "small_waterwheel_supporters",
                    "horizontal_centre_shaft", "bigwheel_scoops", "bigwheel_supportercircle"}) {
                out.add(part("water", g, t));
            }
        }
        out.add(part("wind", "SmallHub", "log"));
        out.add(part("wind", "LargeBlade", "wool"));
        out.add(part("wind", "LargeMetalBlade", "iron"));
        out.add(part("wind", "LargeMetalHub", "iron"));
        out.add(part("water", "small_waterwheel_endknot", "cobble"));
        out.add(part("water", "small_waterwheel_endknot", "log"));
        out.add(part("water", "bigwheel_endknot", "cobble"));
        out.add(part("water", "horizontal_centre_shaft", "cobble"));
        out.add(part("water", "bigwheel_supporters", "spruce"));
        return out;
    }

    public TurbineRenderer(BlockEntityRendererProvider.Context context) {}

    private PoseStack pose;
    private MultiBufferSource buffers;
    private TurbineBlockEntity be;
    private int light;
    private int overlay;

    private void draw(String model, String group, String texture) {
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()),
                be.getBlockState(), Minecraft.getInstance().getModelManager().getModel(part(model, group, texture)), 1f, 1f, 1f, light, overlay);
    }

    @Override
    public void render(TurbineBlockEntity turbine, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!turbine.isPrimary()) {
            return;
        }
        this.pose = pose;
        this.buffers = buffers;
        this.be = turbine;
        this.light = light;
        this.overlay = overlay;
        TurbineBlock block = turbine.block();
        String tier = TIER_TEX[Math.min(block.tier(), 2)];
        boolean large = turbine.isConstructed();
        int size = TurbineBlockEntity.RADIUS;
        float angle = (float) Mth.lerp(partialTick, turbine.node().prevAngle, turbine.node().angle);

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        MechanicalRenderer.orientUp(pose, turbine.facing());
        if (block.kind() == TurbineBlock.Kind.WIND) {
            pose.translate(0, -0.35, 0);
            pose.mulPose(Axis.YP.rotation(angle));
            if (large) {
                pose.scale(0.3f * (size * 2 + 1), Math.min(size, 2), 0.3f * (size * 2 + 1));
                if (block.tier() == 2) {
                    pose.translate(0, -0.11, 0);
                    draw("wind", "LargeMetalBlade", "iron");
                    draw("wind", "LargeMetalHub", "iron");
                } else {
                    draw("wind", "LargeBladeArm", tier);
                    pose.scale(1, 2, 1);
                    pose.translate(0, -0.05, 0);
                    draw("wind", "LargeHub", tier);
                    draw("wind", "LargeBlade", "wool");
                }
            } else {
                draw("wind", "SmallBlade", tier);
                draw("wind", "SmallHub", "log");
            }
        } else {
            pose.mulPose(Axis.YP.rotation(angle));
            boolean vertical = turbine.facing().getAxis() == Direction.Axis.Y;
            if (vertical) {
                if (large) {
                    pose.scale(0.3f * (size * 2 + 1), Math.min(size, 2), 0.3f * (size * 2 + 1));
                    draw("water", "turbine_centre", tier);
                    draw("water", "turbine_blades", tier);
                } else {
                    pose.scale(0.9f, 1, 0.9f);
                    draw("water", "small_waterwheel_endknot", "log");
                    draw("water", "small_turbine_blades", tier);
                }
            } else if (large) {
                pose.scale(0.3f * (size * 2 + 1), Math.min(size, 2), 0.3f * (size * 2 + 1));
                pose.pushPose();
                pose.scale(1, 1.6f, 1);
                draw("water", "bigwheel_endknot", "cobble");
                draw("water", "horizontal_centre_shaft", "cobble");
                pose.popPose();
                pose.scale(1, 1.4f, 1);
                draw("water", "bigwheel_supporters", "spruce");
                draw("water", "bigwheel_scoops", tier);
                draw("water", "bigwheel_supportercircle", tier);
            } else {
                pose.scale(0.7f, 1, 0.7f);
                draw("water", "small_waterwheel_endknot", "cobble");
                draw("water", "small_waterwheel", tier);
                draw("water", "small_waterwheel_supporters", tier);
                draw("water", "horizontal_centre_shaft", tier);
            }
        }
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(TurbineBlockEntity turbine) {
        return new AABB(turbine.getBlockPos()).inflate(TurbineBlockEntity.RADIUS + 1);
    }
}
