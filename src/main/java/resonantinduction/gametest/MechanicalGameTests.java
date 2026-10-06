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

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void motorTurnsPowerIntoRotation(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, RIRegistries.MOTOR.get().defaultBlockState().setValue(resonantinduction.mechanical.motor.MotorBlock.FACING, Direction.EAST));
        gear(helper, pos.east(), 2, Direction.WEST);
        var in = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos), Direction.NORTH);
        helper.assertTrue(in != null && in.receiveEnergy(500_000, false) == 500_000, "motor did not take power from its side");
        helper.assertTrue(helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos), Direction.EAST) == null,
                "motor should not take power through its shaft faces");
        helper.succeedWhen(() -> helper.assertTrue(Math.abs(speed(helper, pos.east())) > 0.1, "gear on the motor turns at " + speed(helper, pos.east())));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void generatorTurnsRotationIntoPower(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, RIRegistries.MOTOR.get().defaultBlockState().setValue(resonantinduction.mechanical.motor.MotorBlock.FACING, Direction.EAST));
        ((resonantinduction.mechanical.motor.MotorBlockEntity) helper.getBlockEntity(pos)).toggleMode();
        gear(helper, pos.east(), GearBlock.CREATIVE, Direction.WEST);
        helper.setBlock(pos.north(), RIRegistries.TESLA.get());
        resonantinduction.tesla.TeslaBlockEntity sink = helper.getBlockEntity(pos.north());
        helper.succeedWhen(() -> helper.assertTrue(sink.getCharge() > 0, "generator made no power from a creative gear"));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void waterFallingThroughATurbineTurnsIt(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 3, 3);
        helper.setBlock(pos, RIRegistries.TURBINES.get(3).get().defaultBlockState().setValue(resonantinduction.mechanical.turbine.TurbineBlock.FACING, Direction.UP));
        helper.setBlock(pos.above(), net.minecraft.world.level.block.Blocks.WATER);
        helper.succeedWhen(() -> helper.assertTrue(Math.abs(speed(helper, pos)) > 0.1, "water turbine turns at " + speed(helper, pos)));
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
