package resonantinduction.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;
import resonantinduction.ResonantInduction;
import resonantinduction.quantum.QuantumGateBlockEntity;

/** Draws each glyph as a textured half-block cube in its corner, like the original RenderQuantumGlyph. */
public class QuantumGateRenderer implements BlockEntityRenderer<QuantumGateBlockEntity> {
    public QuantumGateRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(QuantumGateBlockEntity gate, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
        for (int slot = 0; slot < QuantumGateBlockEntity.SLOTS; slot++) {
            int glyph = gate.glyph(slot);
            if (glyph < 0) {
                continue;
            }
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(ResonantInduction.id("block/glyph_" + glyph));
            cube(consumer, pose.last(), QuantumGateBlockEntity.slotBox(slot).deflate(0.02), sprite, light);
        }
    }

    private static void cube(VertexConsumer c, PoseStack.Pose pose, AABB b, TextureAtlasSprite s, int light) {
        float x0 = (float) b.minX, y0 = (float) b.minY, z0 = (float) b.minZ;
        float x1 = (float) b.maxX, y1 = (float) b.maxY, z1 = (float) b.maxZ;
        float u0 = s.getU0(), u1 = s.getU1(), v0 = s.getV0(), v1 = s.getV1();
        face(c, pose, Direction.DOWN, light, u0, v0, u1, v1, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1);
        face(c, pose, Direction.UP, light, u0, v0, u1, v1, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        face(c, pose, Direction.NORTH, light, u0, v0, u1, v1, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0);
        face(c, pose, Direction.SOUTH, light, u0, v0, u1, v1, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1);
        face(c, pose, Direction.WEST, light, u0, v0, u1, v1, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1);
        face(c, pose, Direction.EAST, light, u0, v0, u1, v1, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0);
    }

    private static void face(VertexConsumer c, PoseStack.Pose pose, Direction dir, int light, float u0, float v0, float u1, float v1,
                             float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz) {
        Vector3f n = dir.step();
        vertex(c, pose, ax, ay, az, u0, v0, n, light);
        vertex(c, pose, bx, by, bz, u0, v1, n, light);
        vertex(c, pose, cx, cy, cz, u1, v1, n, light);
        vertex(c, pose, dx, dy, dz, u1, v0, n, light);
    }

    private static void vertex(VertexConsumer c, PoseStack.Pose pose, float x, float y, float z, float u, float v, Vector3f n, int light) {
        c.addVertex(pose, x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, n.x(), n.y(), n.z());
    }
}
