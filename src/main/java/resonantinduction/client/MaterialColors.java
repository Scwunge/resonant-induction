package resonantinduction.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import resonantinduction.resource.Materials;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Colour of a metal, from the average of its ingot's texture (ignoring dark pixels), as the original Resource Generator did.
 * Cached per metal; cleared on resource reload.
 */
public final class MaterialColors {
    private static final Map<String, Integer> CACHE = new HashMap<>();

    private MaterialColors() {}

    public static void clear() {
        CACHE.clear();
    }

    public static int get(String material) {
        if (material == null || material.isEmpty()) {
            return 0xFFFFFF;
        }
        return CACHE.computeIfAbsent(material, MaterialColors::compute);
    }

    private static int compute(String material) {
        return Materials.firstIngot(material).map(item -> average(new ItemStack(item))).orElse(0xFFFFFF);
    }

    private static int average(ItemStack stack) {
        try {
            TextureAtlasSprite sprite = Minecraft.getInstance().getItemRenderer().getModel(stack, null, null, 0).getParticleIcon();
            ResourceLocation name = sprite.contents().name();
            ResourceLocation file = ResourceLocation.fromNamespaceAndPath(name.getNamespace(), "textures/" + name.getPath() + ".png");
            Optional<net.minecraft.server.packs.resources.Resource> resource = Minecraft.getInstance().getResourceManager().getResource(file);
            if (resource.isEmpty()) {
                return 0xFFFFFF;
            }
            try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
                long r = 0, g = 0, b = 0, n = 0;
                int size = Math.min(image.getWidth(), image.getHeight());
                for (int x = 0; x < image.getWidth(); x++) {
                    for (int y = 0; y < size; y++) {
                        int abgr = image.getPixelRGBA(x, y);
                        int alpha = (abgr >>> 24) & 0xFF;
                        int red = abgr & 0xFF;
                        int green = (abgr >> 8) & 0xFF;
                        int blue = (abgr >> 16) & 0xFF;
                        double luma = 0.2126 * red + 0.7152 * green + 0.0722 * blue;
                        if (alpha > 0 && luma > 40) {
                            r += red;
                            g += green;
                            b += blue;
                            n++;
                        }
                    }
                }
                if (n == 0) {
                    return 0xFFFFFF;
                }
                return (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
            }
        } catch (Exception e) {
            return 0xFFFFFF;
        }
    }
}
