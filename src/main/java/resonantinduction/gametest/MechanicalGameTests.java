package resonantinduction.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import resonantinduction.ResonantInduction;
import resonantinduction.mechanical.MechanicalBlockEntity;
import resonantinduction.mechanical.gear.GearBlock;
import resonantinduction.mechanical.gear.GearBlockEntity;
import resonantinduction.registry.RIRegistries;

@GameTestHolder(ResonantInduction.MODID)
@PrefixGameTestTemplate(false)
public class MechanicalGameTests {
    static final String TEMPLATE = TeslaGameTests.TEMPLATE;

    static void gear(GameTestHelper helper, BlockPos pos, int tier, Direction attach) {
        helper.setBlock(pos, RIRegistries.GEARS.get(tier).get().defaultBlockState().setValue(GearBlock.ATTACH, attach));
    }

    static double speed(GameTestHelper helper, BlockPos pos) {
        return ((MechanicalBlockEntity) helper.getBlockEntity(pos)).node().getAngularVelocity();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void meshingGearsTurnOppositeWays(GameTestHelper helper) {
        BlockPos start = new BlockPos(2, 1, 3);
        gear(helper, start, GearBlock.CREATIVE, Direction.DOWN);
        for (int i = 1; i <= 3; i++) {
            gear(helper, start.east(i), 2, Direction.DOWN);
        }
        helper.succeedWhen(() -> {
            double a = speed(helper, start.east(1));
            double b = speed(helper, start.east(2));
            double c = speed(helper, start.east(3));
            helper.assertTrue(a < -1 && b > 1 && c < -1, "gear speeds " + a + ", " + b + ", " + c + " should alternate in sign");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void shaftCarriesRotationToAnotherGear(GameTestHelper helper) {
        BlockPos bottom = new BlockPos(3, 1, 3);
        gear(helper, bottom, GearBlock.CREATIVE, Direction.DOWN);
        for (int i = 1; i <= 3; i++) {
            helper.setBlock(bottom.above(i), RIRegistries.SHAFTS.get(2).get().defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
        }
        BlockPos top = bottom.above(4);
        gear(helper, top, 2, Direction.UP);
        helper.succeedWhen(() -> helper.assertTrue(Math.abs(speed(helper, top)) > 1, "gear at the top of the shaft turns at " + speed(helper, top)));
    }

    /** A cranked gear spins up while cranked, then coasts down (the original's slow exponential losses). */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void handCrankTurnsAGearThatCoastsDown(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        gear(helper, pos, 0, Direction.DOWN);
        GearBlockEntity gear = helper.getBlockEntity(pos);
        gear.crank(true);
        double[] peak = new double[1];
        helper.runAfterDelay(20, () -> {
            peak[0] = speed(helper, pos);
            helper.assertTrue(peak[0] > 0.3, "cranked gear turns at only " + peak[0]);
        });
        helper.runAfterDelay(150, () -> {
            double now = speed(helper, pos);
            helper.assertTrue(now > 0 && now < peak[0], "gear should be slowing: " + peak[0] + " -> " + now);
            helper.succeed();
        });
    }
}
