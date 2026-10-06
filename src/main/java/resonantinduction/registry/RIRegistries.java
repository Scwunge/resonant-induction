package resonantinduction.registry;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import resonantinduction.RIFeatures;
import resonantinduction.ResonantInduction;
import resonantinduction.charger.ChargerBlock;
import resonantinduction.charger.ChargerBlockEntity;
import resonantinduction.laser.MiningLaserItem;
import resonantinduction.battery.BatteryBlock;
import resonantinduction.battery.BatteryBlockEntity;
import resonantinduction.battery.BatteryItem;
import resonantinduction.mechanical.gear.GearBlock;
import resonantinduction.mechanical.process.GrindingWheelBlockEntity;
import resonantinduction.mechanical.process.MachineBlock;
import resonantinduction.mechanical.process.MechanicalPistonBlockEntity;
import resonantinduction.mechanical.process.MixerBlockEntity;
import resonantinduction.archaic.CastingMoldBlock;
import resonantinduction.atomic.fusion.ElectromagnetBlock;
import resonantinduction.atomic.fusion.PlasmaBlock;
import resonantinduction.schematic.CreativeBuilderBlock;
import resonantinduction.atomic.fusion.PlasmaHeaterBlock;
import resonantinduction.atomic.fusion.PlasmaHeaterBlockEntity;
import resonantinduction.atomic.particle.AcceleratorBlock;
import resonantinduction.atomic.particle.AcceleratorBlockEntity;
import resonantinduction.atomic.particle.FulminationBlock;
import resonantinduction.atomic.particle.ParticleEntity;
import resonantinduction.atomic.particle.QuantumAssemblerBlockEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import resonantinduction.atomic.reactor.ControlRodBlock;
import resonantinduction.atomic.reactor.ElectricTurbineBlock;
import resonantinduction.atomic.reactor.ElectricTurbineBlockEntity;
import resonantinduction.atomic.reactor.FunnelBlock;
import resonantinduction.atomic.reactor.ReactorCellBlock;
import resonantinduction.atomic.reactor.ReactorCellBlockEntity;
import resonantinduction.atomic.reactor.SirenBlock;
import resonantinduction.atomic.reactor.ThermometerBlock;
import resonantinduction.atomic.reactor.ThermometerItem;
import resonantinduction.atomic.CellFluidHandler;
import resonantinduction.atomic.machine.AtomicMachineBlock;
import resonantinduction.atomic.machine.AtomicMachineBlockEntity;
import resonantinduction.atomic.machine.CentrifugeBlockEntity;
import resonantinduction.atomic.machine.ChemicalExtractorBlockEntity;
import resonantinduction.atomic.machine.MachineMenu;
import resonantinduction.atomic.machine.NuclearBoilerBlockEntity;
import resonantinduction.atomic.AntimatterItem;
import resonantinduction.atomic.FuelRodItem;
import resonantinduction.atomic.HazmatArmorItem;
import resonantinduction.atomic.Radiation;
import resonantinduction.atomic.RadioactiveBlock;
import resonantinduction.atomic.RadioactiveItem;
import resonantinduction.atomic.ToxicWasteBlock;
import resonantinduction.fluid.VirtualFluid;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import resonantinduction.logistic.BreakerBlock;
import resonantinduction.logistic.ConveyorBeltBlock;
import resonantinduction.logistic.DetectorBlock;
import resonantinduction.logistic.DetectorBlockEntity;
import resonantinduction.logistic.ManipulatorBlock;
import resonantinduction.logistic.ManipulatorBlockEntity;
import resonantinduction.logistic.PlacerBlock;
import resonantinduction.logistic.PlacerBlockEntity;
import resonantinduction.logistic.SorterBlock;
import resonantinduction.logistic.SorterBlockEntity;
import resonantinduction.archaic.CrateBlock;
import resonantinduction.archaic.CrateBlockEntity;
import resonantinduction.archaic.CrateContents;
import resonantinduction.archaic.CrateItem;
import resonantinduction.archaic.EngineeringTableBlock;
import resonantinduction.archaic.EngineeringTableBlockEntity;
import resonantinduction.archaic.HammerItem;
import resonantinduction.archaic.ImprinterBlock;
import resonantinduction.archaic.ImprinterBlockEntity;
import resonantinduction.archaic.TurntableBlock;
import resonantinduction.fluid.FluidNodeBlockEntity;
import resonantinduction.fluid.GrateBlock;
import resonantinduction.fluid.GrateBlockEntity;
import resonantinduction.fluid.GutterBlock;
import resonantinduction.fluid.GutterBlockEntity;
import resonantinduction.fluid.PipeBlock;
import resonantinduction.fluid.PipeBlockEntity;
import resonantinduction.fluid.PipeMaterial;
import resonantinduction.fluid.PumpBlockEntity;
import resonantinduction.fluid.TankBlock;
import resonantinduction.fluid.TankBlockEntity;
import resonantinduction.fluid.TankItem;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import resonantinduction.archaic.CastingMoldBlockEntity;
import resonantinduction.archaic.FilterBlock;
import resonantinduction.archaic.FireboxBlock;
import resonantinduction.archaic.FireboxBlockEntity;
import resonantinduction.archaic.HotPlateBlock;
import resonantinduction.archaic.HotPlateBlockEntity;
import resonantinduction.resource.MaterialFluid;
import resonantinduction.resource.MaterialFluidType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import resonantinduction.archaic.ImprintItem;
import resonantinduction.archaic.ImprintableBlockEntity;
import resonantinduction.archaic.MillstoneBlock;
import resonantinduction.resource.DustPileBlock;
import resonantinduction.resource.DustSmeltingRecipe;
import resonantinduction.resource.MaterialBlockEntity;
import resonantinduction.resource.OreResourceItem;
import resonantinduction.resource.PoolBlock;
import resonantinduction.resource.PoolBucketItem;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.RenderShape;
import resonantinduction.mechanical.motor.MotorBlock;
import resonantinduction.mechanical.motor.MotorBlockEntity;
import resonantinduction.mechanical.turbine.TurbineBlock;
import resonantinduction.mechanical.turbine.TurbineBlockEntity;
import resonantinduction.mechanical.gear.GearBlockEntity;
import resonantinduction.mechanical.gear.HandCrankItem;
import resonantinduction.mechanical.shaft.ShaftBlock;
import resonantinduction.mechanical.shaft.ShaftBlockEntity;
import resonantinduction.transformer.TransformerBlock;
import resonantinduction.transformer.TransformerBlockEntity;
import resonantinduction.multimeter.MultimeterBlock;
import resonantinduction.multimeter.MultimeterBlockEntity;
import resonantinduction.multimeter.MultimeterMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import resonantinduction.generator.GeneratorBlock;
import resonantinduction.generator.GeneratorBlockEntity;
import resonantinduction.generator.SolarPanelBlockEntity;
import resonantinduction.generator.ThermopileBlockEntity;
import resonantinduction.wire.FlatWireBlock;
import resonantinduction.wire.FramedWireBlock;
import resonantinduction.wire.WireBlock;
import resonantinduction.wire.WireBlockEntity;
import resonantinduction.wire.WireItem;
import resonantinduction.wire.WireMaterial;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.energy.ComponentEnergyStorage;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import resonantinduction.item.QuantumEntanglerItem;
import resonantinduction.levitator.LevitatorBlock;
import resonantinduction.levitator.LevitatorBlockEntity;
import resonantinduction.quantum.QuantumGateBlock;
import resonantinduction.quantum.QuantumGateBlockEntity;
import resonantinduction.quantum.QuantumGlyphItem;

import java.util.List;
import resonantinduction.tesla.TeslaBlock;
import resonantinduction.tesla.TeslaBlockEntity;

public final class RIRegistries {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ResonantInduction.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ResonantInduction.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ResonantInduction.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ResonantInduction.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ResonantInduction.MODID);
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS = DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, ResonantInduction.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, ResonantInduction.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ResonantInduction.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ResonantInduction.MODID);
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, ResonantInduction.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, ResonantInduction.MODID);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, ResonantInduction.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, ResonantInduction.MODID);
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, ResonantInduction.MODID);

    public static final ResourceKey<DamageType> ELECTROCUTION = ResourceKey.create(Registries.DAMAGE_TYPE, ResonantInduction.id("electrocution"));

    public static final DeferredBlock<TeslaBlock> TESLA = BLOCKS.registerBlock("tesla", TeslaBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5f, 6f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().noOcclusion());
    public static final DeferredItem<BlockItem> TESLA_ITEM = ITEMS.registerSimpleBlockItem(TESLA);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TeslaBlockEntity>> TESLA_BE = BLOCK_ENTITIES.register("tesla",
            () -> BlockEntityType.Builder.of(TeslaBlockEntity::new, TESLA.get()).build(null));

    public static final DeferredBlock<LevitatorBlock> LEVITATOR = BLOCKS.registerBlock("levitator", LevitatorBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2f, 6f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredItem<BlockItem> LEVITATOR_ITEM = ITEMS.registerSimpleBlockItem(LEVITATOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LevitatorBlockEntity>> LEVITATOR_BE = BLOCK_ENTITIES.register("levitator",
            () -> BlockEntityType.Builder.of(LevitatorBlockEntity::new, LEVITATOR.get()).build(null));

    public static final DeferredBlock<QuantumGateBlock> QUANTUM_GATE = BLOCKS.registerBlock("quantum_gate", QuantumGateBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(3f, 1200f).sound(SoundType.AMETHYST)
                    .noOcclusion().dynamicShape().lightLevel(s -> 4));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QuantumGateBlockEntity>> QUANTUM_GATE_BE = BLOCK_ENTITIES.register("quantum_gate",
            () -> BlockEntityType.Builder.of(QuantumGateBlockEntity::new, QUANTUM_GATE.get()).build(null));
    /** Nought, Monogon, Digon and Trigon glyphs (0-3). */
    public static final List<DeferredItem<QuantumGlyphItem>> GLYPHS = List.of(
            ITEMS.registerItem("glyph_nought", p -> new QuantumGlyphItem(0, p)),
            ITEMS.registerItem("glyph_monogon", p -> new QuantumGlyphItem(1, p)),
            ITEMS.registerItem("glyph_digon", p -> new QuantumGlyphItem(2, p)),
            ITEMS.registerItem("glyph_trigon", p -> new QuantumGlyphItem(3, p)));

    public static final DeferredBlock<ChargerBlock> CHARGER = BLOCKS.registerBlock("charger", ChargerBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2f, 6f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredItem<BlockItem> CHARGER_ITEM = ITEMS.registerSimpleBlockItem(CHARGER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChargerBlockEntity>> CHARGER_BE = BLOCK_ENTITIES.register("charger",
            () -> BlockEntityType.Builder.of(ChargerBlockEntity::new, CHARGER.get()).build(null));

    public static final Map<WireMaterial, DeferredBlock<FlatWireBlock>> FLAT_WIRES = new EnumMap<>(WireMaterial.class);
    public static final Map<WireMaterial, DeferredBlock<FramedWireBlock>> FRAMED_WIRES = new EnumMap<>(WireMaterial.class);
    public static final Map<WireMaterial, DeferredItem<WireItem>> WIRE_ITEMS = new EnumMap<>(WireMaterial.class);

    static {
        for (WireMaterial m : WireMaterial.values()) {
            String name = m.getSerializedName();
            DeferredBlock<FlatWireBlock> flat = BLOCKS.registerBlock(name + "_wire", p -> new FlatWireBlock(m, p), wireProperties());
            DeferredBlock<FramedWireBlock> framed = BLOCKS.registerBlock(name + "_framed_wire", p -> new FramedWireBlock(m, p), wireProperties());
            FLAT_WIRES.put(m, flat);
            FRAMED_WIRES.put(m, framed);
            WIRE_ITEMS.put(m, ITEMS.register(name + "_wire", () -> new WireItem(flat.get(), framed, m, new Item.Properties())));
        }
    }

    private static BlockBehaviour.Properties wireProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.1f).sound(SoundType.WOOL)
                .noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY);
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WireBlockEntity>> WIRE_BE = BLOCK_ENTITIES.register("wire",
            () -> {
                List<net.minecraft.world.level.block.Block> blocks = new ArrayList<>();
                FLAT_WIRES.values().forEach(b -> blocks.add(b.get()));
                FRAMED_WIRES.values().forEach(b -> blocks.add(b.get()));
                return BlockEntityType.Builder.of(WireBlockEntity::new, blocks.toArray(new net.minecraft.world.level.block.Block[0])).build(null);
            });

    public static final DeferredBlock<BatteryBlock> BATTERY = BLOCKS.registerBlock("battery", BatteryBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3f, 6f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredItem<BatteryItem> BATTERY_ITEM = ITEMS.register("battery", () -> new BatteryItem(BATTERY.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryBlockEntity>> BATTERY_BE = BLOCK_ENTITIES.register("battery",
            () -> BlockEntityType.Builder.of(BatteryBlockEntity::new, BATTERY.get()).build(null));

    public static final DeferredBlock<GeneratorBlock> SOLAR_PANEL = BLOCKS.registerBlock("solar_panel",
            p -> new GeneratorBlock(p, () -> RIRegistries.SOLAR_PANEL_BE.get(), SolarPanelBlockEntity::new, net.minecraft.world.level.block.Block.box(0, 0, 0, 16, 4.8, 16)),
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2f, 6f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> SOLAR_PANEL_ITEM = ITEMS.registerSimpleBlockItem(SOLAR_PANEL);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_BE = BLOCK_ENTITIES.register("solar_panel",
            () -> BlockEntityType.Builder.of(SolarPanelBlockEntity::new, SOLAR_PANEL.get()).build(null));
    public static final DeferredBlock<GeneratorBlock> THERMOPILE = BLOCKS.registerBlock("thermopile",
            p -> new GeneratorBlock(p, () -> RIRegistries.THERMOPILE_BE.get(), ThermopileBlockEntity::new, GeneratorBlock.full()),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(5f, 1200f).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> THERMOPILE_ITEM = ITEMS.registerSimpleBlockItem(THERMOPILE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThermopileBlockEntity>> THERMOPILE_BE = BLOCK_ENTITIES.register("thermopile",
            () -> BlockEntityType.Builder.of(ThermopileBlockEntity::new, THERMOPILE.get()).build(null));

    public static final DeferredBlock<MultimeterBlock> MULTIMETER = BLOCKS.registerBlock("multimeter", MultimeterBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5f, 6f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredItem<BlockItem> MULTIMETER_ITEM = ITEMS.registerSimpleBlockItem(MULTIMETER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MultimeterBlockEntity>> MULTIMETER_BE = BLOCK_ENTITIES.register("multimeter",
            () -> BlockEntityType.Builder.of(MultimeterBlockEntity::new, MULTIMETER.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<MultimeterMenu>> MULTIMETER_MENU = MENUS.register("multimeter",
            () -> IMenuTypeExtension.create(MultimeterMenu::fromNetwork));

    public static final DeferredBlock<TransformerBlock> TRANSFORMER = BLOCKS.registerBlock("transformer", TransformerBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2f, 6f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredItem<BlockItem> TRANSFORMER_ITEM = ITEMS.registerSimpleBlockItem(TRANSFORMER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TransformerBlockEntity>> TRANSFORMER_BE = BLOCK_ENTITIES.register("transformer",
            () -> BlockEntityType.Builder.of(TransformerBlockEntity::new, TRANSFORMER.get()).build(null));

    public static final String[] MECH_TIERS = {"wood", "stone", "metal", "creative"};
    public static final List<DeferredBlock<GearBlock>> GEARS = new ArrayList<>();
    public static final List<DeferredBlock<ShaftBlock>> SHAFTS = new ArrayList<>();

    static {
        for (int t = 0; t < 4; t++) {
            int tier = t;
            DeferredBlock<GearBlock> gear = BLOCKS.registerBlock("gear_" + MECH_TIERS[t], p -> new GearBlock(tier, p),
                    BlockBehaviour.Properties.of().mapColor(tier == 0 ? MapColor.WOOD : tier == 1 ? MapColor.STONE : MapColor.METAL)
                            .strength(1.5f, 4f).sound(tier == 0 ? SoundType.WOOD : tier == 1 ? SoundType.STONE : SoundType.METAL).noOcclusion());
            GEARS.add(gear);
            ITEMS.registerSimpleBlockItem(gear);
            if (t < 3) {
                DeferredBlock<ShaftBlock> shaft = BLOCKS.registerBlock("shaft_" + MECH_TIERS[t], p -> new ShaftBlock(tier, p),
                        BlockBehaviour.Properties.of().mapColor(tier == 0 ? MapColor.WOOD : tier == 1 ? MapColor.STONE : MapColor.METAL)
                                .strength(1.5f, 4f).sound(tier == 0 ? SoundType.WOOD : tier == 1 ? SoundType.STONE : SoundType.METAL).noOcclusion());
                SHAFTS.add(shaft);
                ITEMS.registerSimpleBlockItem(shaft);
            }
        }
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GearBlockEntity>> GEAR_BE = BLOCK_ENTITIES.register("gear",
            () -> BlockEntityType.Builder.of(GearBlockEntity::new, GEARS.stream().map(DeferredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShaftBlockEntity>> SHAFT_BE = BLOCK_ENTITIES.register("shaft",
            () -> BlockEntityType.Builder.of(ShaftBlockEntity::new, SHAFTS.stream().map(DeferredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
    public static final DeferredBlock<MotorBlock> MOTOR = BLOCKS.registerBlock("motor", MotorBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3f, 6f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> MOTOR_ITEM = ITEMS.registerSimpleBlockItem(MOTOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MotorBlockEntity>> MOTOR_BE = BLOCK_ENTITIES.register("motor",
            () -> BlockEntityType.Builder.of(MotorBlockEntity::new, MOTOR.get()).build(null));
    public static final List<DeferredBlock<TurbineBlock>> TURBINES = new ArrayList<>();

    static {
        for (TurbineBlock.Kind kind : TurbineBlock.Kind.values()) {
            for (int t = 0; t < 3; t++) {
                int tier = t;
                DeferredBlock<TurbineBlock> turbine = BLOCKS.registerBlock(kind.name().toLowerCase() + "_turbine_" + MECH_TIERS[t], p -> new TurbineBlock(kind, tier, p),
                        BlockBehaviour.Properties.of().mapColor(tier == 0 ? MapColor.WOOD : tier == 1 ? MapColor.STONE : MapColor.METAL)
                                .strength(2f, 4f).sound(tier == 0 ? SoundType.WOOD : tier == 1 ? SoundType.STONE : SoundType.METAL).noOcclusion());
                TURBINES.add(turbine);
                ITEMS.registerSimpleBlockItem(turbine);
            }
        }
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TurbineBlockEntity>> TURBINE_BE = BLOCK_ENTITIES.register("turbine",
            () -> BlockEntityType.Builder.of(TurbineBlockEntity::new, TURBINES.stream().map(DeferredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
    // ---- ore processing ----
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> MATERIAL = COMPONENTS.registerComponentType("material",
            b -> b.persistent(Codec.STRING).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<ItemStack>>> IMPRINT_ITEMS = COMPONENTS.registerComponentType("imprint_items",
            b -> b.persistent(ItemStack.CODEC.listOf()).networkSynchronized(ItemStack.LIST_STREAM_CODEC));
    public static final DeferredItem<OreResourceItem> RUBBLE = ITEMS.registerItem("rubble", p -> new OreResourceItem(OreResourceItem.Form.RUBBLE, p));
    public static final DeferredItem<OreResourceItem> DUST = ITEMS.registerItem("dust", p -> new OreResourceItem(OreResourceItem.Form.DUST, p));
    public static final DeferredItem<OreResourceItem> REFINED_DUST = ITEMS.registerItem("refined_dust", p -> new OreResourceItem(OreResourceItem.Form.REFINED_DUST, p));
    public static final DeferredItem<PoolBucketItem> MOLTEN_BUCKET = ITEMS.registerItem("molten_bucket", p -> new PoolBucketItem(PoolBlock.Kind.MOLTEN, p), new Item.Properties().stacksTo(1));
    public static final DeferredItem<PoolBucketItem> MIXTURE_BUCKET = ITEMS.registerItem("mixture_bucket", p -> new PoolBucketItem(PoolBlock.Kind.MIXTURE, p), new Item.Properties().stacksTo(1));
    public static final DeferredBlock<DustPileBlock> DUST_PILE = BLOCKS.registerBlock("dust_pile", p -> new DustPileBlock(false, p),
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.5f).sound(SoundType.SAND).noOcclusion());
    public static final DeferredBlock<DustPileBlock> REFINED_DUST_PILE = BLOCKS.registerBlock("refined_dust_pile", p -> new DustPileBlock(true, p),
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.5f).sound(SoundType.SAND).noOcclusion());
    public static final DeferredBlock<PoolBlock> MOLTEN_POOL = BLOCKS.registerBlock("molten_pool", p -> new PoolBlock(PoolBlock.Kind.MOLTEN, p),
            BlockBehaviour.Properties.of().mapColor(MapColor.FIRE).strength(100f).noOcclusion().noLootTable().lightLevel(s -> 12).replaceable().liquid());
    public static final DeferredBlock<PoolBlock> MIXTURE_POOL = BLOCKS.registerBlock("mixture_pool", p -> new PoolBlock(PoolBlock.Kind.MIXTURE, p),
            BlockBehaviour.Properties.of().mapColor(MapColor.WATER).strength(100f).noOcclusion().noLootTable().replaceable().liquid());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MaterialBlockEntity>> MATERIAL_BE = BLOCK_ENTITIES.register("material",
            () -> BlockEntityType.Builder.of(MaterialBlockEntity::new, DUST_PILE.get(), REFINED_DUST_PILE.get(), MOLTEN_POOL.get(), MIXTURE_POOL.get()).build(null));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DustSmeltingRecipe.Smelting>> DUST_SMELTING = RECIPE_SERIALIZERS.register("dust_smelting",
            () -> DustSmeltingRecipe.serializer(DustSmeltingRecipe.Smelting::new, DustSmeltingRecipe.Smelting::getCookingTime, 200));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DustSmeltingRecipe.Blasting>> DUST_BLASTING = RECIPE_SERIALIZERS.register("dust_blasting",
            () -> DustSmeltingRecipe.serializer(DustSmeltingRecipe.Blasting::new, DustSmeltingRecipe.Blasting::getCookingTime, 100));

    public static final DeferredBlock<MachineBlock> MECHANICAL_PISTON = BLOCKS.registerBlock("mechanical_piston",
            p -> new MachineBlock(p, () -> RIRegistries.MECHANICAL_PISTON_BE.get(), MechanicalPistonBlockEntity::new, net.minecraft.world.phys.shapes.Shapes.block(), RenderShape.MODEL),
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3f, 6f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MechanicalPistonBlockEntity>> MECHANICAL_PISTON_BE = BLOCK_ENTITIES.register("mechanical_piston",
            () -> BlockEntityType.Builder.of(MechanicalPistonBlockEntity::new, MECHANICAL_PISTON.get()).build(null));
    public static final DeferredBlock<MachineBlock> GRINDING_WHEEL = BLOCKS.registerBlock("grinding_wheel",
            p -> new MachineBlock(p, () -> RIRegistries.GRINDING_WHEEL_BE.get(), GrindingWheelBlockEntity::new,
                    net.minecraft.world.level.block.Block.box(0.8, 0.8, 0.8, 15.2, 15.2, 15.2), RenderShape.ENTITYBLOCK_ANIMATED),
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3f, 6f).sound(SoundType.STONE).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GrindingWheelBlockEntity>> GRINDING_WHEEL_BE = BLOCK_ENTITIES.register("grinding_wheel",
            () -> BlockEntityType.Builder.of(GrindingWheelBlockEntity::new, GRINDING_WHEEL.get()).build(null));
    public static final DeferredBlock<MachineBlock> MIXER = BLOCKS.registerBlock("mixer",
            p -> new MachineBlock(p, () -> RIRegistries.MIXER_BE.get(), MixerBlockEntity::new, net.minecraft.world.level.block.Block.box(3, 0, 3, 13, 16, 13), RenderShape.MODEL),
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3f, 6f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MixerBlockEntity>> MIXER_BE = BLOCK_ENTITIES.register("mixer",
            () -> BlockEntityType.Builder.of(MixerBlockEntity::new, MIXER.get()).build(null));
    public static final DeferredBlock<FilterBlock> FILTER = BLOCKS.registerBlock("filter", FilterBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2f, 6f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ImprintableBlockEntity>> IMPRINTABLE_BE = BLOCK_ENTITIES.register("imprintable",
            () -> BlockEntityType.Builder.of(ImprintableBlockEntity::new, FILTER.get()).build(null));
    public static final DeferredBlock<MillstoneBlock> MILLSTONE = BLOCKS.registerBlock("millstone", MillstoneBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3f, 6f).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MillstoneBlock.Tile>> MILLSTONE_BE = BLOCK_ENTITIES.register("millstone",
            () -> BlockEntityType.Builder.of(MillstoneBlock.Tile::new, MILLSTONE.get()).build(null));
    public static final DeferredItem<ImprintItem> IMPRINT = ITEMS.registerItem("imprint", ImprintItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredHolder<SoundEvent, SoundEvent> GRINDER_SOUND = SOUNDS.register("grinder",
            () -> SoundEvent.createVariableRangeEvent(ResonantInduction.id("grinder")));

    static {
        ITEMS.registerSimpleBlockItem(MECHANICAL_PISTON);
        ITEMS.registerSimpleBlockItem(GRINDING_WHEEL);
        ITEMS.registerSimpleBlockItem(MIXER);
        ITEMS.registerSimpleBlockItem(FILTER);
        ITEMS.registerSimpleBlockItem(MILLSTONE);
    }

    // ---- smelting: fluids, firebox, hot plate, casting mold ----
    public static final DeferredHolder<FluidType, MaterialFluidType> MOLTEN_METAL_TYPE = FLUID_TYPES.register("molten_metal",
            () -> new MaterialFluidType(FluidType.Properties.create().temperature(1811).lightLevel(12).density(7900).viscosity(6000)
                    .canSwim(false).canDrown(false).canExtinguish(false).supportsBoating(false)));
    public static final DeferredHolder<FluidType, MaterialFluidType> DUST_MIXTURE_TYPE = FLUID_TYPES.register("dust_mixture",
            () -> new MaterialFluidType(FluidType.Properties.create().density(1500).viscosity(2000)));
    public static final DeferredHolder<Fluid, MaterialFluid> MOLTEN_METAL = FLUIDS.register("molten_metal", () -> new MaterialFluid(MOLTEN_METAL_TYPE::get));
    public static final DeferredHolder<Fluid, MaterialFluid> DUST_MIXTURE = FLUIDS.register("dust_mixture", () -> new MaterialFluid(DUST_MIXTURE_TYPE::get));

    public static final DeferredBlock<FireboxBlock> FIREBOX = BLOCKS.registerBlock("firebox", p -> new FireboxBlock(false, p),
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.5f, 6f).sound(SoundType.STONE).requiresCorrectToolForDrops()
                    .lightLevel(s -> s.getValue(FireboxBlock.LIT) ? 13 : 0));
    public static final DeferredBlock<FireboxBlock> ELECTRIC_FIREBOX = BLOCKS.registerBlock("electric_firebox", p -> new FireboxBlock(true, p),
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5f, 6f).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(s -> s.getValue(FireboxBlock.LIT) ? 13 : 0));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FireboxBlockEntity>> FIREBOX_BE = BLOCK_ENTITIES.register("firebox",
            () -> BlockEntityType.Builder.of(FireboxBlockEntity::new, FIREBOX.get(), ELECTRIC_FIREBOX.get()).build(null));
    public static final DeferredBlock<HotPlateBlock> HOT_PLATE = BLOCKS.registerBlock("hot_plate", HotPlateBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2f, 6f).sound(SoundType.STONE).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HotPlateBlockEntity>> HOT_PLATE_BE = BLOCK_ENTITIES.register("hot_plate",
            () -> BlockEntityType.Builder.of(HotPlateBlockEntity::new, HOT_PLATE.get()).build(null));
    public static final DeferredBlock<CastingMoldBlock> CASTING_MOLD = BLOCKS.registerBlock("casting_mold", CastingMoldBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3f, 6f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CastingMoldBlockEntity>> CASTING_MOLD_BE = BLOCK_ENTITIES.register("casting_mold",
            () -> BlockEntityType.Builder.of(CastingMoldBlockEntity::new, CASTING_MOLD.get()).build(null));

    static {
        ITEMS.registerSimpleBlockItem(FIREBOX);
        ITEMS.registerSimpleBlockItem(ELECTRIC_FIREBOX);
        ITEMS.registerSimpleBlockItem(HOT_PLATE);
        ITEMS.registerSimpleBlockItem(CASTING_MOLD);
    }

    // ---- fluids: gutter, tank, grate, pipes, pump ----
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> FLUID_CONTENT = COMPONENTS.registerComponentType("fluid",
            b -> b.persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC));
    public static final DeferredBlock<GutterBlock> GUTTER = BLOCKS.registerBlock("gutter", GutterBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5f, 6f).sound(SoundType.STONE).noOcclusion());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GutterBlockEntity>> GUTTER_BE = BLOCK_ENTITIES.register("gutter",
            () -> BlockEntityType.Builder.of(GutterBlockEntity::new, GUTTER.get()).build(null));
    public static final DeferredBlock<TankBlock> TANK = BLOCKS.registerBlock("tank", TankBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(1f, 6f).sound(SoundType.GLASS).noOcclusion()
                    .isValidSpawn((s, l, p, e) -> false).isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false));
    public static final DeferredItem<TankItem> TANK_ITEM = ITEMS.register("tank", () -> new TankItem(TANK.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TankBlockEntity>> TANK_BE = BLOCK_ENTITIES.register("tank",
            () -> BlockEntityType.Builder.of(TankBlockEntity::new, TANK.get()).build(null));
    public static final DeferredBlock<GrateBlock> GRATE = BLOCKS.registerBlock("grate", GrateBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2f, 6f).sound(SoundType.WOOD));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GrateBlockEntity>> GRATE_BE = BLOCK_ENTITIES.register("grate",
            () -> BlockEntityType.Builder.of(GrateBlockEntity::new, GRATE.get()).build(null));
    public static final Map<PipeMaterial, DeferredBlock<PipeBlock>> PIPES = new EnumMap<>(PipeMaterial.class);

    static {
        ITEMS.registerSimpleBlockItem(GUTTER);
        ITEMS.registerSimpleBlockItem(GRATE);
        for (PipeMaterial m : PipeMaterial.values()) {
            DeferredBlock<PipeBlock> pipe = BLOCKS.registerBlock(m.getSerializedName() + "_pipe", p -> new PipeBlock(m, p),
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1f, 4f).sound(m == PipeMaterial.CERAMIC ? SoundType.STONE : SoundType.METAL)
                            .noOcclusion().forceSolidOn());
            PIPES.put(m, pipe);
            ITEMS.registerSimpleBlockItem(pipe);
        }
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PipeBlockEntity>> PIPE_BE = BLOCK_ENTITIES.register("pipe",
            () -> BlockEntityType.Builder.of(PipeBlockEntity::new, PIPES.values().stream().map(DeferredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
    public static final DeferredBlock<MachineBlock> PUMP = BLOCKS.registerBlock("pump",
            p -> new MachineBlock(p, () -> RIRegistries.PUMP_BE.get(), PumpBlockEntity::new, net.minecraft.world.level.block.Block.box(0, 0, 0, 16, 14, 16), RenderShape.MODEL),
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3f, 6f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PumpBlockEntity>> PUMP_BE = BLOCK_ENTITIES.register("pump",
            () -> BlockEntityType.Builder.of(PumpBlockEntity::new, PUMP.get()).build(null));

    static {
        ITEMS.registerSimpleBlockItem(PUMP);
    }

    // ---- workshop: crates, engineering table, hammer, imprinter, turntable ----
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CrateContents>> CRATE_CONTENTS = COMPONENTS.registerComponentType("crate_contents",
            b -> b.persistent(CrateContents.CODEC).networkSynchronized(CrateContents.STREAM_CODEC));
    public static final String[] CRATE_TIERS = {"wood", "iron", "steel"};
    public static final List<DeferredBlock<CrateBlock>> CRATES = new ArrayList<>();
    public static final List<DeferredItem<CrateItem>> CRATE_ITEMS = new ArrayList<>();

    static {
        for (int t = 0; t < 3; t++) {
            int tier = t;
            DeferredBlock<CrateBlock> crate = BLOCKS.registerBlock("crate_" + CRATE_TIERS[t], p -> new CrateBlock(tier, p),
                    BlockBehaviour.Properties.of().mapColor(tier == 0 ? MapColor.WOOD : MapColor.METAL).strength(tier == 0 ? 2f : 3f, 6f)
                            .sound(tier == 0 ? SoundType.WOOD : SoundType.METAL));
            CRATES.add(crate);
            CRATE_ITEMS.add(ITEMS.register("crate_" + CRATE_TIERS[t], () -> new CrateItem(crate.get(), new Item.Properties())));
        }
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrateBlockEntity>> CRATE_BE = BLOCK_ENTITIES.register("crate",
            () -> BlockEntityType.Builder.of(CrateBlockEntity::new, CRATES.stream().map(DeferredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
    public static final DeferredBlock<EngineeringTableBlock> ENGINEERING_TABLE = BLOCKS.registerBlock("engineering_table", EngineeringTableBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EngineeringTableBlockEntity>> ENGINEERING_TABLE_BE = BLOCK_ENTITIES.register("engineering_table",
            () -> BlockEntityType.Builder.of(EngineeringTableBlockEntity::new, ENGINEERING_TABLE.get()).build(null));
    public static final DeferredItem<HammerItem> HAMMER = ITEMS.registerItem("hammer", HammerItem::new);
    public static final DeferredHolder<SoundEvent, SoundEvent> HAMMER_SOUND = SOUNDS.register("hammer",
            () -> SoundEvent.createVariableRangeEvent(ResonantInduction.id("hammer")));
    public static final DeferredBlock<ImprinterBlock> IMPRINTER = BLOCKS.registerBlock("imprinter", ImprinterBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.5f, 6f).sound(SoundType.STONE));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ImprinterBlockEntity>> IMPRINTER_BE = BLOCK_ENTITIES.register("imprinter",
            () -> BlockEntityType.Builder.of(ImprinterBlockEntity::new, IMPRINTER.get()).build(null));
    public static final DeferredBlock<TurntableBlock> TURNTABLE = BLOCKS.registerBlock("turntable", TurntableBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.5f, 6f).sound(SoundType.STONE));

    static {
        ITEMS.registerSimpleBlockItem(ENGINEERING_TABLE);
        ITEMS.registerSimpleBlockItem(IMPRINTER);
        ITEMS.registerSimpleBlockItem(TURNTABLE);
    }

    // ---- logistics: conveyor belt, manipulator, detector, sorter, breaker, placer ----
    public static final DeferredBlock<ConveyorBeltBlock> CONVEYOR_BELT = BLOCKS.registerBlock("conveyor_belt", ConveyorBeltBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5f, 6f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredHolder<SoundEvent, SoundEvent> CONVEYOR_SOUND = SOUNDS.register("conveyor",
            () -> SoundEvent.createVariableRangeEvent(ResonantInduction.id("conveyor")));
    public static final DeferredBlock<ManipulatorBlock> MANIPULATOR = BLOCKS.registerBlock("manipulator", ManipulatorBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2f, 6f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ManipulatorBlockEntity>> MANIPULATOR_BE = BLOCK_ENTITIES.register("manipulator",
            () -> BlockEntityType.Builder.of(ManipulatorBlockEntity::new, MANIPULATOR.get()).build(null));
    public static final DeferredBlock<DetectorBlock> DETECTOR = BLOCKS.registerBlock("detector", DetectorBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2f, 6f).sound(SoundType.METAL).isRedstoneConductor((s, l, p) -> false));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DetectorBlockEntity>> DETECTOR_BE = BLOCK_ENTITIES.register("detector",
            () -> BlockEntityType.Builder.of(DetectorBlockEntity::new, DETECTOR.get()).build(null));
    public static final DeferredBlock<SorterBlock> SORTER = BLOCKS.registerBlock("sorter", SorterBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2f, 6f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SorterBlockEntity>> SORTER_BE = BLOCK_ENTITIES.register("sorter",
            () -> BlockEntityType.Builder.of(SorterBlockEntity::new, SORTER.get()).build(null));
    public static final DeferredBlock<BreakerBlock> BREAKER = BLOCKS.registerBlock("breaker", BreakerBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3f, 6f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredBlock<PlacerBlock> PLACER = BLOCKS.registerBlock("placer", PlacerBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3f, 6f).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlacerBlockEntity>> PLACER_BE = BLOCK_ENTITIES.register("placer",
            () -> BlockEntityType.Builder.of(PlacerBlockEntity::new, PLACER.get()).build(null));

    static {
        ITEMS.registerSimpleBlockItem(CONVEYOR_BELT);
        ITEMS.registerSimpleBlockItem(MANIPULATOR);
        ITEMS.registerSimpleBlockItem(DETECTOR);
        ITEMS.registerSimpleBlockItem(SORTER);
        ITEMS.registerSimpleBlockItem(BREAKER);
        ITEMS.registerSimpleBlockItem(PLACER);
    }

    /** The original's machine material blocks: decoration, creative only as in the original (it had no recipes for them). */
    public static final List<DeferredBlock<net.minecraft.world.level.block.Block>> MACHINE_MATERIALS = new ArrayList<>();

    static {
        for (String name : new String[] {"material_stone_brick", "material_stone_brick2", "material_stone_chiseled", "material_stone_cobble", "material_stone_cracked", "material_stone", "material_stone_slab", "material_stone_mossy", "material_steel_dark", "material_steel_tint", "material_steel"}) {
            boolean steel = name.contains("steel");
            DeferredBlock<net.minecraft.world.level.block.Block> b = BLOCKS.registerSimpleBlock(name, BlockBehaviour.Properties.of()
                    .mapColor(steel ? MapColor.METAL : MapColor.STONE).strength(steel ? 5f : 1.5f, 6f).sound(steel ? SoundType.METAL : SoundType.STONE)
                    .requiresCorrectToolForDrops());
            MACHINE_MATERIALS.add(b);
            ITEMS.registerSimpleBlockItem(b);
        }
    }

    // ---- atomic: radiation, uranium, cells, fuel rods, hazmat, fluids ----
    public static final DeferredHolder<MobEffect, Radiation.Effect> RADIATION = MOB_EFFECTS.register("radiation", Radiation.Effect::new);
    public static final ResourceKey<DamageType> RADIATION_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, ResonantInduction.id("radiation"));
    public static final DeferredHolder<SoundEvent, SoundEvent> ANTIMATTER_SOUND = SOUNDS.register("antimatter",
            () -> SoundEvent.createVariableRangeEvent(ResonantInduction.id("antimatter")));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> HAZMAT_MATERIAL = ARMOR_MATERIALS.register("hazmat", () -> new ArmorMaterial(
            new EnumMap<>(java.util.Map.of(ArmorItem.Type.HELMET, 0, ArmorItem.Type.CHESTPLATE, 0, ArmorItem.Type.LEGGINGS, 0, ArmorItem.Type.BOOTS, 0, ArmorItem.Type.BODY, 0)),
            0, net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER, () -> net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.WHITE_WOOL),
            List.of(new ArmorMaterial.Layer(ResonantInduction.id("hazmat"))), 0, 0));

    public static final DeferredBlock<RadioactiveBlock> URANIUM_ORE = BLOCKS.registerBlock("uranium_ore", p -> new RadioactiveBlock(true, 1, 0, false, p),
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2f, 3f).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredBlock<RadioactiveBlock> RADIOACTIVE_WASTE = BLOCKS.registerBlock("radioactive_waste", p -> new RadioactiveBlock(false, 5, 2, true, p),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(0.6f).sound(SoundType.GRAVEL).lightLevel(s -> 5));

    public static final DeferredItem<RadioactiveItem> YELLOWCAKE = ITEMS.registerItem("yellowcake", RadioactiveItem::new);
    public static final DeferredItem<RadioactiveItem> URANIUM = ITEMS.registerItem("uranium", RadioactiveItem::new);
    public static final DeferredItem<RadioactiveItem> URANIUM_238 = ITEMS.registerItem("uranium_238", RadioactiveItem::new);
    public static final DeferredItem<Item> EMPTY_CELL = ITEMS.registerSimpleItem("empty_cell");
    public static final DeferredItem<Item> WATER_CELL = ITEMS.register("water_cell", () -> new Item(new Item.Properties().craftRemainder(EMPTY_CELL.get())));
    public static final DeferredItem<Item> DEUTERIUM_CELL = ITEMS.register("deuterium_cell", () -> new Item(new Item.Properties().craftRemainder(EMPTY_CELL.get())));
    public static final DeferredItem<Item> TRITIUM_CELL = ITEMS.register("tritium_cell", () -> new Item(new Item.Properties().craftRemainder(EMPTY_CELL.get())));
    public static final DeferredItem<Item> DARK_MATTER = ITEMS.register("dark_matter", () -> new Item(new Item.Properties().craftRemainder(EMPTY_CELL.get())));
    public static final DeferredItem<AntimatterItem> ANTIMATTER = ITEMS.register("antimatter", () -> new AntimatterItem(0, new Item.Properties().craftRemainder(EMPTY_CELL.get())));
    public static final DeferredItem<AntimatterItem> ANTIMATTER_GRAM = ITEMS.register("antimatter_gram", () -> new AntimatterItem(1, new Item.Properties().craftRemainder(EMPTY_CELL.get())));
    public static final DeferredItem<FuelRodItem> FISSILE_FUEL_ROD = ITEMS.registerItem("fissile_fuel_rod", p -> new FuelRodItem(true, p));
    public static final DeferredItem<FuelRodItem> BREEDER_FUEL_ROD = ITEMS.registerItem("breeder_fuel_rod", p -> new FuelRodItem(false, p));
    public static final DeferredItem<HazmatArmorItem> HAZMAT_MASK = ITEMS.registerItem("hazmat_mask", p -> new HazmatArmorItem(HAZMAT_MATERIAL, ArmorItem.Type.HELMET, p));
    public static final DeferredItem<HazmatArmorItem> HAZMAT_BODY = ITEMS.registerItem("hazmat_body", p -> new HazmatArmorItem(HAZMAT_MATERIAL, ArmorItem.Type.CHESTPLATE, p));
    public static final DeferredItem<HazmatArmorItem> HAZMAT_LEGGINGS = ITEMS.registerItem("hazmat_leggings", p -> new HazmatArmorItem(HAZMAT_MATERIAL, ArmorItem.Type.LEGGINGS, p));
    public static final DeferredItem<HazmatArmorItem> HAZMAT_BOOTS = ITEMS.registerItem("hazmat_boots", p -> new HazmatArmorItem(HAZMAT_MATERIAL, ArmorItem.Type.BOOTS, p));

    /** Gases (lighter than air) for tanks and pipes; steam and plasma are hot. */
    private static FluidType.Properties gas(int temperature) {
        return FluidType.Properties.create().density(-1000).viscosity(200).temperature(temperature).canSwim(false).canDrown(false).supportsBoating(false);
    }

    public static final DeferredHolder<FluidType, FluidType> STEAM_TYPE = FLUID_TYPES.register("steam", () -> new FluidType(gas(373)));
    public static final DeferredHolder<FluidType, FluidType> URANIUM_HEXAFLUORIDE_TYPE = FLUID_TYPES.register("uranium_hexafluoride", () -> new FluidType(gas(330)));
    public static final DeferredHolder<FluidType, FluidType> DEUTERIUM_TYPE = FLUID_TYPES.register("deuterium", () -> new FluidType(gas(300)));
    public static final DeferredHolder<FluidType, FluidType> TRITIUM_TYPE = FLUID_TYPES.register("tritium", () -> new FluidType(gas(300)));
    public static final DeferredHolder<FluidType, FluidType> PLASMA_TYPE = FLUID_TYPES.register("plasma", () -> new FluidType(gas(100_000_000).lightLevel(15)));
    public static final DeferredHolder<FluidType, FluidType> TOXIC_WASTE_TYPE = FLUID_TYPES.register("toxic_waste",
            () -> new FluidType(FluidType.Properties.create().density(1500).viscosity(3000).temperature(310).canExtinguish(true)
                    .sound(net.neoforged.neoforge.common.SoundActions.BUCKET_FILL, net.minecraft.sounds.SoundEvents.BUCKET_FILL)
                    .sound(net.neoforged.neoforge.common.SoundActions.BUCKET_EMPTY, net.minecraft.sounds.SoundEvents.BUCKET_EMPTY)));
    public static final DeferredHolder<Fluid, VirtualFluid> STEAM = FLUIDS.register("steam", () -> new VirtualFluid(STEAM_TYPE::get));
    public static final DeferredHolder<Fluid, VirtualFluid> URANIUM_HEXAFLUORIDE = FLUIDS.register("uranium_hexafluoride", () -> new VirtualFluid(URANIUM_HEXAFLUORIDE_TYPE::get));
    public static final DeferredHolder<Fluid, VirtualFluid> DEUTERIUM = FLUIDS.register("deuterium", () -> new VirtualFluid(DEUTERIUM_TYPE::get));
    public static final DeferredHolder<Fluid, VirtualFluid> TRITIUM = FLUIDS.register("tritium", () -> new VirtualFluid(TRITIUM_TYPE::get));
    public static final DeferredHolder<Fluid, VirtualFluid> PLASMA = FLUIDS.register("plasma", () -> new VirtualFluid(PLASMA_TYPE::get));
    private static BaseFlowingFluid.Properties toxicProperties() {
        return new BaseFlowingFluid.Properties(TOXIC_WASTE_TYPE, RIRegistries.TOXIC_WASTE, RIRegistries.TOXIC_WASTE_FLOWING)
                .block(RIRegistries.TOXIC_WASTE_BLOCK).bucket(RIRegistries.TOXIC_WASTE_BUCKET).tickRate(20);
    }

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> TOXIC_WASTE = FLUIDS.register("toxic_waste", () -> new BaseFlowingFluid.Source(toxicProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> TOXIC_WASTE_FLOWING = FLUIDS.register("flowing_toxic_waste",
            () -> new BaseFlowingFluid.Flowing(toxicProperties()));
    public static final DeferredBlock<ToxicWasteBlock> TOXIC_WASTE_BLOCK = BLOCKS.registerBlock("toxic_waste", p -> new ToxicWasteBlock(TOXIC_WASTE.get(), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).replaceable().noCollission().strength(100f).pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)
                    .noLootTable().liquid().sound(SoundType.EMPTY));
    public static final DeferredItem<net.minecraft.world.item.BucketItem> TOXIC_WASTE_BUCKET = ITEMS.registerItem("toxic_waste_bucket",
            p -> new net.minecraft.world.item.BucketItem(TOXIC_WASTE.get(), p), new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1));

    static {
        ITEMS.registerSimpleBlockItem(URANIUM_ORE);
        ITEMS.registerSimpleBlockItem(RADIOACTIVE_WASTE);
    }

    // ---- atomic machines: chemical extractor, nuclear boiler, centrifuge ----
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> ATOMIC_MACHINE_MENU = MENUS.register("atomic_machine",
            () -> IMenuTypeExtension.create(MachineMenu::fromNetwork));

    private static BlockBehaviour.Properties atomicMachine() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3f, 6f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops();
    }

    public static final DeferredBlock<AtomicMachineBlock> CHEMICAL_EXTRACTOR = BLOCKS.registerBlock("chemical_extractor",
            p -> new AtomicMachineBlock(p, () -> RIRegistries.CHEMICAL_EXTRACTOR_BE.get(), ChemicalExtractorBlockEntity::new, net.minecraft.world.phys.shapes.Shapes.block()),
            atomicMachine());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChemicalExtractorBlockEntity>> CHEMICAL_EXTRACTOR_BE = BLOCK_ENTITIES.register("chemical_extractor",
            () -> BlockEntityType.Builder.of(ChemicalExtractorBlockEntity::new, CHEMICAL_EXTRACTOR.get()).build(null));
    public static final DeferredBlock<AtomicMachineBlock> NUCLEAR_BOILER = BLOCKS.registerBlock("nuclear_boiler",
            p -> new AtomicMachineBlock(p, () -> RIRegistries.NUCLEAR_BOILER_BE.get(), NuclearBoilerBlockEntity::new, net.minecraft.world.phys.shapes.Shapes.block()),
            atomicMachine());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NuclearBoilerBlockEntity>> NUCLEAR_BOILER_BE = BLOCK_ENTITIES.register("nuclear_boiler",
            () -> BlockEntityType.Builder.of(NuclearBoilerBlockEntity::new, NUCLEAR_BOILER.get()).build(null));
    public static final DeferredBlock<AtomicMachineBlock> CENTRIFUGE = BLOCKS.registerBlock("centrifuge",
            p -> new AtomicMachineBlock(p, () -> RIRegistries.CENTRIFUGE_BE.get(), CentrifugeBlockEntity::new, net.minecraft.world.phys.shapes.Shapes.block()),
            atomicMachine());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CentrifugeBlockEntity>> CENTRIFUGE_BE = BLOCK_ENTITIES.register("centrifuge",
            () -> BlockEntityType.Builder.of(CentrifugeBlockEntity::new, CENTRIFUGE.get()).build(null));

    static {
        ITEMS.registerSimpleBlockItem(CHEMICAL_EXTRACTOR);
        ITEMS.registerSimpleBlockItem(NUCLEAR_BOILER);
        ITEMS.registerSimpleBlockItem(CENTRIFUGE);
    }

    // ---- atomic: fission reactor, turbine, sensors, plasma ----
    public static final DeferredHolder<SoundEvent, SoundEvent> REACTOR_CELL_SOUND = SOUNDS.register("reactorcell",
            () -> SoundEvent.createVariableRangeEvent(ResonantInduction.id("reactorcell")));
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_SOUND = SOUNDS.register("alarm",
            () -> SoundEvent.createVariableRangeEvent(ResonantInduction.id("alarm")));
    public static final DeferredHolder<SoundEvent, SoundEvent> TURBINE_SOUND = SOUNDS.register("turbine",
            () -> SoundEvent.createVariableRangeEvent(ResonantInduction.id("turbine")));

    public static final DeferredBlock<ReactorCellBlock> REACTOR_CELL = BLOCKS.registerBlock("reactor_cell", ReactorCellBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4f, 12f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorCellBlockEntity>> REACTOR_CELL_BE = BLOCK_ENTITIES.register("reactor_cell",
            () -> BlockEntityType.Builder.of(ReactorCellBlockEntity::new, REACTOR_CELL.get()).build(null));
    public static final DeferredBlock<ControlRodBlock> CONTROL_ROD = BLOCKS.registerBlock("control_rod", ControlRodBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3f, 6f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredBlock<ThermometerBlock> THERMOMETER = BLOCKS.registerBlock("thermometer", ThermometerBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2f, 6f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredItem<ThermometerItem> THERMOMETER_ITEM = ITEMS.register("thermometer", () -> new ThermometerItem(THERMOMETER.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThermometerBlock.Tile>> THERMOMETER_BE = BLOCK_ENTITIES.register("thermometer",
            () -> BlockEntityType.Builder.of(ThermometerBlock.Tile::new, THERMOMETER.get()).build(null));
    public static final DeferredBlock<SirenBlock> SIREN = BLOCKS.registerBlock("siren", SirenBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2f, 6f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredBlock<ElectricTurbineBlock> ELECTRIC_TURBINE = BLOCKS.registerBlock("electric_turbine", ElectricTurbineBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3f, 6f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricTurbineBlockEntity>> ELECTRIC_TURBINE_BE = BLOCK_ENTITIES.register("electric_turbine",
            () -> BlockEntityType.Builder.of(ElectricTurbineBlockEntity::new, ELECTRIC_TURBINE.get()).build(null));
    public static final DeferredBlock<FunnelBlock> FUNNEL = BLOCKS.registerBlock("steam_funnel", FunnelBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2f, 6f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FunnelBlock.Tile>> FUNNEL_BE = BLOCK_ENTITIES.register("steam_funnel",
            () -> BlockEntityType.Builder.of(FunnelBlock.Tile::new, FUNNEL.get()).build(null));
    public static final DeferredBlock<PlasmaBlock> PLASMA_BLOCK = BLOCKS.registerBlock("plasma", PlasmaBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(-1f, 3600000f).noLootTable().noOcclusion().noCollission().lightLevel(s -> 7)
                    .replaceable().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlasmaBlock.Tile>> PLASMA_BE = BLOCK_ENTITIES.register("plasma",
            () -> BlockEntityType.Builder.of(PlasmaBlock.Tile::new, PLASMA_BLOCK.get()).build(null));

    static {
        ITEMS.registerSimpleBlockItem(REACTOR_CELL);
        ITEMS.registerSimpleBlockItem(CONTROL_ROD);
        ITEMS.registerSimpleBlockItem(SIREN);
        ITEMS.registerSimpleBlockItem(ELECTRIC_TURBINE);
        ITEMS.registerSimpleBlockItem(FUNNEL);
    }

    // ---- atomic: electromagnets, particle accelerator, fulmination generator, quantum assembler, plasma heater ----
    public static final DeferredHolder<SoundEvent, SoundEvent> ACCELERATOR_SOUND = SOUNDS.register("accelerator",
            () -> SoundEvent.createVariableRangeEvent(ResonantInduction.id("accelerator")));
    public static final DeferredHolder<SoundEvent, SoundEvent> ASSEMBLER_SOUND = SOUNDS.register("assembler",
            () -> SoundEvent.createVariableRangeEvent(ResonantInduction.id("assembler")));

    public static final DeferredBlock<ElectromagnetBlock> ELECTROMAGNET = BLOCKS.registerBlock("electromagnet", p -> new ElectromagnetBlock(false, p),
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3f, 12f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredBlock<ElectromagnetBlock> ELECTROMAGNET_GLASS = BLOCKS.registerBlock("electromagnet_glass", p -> new ElectromagnetBlock(true, p),
            BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(3f, 12f).sound(SoundType.GLASS).requiresCorrectToolForDrops().noOcclusion()
                    .isValidSpawn((s, l, p, e) -> false).isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false));
    public static final DeferredBlock<AcceleratorBlock> ACCELERATOR = BLOCKS.registerBlock("particle_accelerator", AcceleratorBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3f, 6f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AcceleratorBlockEntity>> ACCELERATOR_BE = BLOCK_ENTITIES.register("particle_accelerator",
            () -> BlockEntityType.Builder.of(AcceleratorBlockEntity::new, ACCELERATOR.get()).build(null));
    public static final DeferredHolder<EntityType<?>, EntityType<ParticleEntity>> PARTICLE = ENTITY_TYPES.register("accelerated_particle",
            () -> EntityType.Builder.<ParticleEntity>of(ParticleEntity::new, MobCategory.MISC).sized(0.3f, 0.3f).clientTrackingRange(16).updateInterval(1)
                    .noSave().fireImmune().build("accelerated_particle"));
    public static final DeferredBlock<FulminationBlock> FULMINATION = BLOCKS.registerBlock("fulmination_generator", FulminationBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(10f, 5000f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FulminationBlock.Tile>> FULMINATION_BE = BLOCK_ENTITIES.register("fulmination_generator",
            () -> BlockEntityType.Builder.of(FulminationBlock.Tile::new, FULMINATION.get()).build(null));
    public static final DeferredBlock<AtomicMachineBlock> QUANTUM_ASSEMBLER = BLOCKS.registerBlock("quantum_assembler",
            p -> new AtomicMachineBlock(p, () -> RIRegistries.QUANTUM_ASSEMBLER_BE.get(), QuantumAssemblerBlockEntity::new, net.minecraft.world.phys.shapes.Shapes.block()),
            atomicMachine());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QuantumAssemblerBlockEntity>> QUANTUM_ASSEMBLER_BE = BLOCK_ENTITIES.register("quantum_assembler",
            () -> BlockEntityType.Builder.of(QuantumAssemblerBlockEntity::new, QUANTUM_ASSEMBLER.get()).build(null));

    public static final DeferredBlock<PlasmaHeaterBlock> PLASMA_HEATER = BLOCKS.registerBlock("plasma_heater", PlasmaHeaterBlock::new, atomicMachine());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlasmaHeaterBlockEntity>> PLASMA_HEATER_BE = BLOCK_ENTITIES.register("plasma_heater",
            () -> BlockEntityType.Builder.of(PlasmaHeaterBlockEntity::new, PLASMA_HEATER.get()).build(null));

    static {
        ITEMS.registerSimpleBlockItem(PLASMA_HEATER);
        ITEMS.registerSimpleBlockItem(ELECTROMAGNET);
        ITEMS.registerSimpleBlockItem(ELECTROMAGNET_GLASS);
        ITEMS.registerSimpleBlockItem(ACCELERATOR);
        ITEMS.registerSimpleBlockItem(FULMINATION);
        ITEMS.registerSimpleBlockItem(QUANTUM_ASSEMBLER);
    }

    // ---- schematics ----
    public static final DeferredBlock<CreativeBuilderBlock> CREATIVE_BUILDER = BLOCKS.registerBlock("creative_builder", CreativeBuilderBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_MAGENTA).strength(-1f, 3600000f).sound(SoundType.METAL).noLootTable());

    static {
        ITEMS.registerSimpleBlockItem(CREATIVE_BUILDER);
    }

    public static final DeferredItem<HandCrankItem> HAND_CRANK = ITEMS.registerItem("hand_crank", HandCrankItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredHolder<SoundEvent, SoundEvent> GEAR_CRANK = SOUNDS.register("gear_crank",
            () -> SoundEvent.createVariableRangeEvent(ResonantInduction.id("gear_crank")));

    /** Stored FE of energy items. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY = COMPONENTS.registerComponentType("energy",
            b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BATTERY_TIER = COMPONENTS.registerComponentType("battery_tier",
            b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LASER_MODE = COMPONENTS.registerComponentType("laser_mode",
            b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredItem<MiningLaserItem> MINING_LASER = ITEMS.registerItem("mining_laser", MiningLaserItem::new,
            new Item.Properties().stacksTo(1));

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<RIFeatures.FeatureCondition>> FEATURE_CONDITION =
            CONDITIONS.register("feature", () -> RIFeatures.FeatureCondition.CODEC);

    /** The device a Quantum Entangler has marked, waiting to be linked with a second one. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> LINK_TARGET = COMPONENTS.registerComponentType("link_target",
            b -> b.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC));

    public static final DeferredItem<QuantumEntanglerItem> QUANTUM_ENTANGLER = ITEMS.registerItem("quantum_entangler", QuantumEntanglerItem::new,
            new Item.Properties().stacksTo(1));

    public static final DeferredHolder<SoundEvent, SoundEvent> ELECTRIC_SHOCK = SOUNDS.register("electricshock",
            () -> SoundEvent.createVariableRangeEvent(ResonantInduction.id("electricshock")));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.resonantinduction"))
            .icon(() -> new ItemStack(TESLA_ITEM.get()))
            .displayItems((params, out) -> ITEMS.getEntries().forEach(e -> {
                if (e.get() instanceof BatteryItem battery) {
                    battery.variants().forEach(out::accept);
                } else if (e.get() instanceof OreResourceItem || e.get() instanceof PoolBucketItem) {
                    // One of each per metal, once tags are known.
                    for (String m : resonantinduction.resource.Materials.all()) {
                        out.accept(resonantinduction.resource.Materials.of(e.get(), m, 1));
                    }
                } else {
                    out.accept(e.get());
                }
            }))
            .build());

    private RIRegistries() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        COMPONENTS.register(modBus);
        SOUNDS.register(modBus);
        TABS.register(modBus);
        CONDITIONS.register(modBus);
        MENUS.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        FLUID_TYPES.register(modBus);
        FLUIDS.register(modBus);
        MOB_EFFECTS.register(modBus);
        ARMOR_MATERIALS.register(modBus);
        ENTITY_TYPES.register(modBus);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, TESLA_BE.get(), TeslaBlockEntity::getEnergyCapability);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, QUANTUM_GATE_BE.get(), QuantumGateBlockEntity::getItemCapability);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CHARGER_BE.get(), ChargerBlockEntity::getEnergyCapability);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, WIRE_BE.get(), WireBlockEntity::getEnergyCapability);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BATTERY_BE.get(), BatteryBlockEntity::getEnergyCapability);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, TRANSFORMER_BE.get(), TransformerBlockEntity::getEnergyCapability);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MOTOR_BE.get(), MotorBlockEntity::getEnergyCapability);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, SOLAR_PANEL_BE.get(), GeneratorBlockEntity::getEnergyCapability);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, THERMOPILE_BE.get(), GeneratorBlockEntity::getEnergyCapability);
        event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ctx) -> {
            int capacity = BatteryItem.capacity(stack);
            return new ComponentEnergyStorage(stack, ENERGY.get(), capacity, Math.max(1, capacity / 100), Math.max(1, capacity / 100));
        }, BATTERY_ITEM.get());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CHARGER_BE.get(), ChargerBlockEntity::getItemCapability);
        // Original transfer rate: a hundredth of the battery per tick.
        event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ctx) -> {
            int capacity = MiningLaserItem.capacity();
            return new ComponentEnergyStorage(stack, ENERGY.get(), capacity, Math.max(1, capacity / 100), capacity);
        }, MINING_LASER.get());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, QUANTUM_GATE_BE.get(), QuantumGateBlockEntity::getFluidCapability);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FIREBOX_BE.get(), FireboxBlockEntity::getFluidCapability);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, FIREBOX_BE.get(), FireboxBlockEntity::getEnergyCapability);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FIREBOX_BE.get(), FireboxBlockEntity::getItemCapability);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, HOT_PLATE_BE.get(), (be, side) -> be.inventory());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CASTING_MOLD_BE.get(), CastingMoldBlockEntity::getFluidCapability);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, GUTTER_BE.get(), FluidNodeBlockEntity::getFluidCapability);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, PIPE_BE.get(), FluidNodeBlockEntity::getFluidCapability);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, GRATE_BE.get(), FluidNodeBlockEntity::getFluidCapability);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TANK_BE.get(), TankBlockEntity::getFluidCapability);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, PUMP_BE.get(), PumpBlockEntity::getFluidCapability);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CRATE_BE.get(), CrateBlockEntity::getItemCapability);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, IMPRINTER_BE.get(), ImprinterBlockEntity::getItemCapability);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SORTER_BE.get(), SorterBlockEntity::getItemCapability);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PLACER_BE.get(), PlacerBlockEntity::getItemCapability);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CHEMICAL_EXTRACTOR_BE.get(), AtomicMachineBlockEntity::getEnergyCapability);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CHEMICAL_EXTRACTOR_BE.get(), ChemicalExtractorBlockEntity::getFluidCapability);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CHEMICAL_EXTRACTOR_BE.get(), ChemicalExtractorBlockEntity::getItemCapability);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, NUCLEAR_BOILER_BE.get(), AtomicMachineBlockEntity::getEnergyCapability);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, NUCLEAR_BOILER_BE.get(), NuclearBoilerBlockEntity::getFluidCapability);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, NUCLEAR_BOILER_BE.get(), NuclearBoilerBlockEntity::getItemCapability);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CENTRIFUGE_BE.get(), AtomicMachineBlockEntity::getEnergyCapability);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ACCELERATOR_BE.get(), AcceleratorBlockEntity::getEnergyCapability);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ACCELERATOR_BE.get(), AcceleratorBlockEntity::getItemCapability);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, FULMINATION_BE.get(), FulminationBlock.Tile::getEnergyCapability);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, QUANTUM_ASSEMBLER_BE.get(), AtomicMachineBlockEntity::getEnergyCapability);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, QUANTUM_ASSEMBLER_BE.get(), QuantumAssemblerBlockEntity::getItemCapability);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, PLASMA_HEATER_BE.get(), PlasmaHeaterBlockEntity::getEnergyCapability);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, PLASMA_HEATER_BE.get(), PlasmaHeaterBlockEntity::getFluidCapability);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CENTRIFUGE_BE.get(), CentrifugeBlockEntity::getFluidCapability);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CENTRIFUGE_BE.get(), CentrifugeBlockEntity::getItemCapability);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, REACTOR_CELL_BE.get(), ReactorCellBlockEntity::getFluidCapability);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ELECTRIC_TURBINE_BE.get(), ElectricTurbineBlockEntity::getFluidCapability);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FUNNEL_BE.get(), FunnelBlock.Tile::getFluidCapability);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, REACTOR_CELL_BE.get(), (be, side) -> be.primary().inventory());
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, ctx) -> new CellFluidHandler(stack), EMPTY_CELL.get(), WATER_CELL.get(), DEUTERIUM_CELL.get(), TRITIUM_CELL.get());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CASTING_MOLD_BE.get(), CastingMoldBlockEntity::getItemCapability);
    }
}
