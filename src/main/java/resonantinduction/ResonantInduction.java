package resonantinduction;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import resonantinduction.atomic.particle.ParticleEntity;
import resonantinduction.network.RINetwork;
import resonantinduction.quantum.QuantumTeleports;
import resonantinduction.registry.RIRegistries;
import resonantinduction.tesla.TeslaGrid;

@Mod(ResonantInduction.MODID)
public final class ResonantInduction {
    public static final String MODID = "resonantinduction";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ResonantInduction(IEventBus modBus, ModContainer container) {
        RIRegistries.register(modBus);
        modBus.addListener(RIRegistries::registerCapabilities);
        modBus.addListener(RINetwork::register);
        container.registerConfig(ModConfig.Type.SERVER, RIConfig.SPEC);
        container.registerConfig(ModConfig.Type.COMMON, RIFeatures.SPEC);
        NeoForge.EVENT_BUS.addListener(TeslaGrid::onServerStopped);
        NeoForge.EVENT_BUS.addListener(QuantumTeleports::onServerTick);
        NeoForge.EVENT_BUS.addListener(QuantumTeleports::onServerStopped);
        NeoForge.EVENT_BUS.addListener(resonantinduction.atomic.ThermalGrid::onLevelTick);
        modBus.addListener((RegisterTicketControllersEvent e) -> e.register(ParticleEntity.TICKETS));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
