package resonantinduction.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import resonantinduction.ResonantInduction;
import resonantinduction.registry.RIRegistries;

@EventBusSubscriber(modid = ResonantInduction.MODID, value = Dist.CLIENT)
public final class RIClient {
    private RIClient() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(RIRegistries.QUANTUM_GATE_BE.get(), QuantumGateRenderer::new);
    }
}
