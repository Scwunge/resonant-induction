package resonantinduction.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import resonantinduction.atomic.machine.MachineLayout;
import resonantinduction.atomic.machine.MachineMenu;

import java.util.ArrayList;
import java.util.List;

/**
 * The atomic machines' screen, drawn from the machine's layout: a plain panel with its slots, fluid gauges (hover for the amount),
 * the work bar, the energy bar and a few lines about what it does.
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int DARK = 0xFF373737;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int SLOT = 0xFF8B8B8B;
    private static final int GAUGE_W = 18;
    private static final int GAUGE_H = 49;

    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = menu.machine().layout().height();
        this.inventoryLabelY = imageHeight - 94;
    }

    private static void bevel(GuiGraphics g, int x, int y, int w, int h, int topLeft, int bottomRight, int fill) {
        g.fill(x, y, x + w, y + h, fill);
        g.fill(x, y, x + w - 1, y + 1, topLeft);
        g.fill(x, y, x + 1, y + h - 1, topLeft);
        g.fill(x + 1, y + h - 1, x + w, y + h, bottomRight);
        g.fill(x + w - 1, y + 1, x + w, y + h, bottomRight);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        // Panel, with the usual rounded light/dark edge.
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, PANEL);
        g.fill(x + 2, y, x + imageWidth - 2, y + 1, 0xFF000000);
        g.fill(x + 2, y + imageHeight - 1, x + imageWidth - 2, y + imageHeight, 0xFF000000);
        g.fill(x, y + 2, x + 1, y + imageHeight - 2, 0xFF000000);
        g.fill(x + imageWidth - 1, y + 2, x + imageWidth, y + imageHeight - 2, 0xFF000000);
        g.fill(x + 2, y + 1, x + imageWidth - 3, y + 3, LIGHT);
        g.fill(x + 1, y + 2, x + 3, y + imageHeight - 3, LIGHT);
        g.fill(x + 3, y + imageHeight - 3, x + imageWidth - 2, y + imageHeight - 1, 0xFF555555);
        g.fill(x + imageWidth - 3, y + 3, x + imageWidth - 1, y + imageHeight - 2, 0xFF555555);

        MachineLayout layout = menu.machine().layout();
        for (MachineLayout.SlotAt s : layout.slots()) {
            bevel(g, x + s.x(), y + s.y(), 18, 18, DARK, LIGHT, SLOT);
            if (s.kind() == MachineLayout.Kind.BATTERY && !menu.getSlot(layout.slots().indexOf(s)).hasItem()) {
                // A little battery mark.
                g.fill(x + s.x() + 6, y + s.y() + 4, x + s.x() + 12, y + s.y() + 15, 0xFF6B6B6B);
                g.fill(x + s.x() + 8, y + s.y() + 3, x + s.x() + 10, y + s.y() + 4, 0xFF6B6B6B);
            } else if (s.kind() == MachineLayout.Kind.FLUID && !menu.getSlot(layout.slots().indexOf(s)).hasItem()) {
                g.fill(x + s.x() + 7, y + s.y() + 5, x + s.x() + 11, y + s.y() + 13, 0xFF5A7FB8);
            }
        }
        for (int p = 0; p < 36; p++) {
            int col = p % 9;
            int row = p / 9;
            int sy = row < 3 ? layout.height() - 82 + row * 18 : layout.height() - 24;
            bevel(g, x + 7 + col * 18, y + sy - 1, 18, 18, DARK, LIGHT, SLOT);
        }
        List<FluidTank> tanks = menu.machine().tanks();
        for (MachineLayout.GaugeAt gauge : layout.gauges()) {
            int gx = x + gauge.x();
            int gy = y + gauge.y();
            bevel(g, gx, gy, GAUGE_W, GAUGE_H, DARK, LIGHT, 0xFF2A2A2A);
            FluidTank tank = tanks.get(gauge.tank());
            if (!tank.isEmpty()) {
                int h = (int) Math.ceil((GAUGE_H - 2) * (double) tank.getFluidAmount() / tank.getCapacity());
                drawFluid(g, tank.getFluid(), gx + 1, gy + GAUGE_H - 1 - h, GAUGE_W - 2, h);
            }
            for (int tick = 1; tick < 5; tick++) {
                int ty = gy + tick * GAUGE_H / 5;
                g.fill(gx + 1, ty, gx + 6, ty + 1, 0xAA000000);
            }
        }
        // Work bar: an arrow-length bar filling left to right.
        int bx = x + layout.barX();
        int by = y + layout.barY();
        bevel(g, bx, by, 24, 16, DARK, LIGHT, 0xFF8B8B8B);
        if (menu.jobTime() > 0 && menu.timer() > 0) {
            int w = (int) (22 * (1 - (double) menu.timer() / menu.jobTime()));
            g.fill(bx + 1, by + 1, bx + 1 + w, by + 15, 0xFF4FB84F);
        }
        // Energy bar along the bottom of the machine area.
        int ex = x + layout.energyX();
        int ey = y + layout.energyY();
        bevel(g, ex, ey, 160, 8, DARK, LIGHT, 0xFF2A2A2A);
        if (menu.capacity() > 0) {
            int w = (int) (158 * Math.min(1, (double) menu.energy() / menu.capacity()));
            g.fill(ex + 1, ey + 1, ex + 1 + w, ey + 7, 0xFFD8B33A);
        }
    }

    private void drawFluid(GuiGraphics g, FluidStack fluid, int x, int y, int w, int h) {
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fluid));
        int tint = ext.getTintColor(fluid);
        float r = ((tint >> 16) & 0xFF) / 255f;
        float gr = ((tint >> 8) & 0xFF) / 255f;
        float b = (tint & 0xFF) / 255f;
        for (int yy = 0; yy < h; yy += 16) {
            int part = Math.min(16, h - yy);
            g.blit(x, y + h - yy - part, 0, w, part, sprite, r, gr, b, 1f);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, (imageWidth - font.width(title)) / 2, 6, 0x404040, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
        MachineLayout layout = menu.machine().layout();
        for (MachineLayout.Line line : layout.lines()) {
            g.drawString(font, line.text(), line.x(), line.y(), 0x404040, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        MachineLayout layout = menu.machine().layout();
        List<FluidTank> tanks = menu.machine().tanks();
        for (MachineLayout.GaugeAt gauge : layout.gauges()) {
            if (isHovering(gauge.x(), gauge.y(), GAUGE_W, GAUGE_H, mouseX, mouseY)) {
                FluidTank tank = tanks.get(gauge.tank());
                List<Component> tip = new ArrayList<>();
                tip.add(tank.isEmpty() ? Component.translatable("tooltip.resonantinduction.empty") : tank.getFluid().getHoverName());
                tip.add(Component.translatable("tooltip.resonantinduction.fluid_amount", tank.getFluidAmount(), tank.getCapacity()));
                g.renderComponentTooltip(font, tip, mouseX, mouseY);
            }
        }
        if (isHovering(layout.energyX(), layout.energyY(), 160, 8, mouseX, mouseY)) {
            g.renderComponentTooltip(font, List.of(Component.translatable("tooltip.resonantinduction.energy", menu.energy(), menu.capacity()),
                    Component.translatable("tooltip.resonantinduction.machine_use", menu.machine().usePerTick())), mouseX, mouseY);
        }
        renderTooltip(g, mouseX, mouseY);
    }
}
