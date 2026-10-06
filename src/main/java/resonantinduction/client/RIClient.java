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
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import resonantinduction.resource.MaterialFluid;
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
        event.registerBlockEntityRenderer(RIRegistries.MECHANICAL_PISTON_BE.get(), ProcessRenderer::new);
        event.registerBlockEntityRenderer(RIRegistries.GRINDING_WHEEL_BE.get(), ProcessRenderer::new);
        event.registerBlockEntityRenderer(RIRegistries.MIXER_BE.get(), ProcessRenderer::new);
        event.registerBlockEntityRenderer(RIRegistries.HOT_PLATE_BE.get(), HotPlateRenderer::new);
        event.registerBlockEntityRenderer(RIRegistries.CASTING_MOLD_BE.get(), CastingMoldRenderer::new);
        event.registerBlockEntityRenderer(RIRegistries.GUTTER_BE.get(), FluidBlockRenderers.Gutter::new);
        event.registerBlockEntityRenderer(RIRegistries.TANK_BE.get(), FluidBlockRenderers.Tank::new);
        event.registerBlockEntityRenderer(RIRegistries.PIPE_BE.get(), FluidBlockRenderers.Pipe::new);
        event.registerBlockEntityRenderer(RIRegistries.PUMP_BE.get(), FluidBlockRenderers.Pump::new);
        event.registerBlockEntityRenderer(RIRegistries.CRATE_BE.get(), CrateRenderer::new);
        event.registerBlockEntityRenderer(RIRegistries.ENGINEERING_TABLE_BE.get(), TableRenderers.EngineeringTable::new);
        event.registerBlockEntityRenderer(RIRegistries.IMPRINTER_BE.get(), TableRenderers.Imprinter::new);
    }

    /** Molten metal and dust mixture in tanks and pipes, tinted to their metal. */
    @SubscribeEvent
    public static void clientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(materialFluid("molten_flow"), RIRegistries.MOLTEN_METAL_TYPE.get());
        event.registerFluidType(materialFluid("mixture_flow"), RIRegistries.DUST_MIXTURE_TYPE.get());
    }

    private static IClientFluidTypeExtensions materialFluid(String texture) {
        ResourceLocation tex = ResonantInduction.id("block/" + texture);
        return new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return tex;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return tex;
            }

            @Override
            public int getTintColor(FluidStack stack) {
                return FastColor.ARGB32.opaque(MaterialColors.get(MaterialFluid.material(stack)));
            }
        };
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
        event.register(ProcessRenderer.PISTON_ROTOR);
        event.register(ProcessRenderer.PISTON_SHAFT);
        event.register(ProcessRenderer.GRINDER_WHEEL);
        event.register(ProcessRenderer.GRINDER_TEETH);
        event.register(ProcessRenderer.MIXER_ROTOR);
        event.register(FluidBlockRenderers.PUMP_FIN);
        event.register(FluidBlockRenderers.PUMP_INNER_FIN);
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
    public static void materialBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> level != null && pos != null
                        && level.getBlockEntity(pos) instanceof resonantinduction.resource.MaterialBlockEntity be ? FastColor.ARGB32.opaque(MaterialColors.get(be.material())) : -1,
                RIRegistries.DUST_PILE.get(), RIRegistries.REFINED_DUST_PILE.get(), RIRegistries.MOLTEN_POOL.get(), RIRegistries.MIXTURE_POOL.get());
    }

    /** Pipes: tint 0 is the material, tint 1 (the tube) the dye if it has one. */
    @SubscribeEvent
    public static void pipeColors(RegisterColorHandlersEvent.Block event) {
        for (var e : RIRegistries.PIPES.entrySet()) {
            int material = e.getKey().color;
            event.register((state, level, pos, tint) -> {
                if (tint == 1 && level != null && pos != null && level.getBlockEntity(pos) instanceof resonantinduction.fluid.PipeBlockEntity pipe && pipe.color() != null) {
                    return FastColor.ARGB32.opaque(pipe.color().getTextureDiffuseColor());
                }
                return FastColor.ARGB32.opaque(material);
            }, e.getValue().get());
        }
    }

    @SubscribeEvent
    public static void reload(net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager -> MaterialColors.clear());
    }

    @SubscribeEvent
    public static void itemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> tint == 0 ? FastColor.ARGB32.opaque(MaterialColors.get(resonantinduction.resource.Materials.material(stack))) : -1,
                RIRegistries.RUBBLE.get(), RIRegistries.DUST.get(), RIRegistries.REFINED_DUST.get());
        event.register((stack, tint) -> tint == 1 ? FastColor.ARGB32.opaque(MaterialColors.get(resonantinduction.resource.Materials.material(stack))) : -1,
                RIRegistries.MOLTEN_BUCKET.get(), RIRegistries.MIXTURE_BUCKET.get());
        for (WireMaterial m : WireMaterial.values()) {
            event.register((stack, tint) -> tint == 0 ? FastColor.ARGB32.opaque(((WireItem) stack.getItem()).material().color) : -1,
                    RIRegistries.WIRE_ITEMS.get(m).get());
        }
        for (var e : RIRegistries.PIPES.entrySet()) {
            int material = e.getKey().color;
            event.register((stack, tint) -> FastColor.ARGB32.opaque(material), e.getValue().get());
        }
    }
}
