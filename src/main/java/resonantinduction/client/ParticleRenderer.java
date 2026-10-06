package resonantinduction.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import resonantinduction.atomic.particle.ParticleEntity;

/** An accelerated particle: a small burst of light rays that grows denser and fades as it flies, as the original. */
public class ParticleRenderer extends EntityRenderer<ParticleEntity> {
    private static final float HALF_SQRT_3 = (float) (Math.sqrt(3) / 2);

    public ParticleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ParticleEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partialTick;
        while (age > 200) {
            age -= 100;
        }
        float growth = (5 + age) / 200f;
        float fade = growth > 0.8f ? (growth - 0.8f) / 0.2f : 0;
        pose.pushPose();
        pose.scale(0.15f, 0.15f, 0.15f);
        pose.translate(0, -1, -2);
        rays(pose, buffers.getBuffer(RenderType.dragonRays()), growth, fade);
        rays(pose, buffers.getBuffer(RenderType.dragonRaysDepth()), growth, fade);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    private static void rays(PoseStack pose, VertexConsumer buffer, float growth, float fade) {
        pose.pushPose();
        int centre = FastColor.ARGB32.colorFromFloat(1 - fade, 1, 1, 1);
        RandomSource random = RandomSource.create(432L);
        Vector3f origin = new Vector3f();
        Vector3f a = new Vector3f();
        Vector3f b = new Vector3f();
        Vector3f c = new Vector3f();
        Quaternionf turn = new Quaternionf();
        int count = Mth.floor((growth + growth * growth) / 2 * 60);
        for (int i = 0; i < count; i++) {
            turn.rotationXYZ(random.nextFloat() * Mth.TWO_PI, random.nextFloat() * Mth.TWO_PI, random.nextFloat() * Mth.TWO_PI)
                    .rotateXYZ(random.nextFloat() * Mth.TWO_PI, random.nextFloat() * Mth.TWO_PI, random.nextFloat() * Mth.TWO_PI + growth * Mth.HALF_PI);
            pose.mulPose(turn);
            float length = random.nextFloat() * 20 + 5 + fade * 10;
            float width = random.nextFloat() * 2 + 1 + fade * 2;
            a.set(-HALF_SQRT_3 * width, length, -0.5f * width);
            b.set(HALF_SQRT_3 * width, length, -0.5f * width);
            c.set(0, length, width);
            PoseStack.Pose last = pose.last();
            buffer.addVertex(last, origin).setColor(centre);
            buffer.addVertex(last, a).setColor(0);
            buffer.addVertex(last, b).setColor(0);
            buffer.addVertex(last, origin).setColor(centre);
            buffer.addVertex(last, b).setColor(0);
            buffer.addVertex(last, c).setColor(0);
            buffer.addVertex(last, origin).setColor(centre);
            buffer.addVertex(last, c).setColor(0);
            buffer.addVertex(last, a).setColor(0);
        }
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(ParticleEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
