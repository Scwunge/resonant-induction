package resonantinduction.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import resonantinduction.ResonantInduction;
import resonantinduction.registry.RIRegistries;
import resonantinduction.tesla.TeslaBlockEntity;

@GameTestHolder(ResonantInduction.MODID)
@PrefixGameTestTemplate(false)
public class GeneratorGameTests {
    static final String TEMPLATE = TeslaGameTests.TEMPLATE;

    @GameTest(template = TEMPLATE)
    public static void thermopileNeedsHotAndCold(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos, RIRegistries.THERMOPILE.get());
        helper.setBlock(pos.north(), Blocks.ICE);
        helper.setBlock(pos.south(), Blocks.MAGMA_BLOCK); // not a heat source: no output yet
        helper.setBlock(pos.east(), RIRegistries.TESLA.get());
        TeslaBlockEntity sink = helper.getBlockEntity(pos.east());
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(sink.getCharge() == 0, "thermopile made power with only a cold side");
            helper.setBlock(pos.south(), Blocks.LAVA);
        });
        // ice (2 cold) + lava (2 hot): 15 * 3 = 45 FE/t
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(sink.getCharge() >= 45 * 15, "thermopile delivered only " + sink.getCharge() + " FE");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void transformerPassesPowerOneWay(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, RIRegistries.TRANSFORMER.get().defaultBlockState()
                .setValue(resonantinduction.transformer.TransformerBlock.FACING, net.minecraft.core.Direction.WEST));
        helper.setBlock(pos.east(), RIRegistries.TESLA.get());
        TeslaBlockEntity sink = helper.getBlockEntity(pos.east());
        var level = helper.getLevel();
        var in = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos), net.minecraft.core.Direction.WEST);
        helper.assertTrue(in != null && in.receiveEnergy(700, false) == 700 && sink.getCharge() == 700, "transformer did not pass 700 FE through");
        helper.assertTrue(level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos),
                net.minecraft.core.Direction.EAST) == null, "the output side should not take power");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void solarPanelWorksInDaylight(GameTestHelper helper) {
        helper.getLevel().setDayTime(6000);
        helper.getLevel().setWeatherParameters(6000, 0, false, false);
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, RIRegistries.SOLAR_PANEL.get());
        helper.setBlock(pos.east(), RIRegistries.TESLA.get());
        TeslaBlockEntity sink = helper.getBlockEntity(pos.east());
        helper.succeedWhen(() -> helper.assertTrue(sink.getCharge() >= 500, "solar panel delivered " + sink.getCharge() + " FE"));
    }
}
