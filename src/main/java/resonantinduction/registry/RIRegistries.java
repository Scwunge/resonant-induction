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
import resonantinduction.archaic.FilterBlock;
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
    }
}
