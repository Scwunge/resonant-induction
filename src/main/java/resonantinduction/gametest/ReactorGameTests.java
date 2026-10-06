package resonantinduction.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import resonantinduction.ResonantInduction;
import resonantinduction.atomic.ThermalGrid;
import resonantinduction.atomic.reactor.ElectricTurbineBlockEntity;
import resonantinduction.atomic.reactor.ReactorCellBlockEntity;
import resonantinduction.atomic.reactor.ThermometerBlock;
import resonantinduction.registry.RIRegistries;

@GameTestHolder(ResonantInduction.MODID)
@PrefixGameTestTemplate(false)
public class ReactorGameTests {
    static final String TEMPLATE = TeslaGameTests.TEMPLATE;

    static ReactorCellBlockEntity cell(GameTestHelper helper, BlockPos pos, boolean fuel) {
        helper.setBlock(pos, RIRegistries.REACTOR_CELL.get());
        ReactorCellBlockEntity cell = helper.getBlockEntity(pos);
        if (fuel) {
            cell.inventory().setStackInSlot(0, new ItemStack(RIRegistries.FISSILE_FUEL_ROD.get()));
        }
        return cell;
    }

    static float temperature(GameTestHelper helper, BlockPos pos) {
        return ThermalGrid.temperature(helper.getLevel(), helper.absolutePos(pos));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void fuelHeatsTheCellAndWears(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        ReactorCellBlockEntity cell = cell(helper, pos, true);
        helper.succeedWhen(() -> {
            helper.assertTrue(temperature(helper, pos) > 600, "only " + temperature(helper, pos) + " K");
            helper.assertTrue(cell.inventory().getStackInSlot(0).getDamageValue() >= 2, "fuel rod not burning");
            helper.assertTrue(cell.tank().getFluid().getFluid().isSame(RIRegistries.TOXIC_WASTE.get()) && cell.tank().getFluidAmount() > 5, "no toxic waste");
        });
    }

    /** Four control rods round a cell take off a tenth each: it runs cooler than a bare one. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void controlRodsSlowTheReaction(GameTestHelper helper) {
        BlockPos bare = new BlockPos(1, 1, 1);
        BlockPos rodded = new BlockPos(5, 1, 5);
        cell(helper, bare, true);
        cell(helper, rodded, true);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            helper.setBlock(rodded.relative(d), RIRegistries.CONTROL_ROD.get());
        }
        helper.runAfterDelay(60, () -> {
            float a = temperature(helper, bare);
            float b = temperature(helper, rodded);
            helper.assertTrue(b < a * 0.95f, "rods did nothing: bare " + a + " K, with rods " + b + " K");
            helper.succeed();
        });
    }

    /** Water round a hot cell boils; the steam rises into turbines above the water, which make power. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void reactorSteamTurnsTurbines(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        cell(helper, pos, true);
        BlockPos water = pos.east();
        // Walled in on the three sides away from the cell.
        SmeltingGameTests.wallIn(helper, water, Direction.WEST);
        helper.setBlock(water, Blocks.WATER);
        helper.setBlock(water.above(), RIRegistries.ELECTRIC_TURBINE.get());
        ElectricTurbineBlockEntity turbine = helper.getBlockEntity(water.above());
        helper.succeedWhen(() -> helper.assertTrue(turbine.produced() > 0, "turbine idle at " + temperature(helper, pos) + " K"));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void funnelPassesSteamUp(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, RIRegistries.FUNNEL.get());
        helper.setBlock(pos.above(), RIRegistries.ELECTRIC_TURBINE.get());
        ElectricTurbineBlockEntity turbine = helper.getBlockEntity(pos.above());
        IFluidHandler bottom = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), Direction.DOWN);
        IFluidHandler side = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), Direction.NORTH);
        helper.assertTrue(side.fill(new FluidStack(RIRegistries.STEAM.get(), 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "steam went in at the side");
        helper.assertTrue(bottom.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "took water");
        helper.assertTrue(bottom.fill(new FluidStack(RIRegistries.STEAM.get(), 4000), IFluidHandler.FluidAction.EXECUTE) == 4000, "did not take steam");
        helper.succeedWhen(() -> helper.assertTrue(turbine.produced() > 0, "turbine got no steam"));
    }

    /** A wrench on the middle of a 3x3 joins it into one big turbine; steam put into any part reaches the middle. */
    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void turbinesJoinIntoABigOne(GameTestHelper helper) {
        BlockPos centre = new BlockPos(3, 1, 3);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                helper.setBlock(centre.offset(x, 0, z), RIRegistries.ELECTRIC_TURBINE.get());
            }
        }
        ElectricTurbineBlockEntity middle = helper.getBlockEntity(centre);
        ElectricTurbineBlockEntity corner = helper.getBlockEntity(centre.offset(1, 0, 1));
        helper.assertTrue(middle.toggleMultiblock(), "did not join");
        helper.assertTrue(middle.formed() && middle.area() == 9 && corner.isMember() && corner.master() == middle, "not one turbine");
        IFluidHandler cornerIn = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(centre.offset(1, 0, 1)), Direction.DOWN);
        helper.assertTrue(cornerIn.fill(new FluidStack(RIRegistries.STEAM.get(), 5000), IFluidHandler.FluidAction.EXECUTE) == 5000, "big turbine holds less");
        helper.assertTrue(middle.tank().getFluidAmount() == 5000, "steam not in the middle");
        helper.assertTrue(corner.toggleMultiblock() && !middle.formed() && !corner.isMember(), "did not split");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void thermometerSignalsWhenHot(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, RIRegistries.THERMOMETER.get());
        ThermometerBlock.Tile t = helper.getBlockEntity(pos);
        // It reads its own block when tracking nothing; its warning level starts at 1000 K. Heat it steadily.
        helper.onEachTick(() -> ThermalGrid.addTemperature(helper.getLevel(), helper.absolutePos(pos), 100));
        helper.succeedWhen(() -> {
            helper.assertTrue(t.detected() > 1000, "reads " + t.detected());
            helper.assertTrue(helper.getLevel().getSignal(helper.absolutePos(pos), Direction.NORTH) == 15, "no redstone signal");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void plasmaSpreadsButNotThroughIron(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        for (Direction d : Direction.values()) {
            helper.setBlock(pos.relative(d), Blocks.IRON_BLOCK);
        }
        helper.setBlock(pos.relative(Direction.EAST), Blocks.AIR);
        helper.setBlock(pos.relative(Direction.EAST, 2), Blocks.IRON_BLOCK);
        helper.setBlock(pos, RIRegistries.PLASMA_BLOCK.get());
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(RIRegistries.PLASMA_BLOCK.get(), pos.east());
            for (Direction d : Direction.values()) {
                if (d != Direction.EAST) {
                    helper.assertBlockPresent(Blocks.IRON_BLOCK, pos.relative(d));
                }
            }
            helper.assertTrue(temperature(helper, pos) > 10000, "plasma is cold: " + temperature(helper, pos));
        });
    }
}
