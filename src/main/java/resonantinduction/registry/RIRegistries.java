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
import resonantinduction.ResonantInduction;
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
            .displayItems((params, out) -> ITEMS.getEntries().forEach(e -> out.accept(e.get())))
            .build());

    private RIRegistries() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        COMPONENTS.register(modBus);
        SOUNDS.register(modBus);
        TABS.register(modBus);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, TESLA_BE.get(), TeslaBlockEntity::getEnergyCapability);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, QUANTUM_GATE_BE.get(), QuantumGateBlockEntity::getItemCapability);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, QUANTUM_GATE_BE.get(), QuantumGateBlockEntity::getFluidCapability);
    }
}
