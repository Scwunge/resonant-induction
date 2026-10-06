package resonantinduction.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import resonantinduction.ResonantInduction;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A Techne (.tcn) model drawn the way the original drew it: each shape a box in ModelBase space with its own rotation, which JSON
 * block models cannot express. Read from the zipped model.xml when first drawn.
 */
public final class TechneModel {
    private final ResourceLocation location;
    private List<Shape> shapes;

    record Shape(String name, ModelPart part) {}

    public TechneModel(ResourceLocation location) {
        this.location = location;
    }

    private List<Shape> shapes() {
        if (shapes == null) {
            shapes = load();
        }
        return shapes;
    }

    private List<Shape> load() {
        List<Shape> out = new ArrayList<>();
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(location); ZipInputStream zip = new ZipInputStream(in)) {
            for (ZipEntry e; (e = zip.getNextEntry()) != null; ) {
                if (!e.getName().equals("model.xml")) {
                    continue;
                }
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new java.io.ByteArrayInputStream(zip.readAllBytes()));
                float[] size = floats(doc.getDocumentElement(), "TextureSize");
                NodeList list = doc.getElementsByTagName("Shape");
                for (int i = 0; i < list.getLength(); i++) {
                    Element s = (Element) list.item(i);
                    float[] offset = floats(s, "Offset");
                    float[] pos = floats(s, "Position");
                    float[] rot = floats(s, "Rotation");
                    float[] dim = floats(s, "Size");
                    float[] uv = floats(s, "TextureOffset");
                    boolean mirror = "True".equalsIgnoreCase(text(s, "IsMirrored"));
                    MeshDefinition mesh = new MeshDefinition();
                    mesh.getRoot().addOrReplaceChild("box", CubeListBuilder.create().texOffs((int) uv[0], (int) uv[1]).mirror(mirror)
                                    .addBox(offset[0], offset[1], offset[2], dim[0], dim[1], dim[2]),
                            PartPose.offsetAndRotation(pos[0], pos[1], pos[2], rot[0] * Mth.DEG_TO_RAD, rot[1] * Mth.DEG_TO_RAD, rot[2] * Mth.DEG_TO_RAD));
                    out.add(new Shape(s.getAttribute("name"), LayerDefinition.create(mesh, (int) size[0], (int) size[1]).bakeRoot()));
                }
            }
        } catch (Exception e) {
            ResonantInduction.LOGGER.error("Couldn't read model {}", location, e);
        }
        return out;
    }

    private static String text(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }

    private static float[] floats(Element parent, String tag) {
        String[] parts = text(parent, tag).split(",");
        float[] out = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            out[i] = Float.parseFloat(parts[i].trim());
        }
        return out;
    }

    /** Moves into the model's space for a block at the origin: ModelBase has y down and the block 8..24 high, turned about z. */
    public static void enterBlockSpace(PoseStack pose) {
        pose.translate(0.5, 1.5, 0.5);
        pose.scale(-1, -1, 1);
    }

    /** Draws the shapes whose names pass {@code filter}. */
    public void render(PoseStack pose, VertexConsumer buffer, int light, int overlay, Predicate<String> filter) {
        for (Shape shape : shapes()) {
            if (filter.test(shape.name())) {
                shape.part().render(pose, buffer, light, overlay);
            }
        }
    }
}
