package resonantinduction.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.minecraft.util.FastColor;
import resonantinduction.wire.WireBlockEntity;
import resonantinduction.wire.WireItem;
import resonantinduction.wire.WireMaterial;
import resonantinduction.ResonantInduction;
import resonantinduction.registry.RIRegistries;

@EventBusSubscriber(modid = ResonantInduction.MODID, value = Dist.CLIENT)
public final class RIClient {
    private RIClient() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(RIRegistries.QUANTUM_GATE_BE.get(), QuantumGateRenderer::new);
        event.registerBlockEntityRenderer(RIRegistries.CHARGER_BE.get(), ChargerRenderer::new);
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
