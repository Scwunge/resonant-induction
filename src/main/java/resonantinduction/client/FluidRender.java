package resonantinduction.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

/** Draws a box of fluid (its still texture, tinted) for tanks, gutters and pipes. */
public final class FluidRender {
    private FluidRender() {}

    /** A box from (x0,y0,z0) to (x1,y1,z1) in block units. Faces in {@code skip} (bit per direction ordinal) are left out. */
    public static void box(PoseStack pose, MultiBufferSource buffers, FluidStack fluid, float x0, float y0, float z0, float x1, float y1, float z1,
                           int light, int skip) {
        if (fluid.isEmpty() || y1 <= y0) {
            return;
        }
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fluid));
        int glow = fluid.getFluidType().getLightLevel(fluid);
        if (glow > 0) {
            int block = Math.max(light & 0xFFFF, glow << 4);
            light = (light & 0xFFFF0000) | block;
        }
        spriteBox(pose, buffers.getBuffer(RenderType.translucent()), sprite, ext.getTintColor(fluid), x0, y0, z0, x1, y1, z1, light, skip);
    }

    /** A box textured with {@code sprite} (tinted ARGB {@code color}) into {@code vc}. */
    public static void spriteBox(PoseStack pose, VertexConsumer vc, TextureAtlasSprite sprite, int color, float x0, float y0, float z0, float x1, float y1, float z1,
                                 int light, int skip) {
        int a = (color >>> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        Matrix4f m = pose.last().pose();
        float u0 = sprite.getU0();
        float v0 = sprite.getV0();
        float du = sprite.getU1() - u0;
        float dv = sprite.getV1() - v0;
        // down, up, north, south, west, east
        if ((skip & 1) == 0) {
            quad(vc, m, pose, sprite, r, g, b, a, light, 0, -1, 0,
                    x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, u0 + x0 * du, v0 + z0 * dv, u0 + x1 * du, v0 + z1 * dv);
        }
        if ((skip & 2) == 0) {
            quad(vc, m, pose, sprite, r, g, b, a, light, 0, 1, 0,
                    x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, u0 + x0 * du, v0 + z0 * dv, u0 + x1 * du, v0 + z1 * dv);
        }
        if ((skip & 4) == 0) {
            quad(vc, m, pose, sprite, r, g, b, a, light, 0, 0, -1,
                    x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0, u0 + x0 * du, v0 + (1 - y1) * dv, u0 + x1 * du, v0 + (1 - y0) * dv);
        }
        if ((skip & 8) == 0) {
            quad(vc, m, pose, sprite, r, g, b, a, light, 0, 0, 1,
                    x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1, u0 + x0 * du, v0 + (1 - y1) * dv, u0 + x1 * du, v0 + (1 - y0) * dv);
        }
        if ((skip & 16) == 0) {
            quad(vc, m, pose, sprite, r, g, b, a, light, -1, 0, 0,
                    x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1, u0 + z0 * du, v0 + (1 - y1) * dv, u0 + z1 * du, v0 + (1 - y0) * dv);
        }
        if ((skip & 32) == 0) {
            quad(vc, m, pose, sprite, r, g, b, a, light, 1, 0, 0,
                    x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0, u0 + z0 * du, v0 + (1 - y1) * dv, u0 + z1 * du, v0 + (1 - y0) * dv);
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m, PoseStack pose, TextureAtlasSprite sprite, int r, int g, int b, int a, int light,
                             float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz,
                             float ua, float va, float ub, float vb) {
        vertex(vc, m, pose, ax, ay, az, ua, va, r, g, b, a, light, nx, ny, nz);
        vertex(vc, m, pose, bx, by, bz, ua, vb, r, g, b, a, light, nx, ny, nz);
        vertex(vc, m, pose, cx, cy, cz, ub, vb, r, g, b, a, light, nx, ny, nz);
        vertex(vc, m, pose, dx, dy, dz, ub, va, r, g, b, a, light, nx, ny, nz);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, PoseStack pose, float x, float y, float z, float u, float v,
                               int r, int g, int b, int a, int light, float nx, float ny, float nz) {
        vc.addVertex(m, x, y, z).setColor(r, g, b, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(), nx, ny, nz);
    }
}
