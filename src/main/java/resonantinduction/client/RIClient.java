package resonantinduction.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.renderer.item.ItemProperties;
import resonantinduction.ResonantInduction;
import resonantinduction.battery.BatteryItem;
import net.minecraft.util.FastColor;
import resonantinduction.wire.WireBlockEntity;
import resonantinduction.wire.WireItem;
import resonantinduction.wire.WireMaterial;
import resonantinduction.registry.RIRegistries;

@EventBusSubscriber(modid = ResonantInduction.MODID, value = Dist.CLIENT)
public final class RIClient {
    private RIClient() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(RIRegistries.QUANTUM_GATE_BE.get(), QuantumGateRenderer::new);
        event.registerBlockEntityRenderer(RIRegistries.CHARGER_BE.get(), ChargerRenderer::new);
        event.registerBlockEntityRenderer(RIRegistries.BATTERY_BE.get(), BatteryRenderer::new);
        event.registerBlockEntityRenderer(RIRegistries.MULTIMETER_BE.get(), MultimeterRenderer::new);
        event.registerBlockEntityRenderer(RIRegistries.GEAR_BE.get(), MechanicalRenderer::new);
        event.registerBlockEntityRenderer(RIRegistries.SHAFT_BE.get(), MechanicalRenderer::new);
        event.registerBlockEntityRenderer(RIRegistries.TURBINE_BE.get(), TurbineRenderer::new);
    }

    @SubscribeEvent
    public static void menuScreens(RegisterMenuScreensEvent event) {
        event.register(RIRegistries.MULTIMETER_MENU.get(), MultimeterScreen::new);
    }

    @SubscribeEvent
    public static void additionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BatteryRenderer.CONNECTOR_IN);
        event.register(BatteryRenderer.CONNECTOR_OUT);
        TurbineRenderer.allParts().forEach(event::register);
        for (String tier : MechanicalRenderer.TIERS) {
            event.register(MechanicalRenderer.model("gear_small_" + tier));
            event.register(MechanicalRenderer.model("gear_large_" + tier));
            if (!tier.equals("creative")) {
                event.register(MechanicalRenderer.model("shaft_" + tier));
            }
        }
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        // Battery items light up coils by charge (0-8), like the original item renderer.
        event.enqueueWork(() -> ItemProperties.register(RIRegistries.BATTERY_ITEM.get(), ResonantInduction.id("level"),
                (stack, level, entity, seed) -> Math.round(8f * stack.getOrDefault(RIRegistries.ENERGY.get(), 0) / Math.max(1, BatteryItem.capacity(stack))) / 8f));
    }

    /** Tint 0: the wire metal. Tint 1: the insulation's dye colour. */
    @SubscribeEvent
    public static void blockColors(RegisterColorHandlersEvent.Block event) {
        for (WireMaterial m : WireMaterial.values()) {
            event.register((state, level, pos, tint) -> {
                if (tint == 1 && level != null && pos != null && level.getBlockEntity(pos) instanceof WireBlockEntity wire) {
                    return FastColor.ARGB32.opaque(wire.color().getTextureDiffuseColor());
                }
                return FastColor.ARGB32.opaque(m.color);
            }, RIRegistries.FLAT_WIRES.get(m).get(), RIRegistries.FRAMED_WIRES.get(m).get());
        }
    }

    @SubscribeEvent
    public static void itemColors(RegisterColorHandlersEvent.Item event) {
        for (WireMaterial m : WireMaterial.values()) {
            event.register((stack, tint) -> tint == 0 ? FastColor.ARGB32.opaque(((WireItem) stack.getItem()).material().color) : -1,
                    RIRegistries.WIRE_ITEMS.get(m).get());
        }
    }
}
