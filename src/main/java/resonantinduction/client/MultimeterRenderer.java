package resonantinduction.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import resonantinduction.multimeter.Measure;
import resonantinduction.multimeter.MultimeterBlockEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Writes the readings on the screen. Multimeters side by side on one face form a bigger screen: if they make a full rectangle,
 * its lowest corner draws the combined readings across the whole thing, as the original did.
 */
public class MultimeterRenderer implements BlockEntityRenderer<MultimeterBlockEntity> {
    private final Font font;

    public MultimeterRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    public static String format(double value, Measure measure) {
        double abs = Math.abs(value);
        String number;
        if (abs >= 1e9) {
            number = String.format("%.2fG", value / 1e9);
        } else if (abs >= 1e6) {
            number = String.format("%.2fM", value / 1e6);
        } else if (abs >= 1e4) {
            number = String.format("%.1fk", value / 1e3);
        } else if (value == Math.rint(value)) {
            number = Long.toString((long) value);
        } else {
            number = String.format("%.2f", value);
        }
        return number + " " + measure.unit;
    }

    /** Multimeters joined to this one on the same face, or null if they don't form a full rectangle. */
    static List<MultimeterBlockEntity> group(MultimeterBlockEntity start) {
        Level level = start.getLevel();
        Direction facing = start.facing();
        List<MultimeterBlockEntity> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        open.add(start.getBlockPos());
        while (!open.isEmpty() && out.size() < 256) {
            BlockPos pos = open.poll();
            if (!seen.add(pos) || !(level.getBlockEntity(pos) instanceof MultimeterBlockEntity m) || m.facing() != facing) {
                continue;
            }
            out.add(m);
            for (Direction d : Direction.values()) {
                if (d.getAxis() != facing.getAxis()) {
                    open.add(pos.relative(d));
                }
            }
        }
        return out;
    }

    @Override
    public void render(MultimeterBlockEntity meter, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        List<MultimeterBlockEntity> group = group(meter);
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (MultimeterBlockEntity m : group) {
            BlockPos p = m.getBlockPos();
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
            maxZ = Math.max(maxZ, p.getZ());
        }
        int area = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        BlockPos own = meter.getBlockPos();
        if (area != group.size() || own.getX() != minX || own.getY() != minY || own.getZ() != minZ) {
            return;
        }

        List<Component> lines = new ArrayList<>();
        for (Measure m : Measure.values()) {
            double total = 0;
            for (MultimeterBlockEntity g : group) {
                total += g.value(m);
            }
            if (total != 0 || m == meter.graphType()) {
                lines.add(Component.translatable(m.key()).append(": " + format(total, m)));
            }
        }

        Direction facing = meter.facing();
        double width = facing.getAxis() == Direction.Axis.X ? maxZ - minZ + 1 : maxX - minX + 1;
        double height = facing.getAxis() == Direction.Axis.Y ? maxZ - minZ + 1 : maxY - minY + 1;

        pose.pushPose();
        // Centre of the whole screen, just in front of its face.
        pose.translate((minX + maxX) / 2.0 - own.getX() + 0.5, (minY + maxY) / 2.0 - own.getY() + 0.5, (minZ + maxZ) / 2.0 - own.getZ() + 0.5);
        pose.mulPose(facing.getRotation());
        pose.translate(0, -0.5 + 2.2 / 16, 0);
        pose.mulPose(Axis.XP.rotationDegrees(-90));
        int textWidth = 1;
        for (Component line : lines) {
            textWidth = Math.max(textWidth, font.width(line));
        }
        float scale = (float) Math.min((width * 0.85) / textWidth, (height * 0.85) / (lines.size() * 10.0));
        pose.scale(scale, -scale, scale);
        float y = -lines.size() * 10 / 2f;
        for (Component line : lines) {
            font.drawInBatch(line, -font.width(line) / 2f, y, 0xFF7FFF7F, false, pose.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);
            y += 10;
        }
        pose.popPose();
    }
}
