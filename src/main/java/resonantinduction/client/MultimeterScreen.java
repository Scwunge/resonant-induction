package resonantinduction.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import resonantinduction.multimeter.Measure;
import resonantinduction.multimeter.MultimeterBlockEntity;
import resonantinduction.multimeter.MultimeterMenu;
import resonantinduction.network.MultimeterSettingsPayload;

/**
 * Multimeter settings, laid out like the original: the reading it watches and its value, the redstone logic, a limit, and which
 * reading the screen shows large; all current readings are listed on the right.
 */
public class MultimeterScreen extends AbstractContainerScreen<MultimeterMenu> {
    private EditBox limitBox;
    private int mode;
    private int detect;
    private int graph;
    private boolean loaded;

    public MultimeterScreen(MultimeterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 236;
        imageHeight = 150;
    }

    @Override
    protected void init() {
        super.init();
        MultimeterBlockEntity meter = menu.meter();
        // Read the block's settings once; after that the screen's own choices win (the server copy lags a moment).
        if (meter != null && !loaded) {
            loaded = true;
            mode = meter.detectMode().ordinal();
            detect = meter.detectType().ordinal();
            graph = meter.graphType().ordinal();
        }
        int x = leftPos + 8;
        int y = topPos + 20;
        addRenderableWidget(Button.builder(Component.empty(), b -> {
            detect = (detect + 1) % Measure.values().length;
            send();
        }).bounds(x, y, 110, 20).build()).setMessage(detectLabel());
        addRenderableWidget(Button.builder(Component.empty(), b -> {
            mode = (mode + 1) % MultimeterBlockEntity.DetectMode.values().length;
            send();
        }).bounds(x, y + 44, 110, 20).build()).setMessage(modeLabel());
        limitBox = new EditBox(font, x, y + 70, 110, 16, Component.translatable("gui.resonantinduction.multimeter.limit"));
        limitBox.setMaxLength(12);
        limitBox.setValue(meter != null ? trim(meter.limit()) : "0");
        limitBox.setResponder(s -> send());
        addRenderableWidget(limitBox);
        addRenderableWidget(Button.builder(Component.empty(), b -> {
            graph = (graph + 1) % Measure.values().length;
            send();
        }).bounds(x, y + 100, 110, 20).build()).setMessage(graphLabel());
    }

    private Component detectLabel() {
        return Component.translatable("gui.resonantinduction.multimeter.detect", Component.translatable(Measure.byIndex(detect).key()));
    }

    private Component modeLabel() {
        return Component.translatable("gui.resonantinduction.multimeter.logic",
                Component.translatable("gui.resonantinduction.multimeter.mode." + MultimeterBlockEntity.DetectMode.values()[mode].name().toLowerCase()));
    }

    private Component graphLabel() {
        return Component.translatable("gui.resonantinduction.multimeter.display", Component.translatable(Measure.byIndex(graph).key()));
    }

    private static String trim(double d) {
        return d == Math.rint(d) ? Long.toString((long) d) : Double.toString(d);
    }

    private void send() {
        double limit;
        try {
            limit = Double.parseDouble(limitBox == null ? "0" : limitBox.getValue());
        } catch (NumberFormatException e) {
            limit = 0;
        }
        PacketDistributor.sendToServer(new MultimeterSettingsPayload(menu.pos(), mode, detect, graph, limit));
        rebuildWidgets();
    }

    @Override
    protected void rebuildWidgets() {
        String value = limitBox != null ? limitBox.getValue() : null;
        boolean focused = limitBox != null && limitBox.isFocused();
        super.rebuildWidgets();
        if (value != null) {
            limitBox.setResponder(s -> {});
            limitBox.setValue(value);
            limitBox.setResponder(s -> send());
            limitBox.setFocused(focused);
            if (focused) {
                setFocused(limitBox);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF2B2B2B);
        g.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFFC6C6C6);
        g.fill(leftPos + 124, topPos + 18, leftPos + imageWidth - 6, topPos + imageHeight - 6, 0xFF1E2A1E);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, (imageWidth - font.width(title)) / 2, 6, 0x404040, false);
        MultimeterBlockEntity meter = menu.meter();
        if (meter == null) {
            return;
        }
        Measure d = Measure.byIndex(detect);
        g.drawString(font, Component.translatable("gui.resonantinduction.multimeter.value", MultimeterRenderer.format(meter.value(d), d)), 8, 46, 0x404040, false);
        g.drawString(font, Component.translatable(meter.redstoneOn() ? "gui.resonantinduction.multimeter.redstone_on" : "gui.resonantinduction.multimeter.redstone_off"),
                8, 108, meter.redstoneOn() ? 0xAA0000 : 0x404040, false);
        int y = 22;
        for (Measure m : Measure.values()) {
            g.drawString(font, Component.translatable(m.key()), 128, y, 0x7FFF7F, false);
            String v = MultimeterRenderer.format(meter.value(m), m);
            g.drawString(font, v, imageWidth - 10 - font.width(v), y, m == Measure.byIndex(graph) ? 0xFFFF55 : 0xCCFFCC, false);
            y += 15;
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        // Let the limit box take typing (including 'e', which would otherwise close the screen).
        if (limitBox != null && limitBox.isFocused() && key != 256) {
            return limitBox.keyPressed(key, scan, modifiers) || limitBox.canConsumeInput();
        }
        return super.keyPressed(key, scan, modifiers);
    }
}
