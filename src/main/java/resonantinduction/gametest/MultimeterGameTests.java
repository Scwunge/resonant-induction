package resonantinduction.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import resonantinduction.ResonantInduction;
import resonantinduction.battery.BatteryBlockEntity;
import resonantinduction.multimeter.Measure;
import resonantinduction.multimeter.MultimeterBlock;
import resonantinduction.multimeter.MultimeterBlockEntity;
import resonantinduction.registry.RIRegistries;

@GameTestHolder(ResonantInduction.MODID)
@PrefixGameTestTemplate(false)
public class MultimeterGameTests {
    static final String TEMPLATE = TeslaGameTests.TEMPLATE;

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void readsABatteryAndSignals(GameTestHelper helper) {
        BlockPos batteryPos = new BlockPos(3, 1, 3);
        helper.setBlock(batteryPos, RIRegistries.BATTERY.get());
        BatteryBlockEntity battery = helper.getBlockEntity(batteryPos);
        battery.setEnergy(400_000);
        BlockPos meterPos = batteryPos.east();
        helper.setBlock(meterPos, RIRegistries.MULTIMETER.get().defaultBlockState().setValue(MultimeterBlock.FACING, Direction.EAST));
        MultimeterBlockEntity meter = helper.getBlockEntity(meterPos);
        meter.applySettings(MultimeterBlockEntity.DetectMode.GREATER_THAN, Measure.ENERGY, Measure.ENERGY, 100_000);
        helper.succeedWhen(() -> {
            helper.assertTrue(meter.value(Measure.ENERGY) == 400_000, "multimeter reads " + meter.value(Measure.ENERGY));
            helper.assertTrue(meter.value(Measure.CAPACITY) == 1_000_000, "multimeter capacity " + meter.value(Measure.CAPACITY));
            helper.assertTrue(meter.redstoneOn(), "energy over the limit gave no redstone");
            helper.assertTrue(helper.getLevel().hasNeighborSignal(helper.absolutePos(meterPos.east())), "the block next to the meter gets no redstone");
        });
    }
}
