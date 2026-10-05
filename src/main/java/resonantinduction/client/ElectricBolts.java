package resonantinduction.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import resonantinduction.ResonantInduction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Short-lived lightning arcs. Each arc is a jagged line made by repeatedly splitting segments and nudging the new points
 * sideways, with a few forks, as the original FXElectricBolt did; drawn as camera-facing additive ribbons.
 */
@EventBusSubscriber(modid = ResonantInduction.MODID, value = Dist.CLIENT)
public final class ElectricBolts {
    private static final int MAX_BOLTS = 256;
    private static final List<Bolt> BOLTS = new ArrayList<>();
    private static final RandomSource RANDOM = RandomSource.create();

    private ElectricBolts() {}

    public static void add(Vec3 from, Vec3 to, int argb) {
        if (BOLTS.size() >= MAX_BOLTS) {
            BOLTS.remove(0);
        }
        BOLTS.add(new Bolt(from, to, argb, RANDOM));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().isPaused()) {
            return;
        }
        Iterator<Bolt> it = BOLTS.iterator();
        while (it.hasNext()) {
            if (--it.next().life <= 0) {
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || BOLTS.isEmpty()) {
            return;
        }
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f matrix = pose.last().pose();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());
        for (Bolt bolt : BOLTS) {
            bolt.render(consumer, matrix, cam);
        }
        buffers.endBatch(RenderType.lightning());
        pose.popPose();
    }

    private static final class Bolt {
        private final List<Segment> segments = new ArrayList<>();
        private final float r;
        private final float g;
        private final float b;
        private int life;

        Bolt(Vec3 from, Vec3 to, int argb, RandomSource random) {
            r = ((argb >> 16) & 0xFF) / 255f;
            g = ((argb >> 8) & 0xFF) / 255f;
            b = (argb & 0xFF) / 255f;
            life = 2 + random.nextInt(3);
            double length = from.distanceTo(to);
            build(from, to, length * 0.2, 1f, 0, random);
        }

        /** Midpoint displacement; forks get shorter and fainter. */
        private void build(Vec3 from, Vec3 to, double offset, float alpha, int depth, RandomSource random) {
            List<Vec3> points = new ArrayList<>();
            points.add(from);
            points.add(to);
            double length = from.distanceTo(to);
            int passes = Math.max(2, Math.min(7, (int) Math.ceil(Math.log(Math.max(length, 1) * 4) / Math.log(2))));
            double o = offset;
            for (int pass = 0; pass < passes; pass++) {
                List<Vec3> next = new ArrayList<>(points.size() * 2);
                for (int i = 0; i < points.size() - 1; i++) {
                    Vec3 a = points.get(i);
                    Vec3 c = points.get(i + 1);
                    Vec3 mid = a.add(c).scale(0.5).add(perpendicular(c.subtract(a), random).scale((random.nextDouble() - 0.5) * 2 * o));
                    next.add(a);
                    next.add(mid);
                    // Forks: short side branches (a fraction of the bolt) heading roughly the same way, fainter each level.
                    if (depth < 2 && pass >= 1 && pass < 4 && random.nextFloat() < 0.3f) {
                        Vec3 dir = c.subtract(a);
                        double forkLength = Math.min(length * 0.25, dir.length() * (0.6 + random.nextDouble() * 0.6));
                        Vec3 forkEnd = mid.add(dir.normalize().add(perpendicular(dir, random).scale(0.7)).normalize().scale(forkLength));
                        build(mid, forkEnd, o * 0.5, alpha * 0.5f, depth + 1, random);
                    }
                }
                next.add(points.get(points.size() - 1));
                points = next;
                o *= 0.5;
            }
            for (int i = 0; i < points.size() - 1; i++) {
                segments.add(new Segment(points.get(i), points.get(i + 1), alpha));
            }
        }

        private static Vec3 perpendicular(Vec3 dir, RandomSource random) {
            Vec3 axis = Math.abs(dir.y) < 0.9 * dir.length() ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
            Vec3 p = dir.cross(axis).normalize();
            Vec3 q = dir.cross(p).normalize();
            double angle = random.nextDouble() * Math.PI * 2;
            return p.scale(Math.cos(angle)).add(q.scale(Math.sin(angle)));
        }

        void render(VertexConsumer consumer, Matrix4f matrix, Vec3 cam) {
            for (Segment s : segments) {
                ribbon(consumer, matrix, cam, s, 0.09f, r, g, b, 0.35f * s.alpha);
                ribbon(consumer, matrix, cam, s, 0.03f, 1f, 1f, 1f, 0.8f * s.alpha);
            }
        }

        private static void ribbon(VertexConsumer consumer, Matrix4f m, Vec3 cam, Segment s, float width, float r, float g, float b, float a) {
            Vec3 dir = s.to.subtract(s.from);
            Vec3 side = dir.cross(cam.subtract(s.from)).normalize().scale(width);
            if (side.lengthSqr() < 1.0E-8) {
                return;
            }
            Vec3 p1 = s.from.add(side);
            Vec3 p2 = s.from.subtract(side);
            Vec3 p3 = s.to.subtract(side);
            Vec3 p4 = s.to.add(side);
            // Both windings, so the ribbon shows whichever way culling faces it.
            quad(consumer, m, p1, p2, p3, p4, r, g, b, a);
            quad(consumer, m, p4, p3, p2, p1, r, g, b, a);
        }

        private static void quad(VertexConsumer c, Matrix4f m, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4, float r, float g, float b, float a) {
            c.addVertex(m, (float) p1.x, (float) p1.y, (float) p1.z).setColor(r, g, b, a);
            c.addVertex(m, (float) p2.x, (float) p2.y, (float) p2.z).setColor(r, g, b, a);
            c.addVertex(m, (float) p3.x, (float) p3.y, (float) p3.z).setColor(r, g, b, a);
            c.addVertex(m, (float) p4.x, (float) p4.y, (float) p4.z).setColor(r, g, b, a);
        }
    }

    private record Segment(Vec3 from, Vec3 to, float alpha) {}
}
