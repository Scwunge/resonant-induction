package resonantinduction.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import resonantinduction.network.BuildSchematicPayload;
import resonantinduction.schematic.CreativeBuilderBlock;
import resonantinduction.schematic.Schematics;

import java.util.List;

/** The Creative Builder's screen: pick a schematic and a size, then build it. */
public class CreativeBuilderScreen extends Screen {
    private final BlockPos pos;
    private final List<Schematics.Schematic> choices;
    private int choice;
    private int size = 1;
    private Button schematicButton;

    public CreativeBuilderScreen(BlockPos pos) {
        super(Component.translatable("block.resonantinduction.creative_builder"));
        this.pos = pos;
        this.choices = Schematics.ALL.stream().filter(Schematics.Schematic::available).toList();
    }

    public static void open(BlockPos pos) {
        Minecraft.getInstance().setScreen(new CreativeBuilderScreen(pos));
    }

    @Override
    protected void init() {
        int x = width / 2;
        int y = height / 2;
        schematicButton = addRenderableWidget(Button.builder(schematicName(), b -> {
            choice = (choice + 1) % choices.size();
            b.setMessage(schematicName());
        }).bounds(x - 80, y - 40, 160, 20).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> size = Math.max(1, size - 1)).bounds(x - 80, y - 12, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> size = Math.min(CreativeBuilderBlock.MAX_SIZE, size + 1)).bounds(x + 60, y - 12, 20, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.resonantinduction.creative_builder.build"), b -> {
            if (!choices.isEmpty()) {
                PacketDistributor.sendToServer(new BuildSchematicPayload(pos, Schematics.ALL.indexOf(choices.get(choice)), size));
            }
            onClose();
        }).bounds(x - 80, y + 16, 78, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose()).bounds(x + 2, y + 16, 78, 20).build());
        schematicButton.active = !choices.isEmpty();
    }

    private Component schematicName() {
        return choices.isEmpty() ? Component.translatable("gui.resonantinduction.creative_builder.none") : Component.translatable(choices.get(choice).key());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, height / 2 - 60, 0xFFFFFF);
        g.drawCenteredString(font, Component.translatable("gui.resonantinduction.creative_builder.size", size), width / 2, height / 2 - 6, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
