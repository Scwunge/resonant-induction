package resonantinduction.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import resonantinduction.ResonantInduction;
import resonantinduction.registry.RIRegistries;
import resonantinduction.tesla.TeslaBlockEntity;
import resonantinduction.wire.FlatWireBlock;
import resonantinduction.wire.WireBlock;
import resonantinduction.wire.WireBlockEntity;
import resonantinduction.wire.WireMaterial;

/** A lone Tesla coil makes a handy energy sink: it takes up to 10000 FE from any side but the top. */
@GameTestHolder(ResonantInduction.MODID)
@PrefixGameTestTemplate(false)
public class WireGameTests {
    static final String TEMPLATE = TeslaGameTests.TEMPLATE;

    static void framedLine(GameTestHelper helper, WireMaterial material, BlockPos from, int length) {
        for (int i = 0; i < length; i++) {
            helper.setBlock(from.east(i), RIRegistries.FRAMED_WIRES.get(material).get());
        }
    }

    /** Pushes energy into the wire at {@code pos} as if from the block to its west. */
    static int push(GameTestHelper helper, BlockPos pos, int amount) {
        IEnergyStorage in = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos), Direction.WEST);
        helper.assertTrue(in != null, "wire at " + pos + " exposes no energy input on its west side");
        return in.receiveEnergy(amount, false);
    }

    static TeslaBlockEntity sink(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, RIRegistries.TESLA.get());
        return helper.getBlockEntity(pos);
    }

    /** West-side input needs something there to connect to: a second sink stands in for the generator. */
    static void source(GameTestHelper helper, BlockPos wire) {
        sink(helper, wire.west());
    }

    @GameTest(template = TEMPLATE)
    public static void framedWiresCarryEnergy(GameTestHelper helper) {
        BlockPos start = new BlockPos(2, 1, 3);
        source(helper, start);
        framedLine(helper, WireMaterial.COPPER, start, 6);
        TeslaBlockEntity sink = sink(helper, start.east(6));
        helper.runAfterDelay(1, () -> {
            int sent = push(helper, start, 1500);
            helper.assertTrue(sent == 1500 && sink.getCharge() == 1500, "sent " + sent + ", sink got " + sink.getCharge());
            helper.assertTrue(coilAt(helper, start.west()).getCharge() == 0, "energy went back into its source");
            helper.succeed();
        });
    }

    static TeslaBlockEntity coilAt(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos);
    }

    @GameTest(template = TEMPLATE)
    public static void weakestWireLimitsThroughput(GameTestHelper helper) {
        BlockPos start = new BlockPos(2, 1, 3);
        source(helper, start);
        framedLine(helper, WireMaterial.TIN, start, 4);
        TeslaBlockEntity sink = sink(helper, start.east(4));
        helper.runAfterDelay(1, () -> {
            int capacity = WireMaterial.TIN.capacity();
            int sent = push(helper, start, 5000) + push(helper, start, 5000);
            helper.assertTrue(sent == capacity, "tin moved " + sent + " FE in one tick, capacity " + capacity);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void differentMetalsDoNotJoin(GameTestHelper helper) {
        BlockPos start = new BlockPos(2, 1, 3);
        source(helper, start);
        framedLine(helper, WireMaterial.COPPER, start, 2);
        framedLine(helper, WireMaterial.IRON, start.east(2), 2);
        TeslaBlockEntity sink = sink(helper, start.east(4));
        helper.runAfterDelay(1, () -> {
            push(helper, start, 1000);
            helper.assertTrue(sink.getCharge() == 0, "copper passed energy into iron wire");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void insulationColoursSeparateWires(GameTestHelper helper) {
        BlockPos start = new BlockPos(2, 1, 3);
        source(helper, start);
        framedLine(helper, WireMaterial.COPPER, start, 3);
        TeslaBlockEntity sink = sink(helper, start.east(3));
        helper.runAfterDelay(1, () -> {
            ((WireBlockEntity) helper.getBlockEntity(start.east(1))).setInsulation(true, DyeColor.RED);
            ((WireBlockEntity) helper.getBlockEntity(start.east(2))).setInsulation(true, DyeColor.BLUE);
            push(helper, start, 1000);
            helper.assertTrue(sink.getCharge() == 0, "red insulated wire joined a blue one");
            ((WireBlockEntity) helper.getBlockEntity(start.east(2))).setInsulation(true, DyeColor.WHITE);
            push(helper, start, 1000);
            helper.assertTrue(sink.getCharge() == 1000, "white insulation should join any colour, sink has " + sink.getCharge());
            helper.assertBlockProperty(start.east(1), WireBlock.INSULATED, true);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void switchWireNeedsRedstone(GameTestHelper helper) {
        BlockPos start = new BlockPos(2, 1, 3);
        source(helper, start);
        framedLine(helper, WireMaterial.COPPER, start, 3);
        TeslaBlockEntity sink = sink(helper, start.east(3));
        helper.runAfterDelay(1, () -> {
            ((WireBlockEntity) helper.getBlockEntity(start.east(1))).setSwitched(true);
            push(helper, start, 1000);
            helper.assertTrue(sink.getCharge() == 0, "an unpowered switch wire conducted");
            helper.setBlock(start.east(1).above(), Blocks.REDSTONE_BLOCK);
        });
        helper.runAfterDelay(3, () -> {
            push(helper, start, 1000);
            helper.assertTrue(sink.getCharge() == 1000, "a powered switch wire did not conduct, sink has " + sink.getCharge());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void flatWiresCarryEnergy(GameTestHelper helper) {
        BlockPos start = new BlockPos(2, 2, 3);
        for (int i = 0; i < 5; i++) {
            helper.setBlock(start.east(i).below(), Blocks.STONE);
            helper.setBlock(start.east(i), RIRegistries.FLAT_WIRES.get(WireMaterial.COPPER).get().defaultBlockState().setValue(FlatWireBlock.FACE, Direction.UP));
        }
        source(helper, start);
        TeslaBlockEntity sink = sink(helper, start.east(5));
        helper.runAfterDelay(1, () -> {
            push(helper, start, 800);
            helper.assertTrue(sink.getCharge() == 800, "flat wire delivered " + sink.getCharge());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void bareLiveWiresShock(GameTestHelper helper) {
        BlockPos start = new BlockPos(2, 1, 3);
        source(helper, start);
        framedLine(helper, WireMaterial.ALUMINUM, start, 3);
        sink(helper, start.east(3));
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, start.east(1));
        float before = pig.getHealth();
        helper.onEachTick(() -> push(helper, start, 100));
        helper.succeedWhen(() -> helper.assertTrue(pig.getHealth() < before || !pig.isAlive(), "pig in a live bare wire was not hurt"));
    }
}
