package resonantinduction;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import resonantinduction.network.RINetwork;
import resonantinduction.quantum.QuantumTeleports;
import resonantinduction.registry.RIRegistries;
import resonantinduction.tesla.TeslaGrid;

@Mod(ResonantInduction.MODID)
public final class ResonantInduction {
    public static final String MODID = "resonantinduction";

    public ResonantInduction(IEventBus modBus, ModContainer container) {
        RIRegistries.register(modBus);
        modBus.addListener(RIRegistries::registerCapabilities);
        modBus.addListener(RINetwork::register);
        container.registerConfig(ModConfig.Type.SERVER, RIConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(TeslaGrid::onServerStopped);
        NeoForge.EVENT_BUS.addListener(QuantumTeleports::onServerTick);
        NeoForge.EVENT_BUS.addListener(QuantumTeleports::onServerStopped);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
