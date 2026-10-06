package resonantinduction.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import resonantinduction.ResonantInduction;
import resonantinduction.fluid.GrateBlock;
import resonantinduction.fluid.GrateBlockEntity;
import resonantinduction.fluid.GutterBlockEntity;
import resonantinduction.fluid.PipeBlock;
import resonantinduction.fluid.PipeBlockEntity;
import resonantinduction.fluid.PipeMaterial;
import resonantinduction.fluid.PumpBlockEntity;
import resonantinduction.fluid.TankBlockEntity;
import resonantinduction.mechanical.gear.GearBlock;
import resonantinduction.mechanical.process.MachineBlock;
import resonantinduction.registry.RIRegistries;

@GameTestHolder(ResonantInduction.MODID)
@PrefixGameTestTemplate(false)
public class FluidGameTests {
    static final String TEMPLATE = TeslaGameTests.TEMPLATE;

    static IFluidHandler handler(GameTestHelper helper, BlockPos pos, Direction side) {
        IFluidHandler h = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), side);
        helper.assertTrue(h != null, "no fluid handler at " + pos);
        return h;
    }

    static FluidStack water(int amount) {
        return new FluidStack(Fluids.WATER, amount);
    }

    static int amount(GameTestHelper helper, BlockPos pos) {
        if (helper.getBlockEntity(pos) instanceof TankBlockEntity tank) {
            return tank.tank().getFluidAmount();
        }
        if (helper.getBlockEntity(pos) instanceof GutterBlockEntity gutter) {
            return gutter.tank().getFluidAmount();
        }
        return ((PipeBlockEntity) helper.getBlockEntity(pos)).tank().getFluidAmount();
    }

    static void pipe(GameTestHelper helper, BlockPos pos, PipeMaterial material) {
        helper.setBlock(pos, RIRegistries.PIPES.get(material).get());
    }

    // ---- gutters ----

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void gutterDrinksTheWaterOnIt(GameTestHelper helper) {
        BlockPos gutter = new BlockPos(3, 1, 3);
        helper.setBlock(gutter, RIRegistries.GUTTER.get());
        SmeltingGameTests.wallIn(helper, gutter.above(), null);
        helper.setBlock(gutter.above(), Blocks.WATER);
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(gutter.above()).getFluidState().isEmpty(), "water still on the gutter");
            helper.assertTrue(amount(helper, gutter) == 1000, "gutter holds " + amount(helper, gutter));
        });
    }

    /** Pressure 2 down against a plain tank: 40 mB a tick. */
    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void gutterPoursIntoTheTankBelow(GameTestHelper helper) {
        BlockPos tank = new BlockPos(3, 1, 3);
        helper.setBlock(tank, RIRegistries.TANK.get());
        helper.setBlock(tank.above(), RIRegistries.GUTTER.get());
        handler(helper, tank.above(), Direction.NORTH).fill(water(1000), IFluidHandler.FluidAction.EXECUTE);
        helper.succeedWhen(() -> {
            helper.assertTrue(amount(helper, tank) == 1000, "tank holds " + amount(helper, tank));
            helper.assertTrue(amount(helper, tank.above()) == 0, "gutter still holds " + amount(helper, tank.above()));
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void gutterDrawsFromTheTankAbove(GameTestHelper helper) {
        BlockPos gutter = new BlockPos(3, 1, 3);
        helper.setBlock(gutter, RIRegistries.GUTTER.get());
        helper.setBlock(gutter.above(), RIRegistries.TANK.get());
        handler(helper, gutter.above(), Direction.NORTH).fill(water(600), IFluidHandler.FluidAction.EXECUTE);
        helper.succeedWhen(() -> helper.assertTrue(amount(helper, gutter) == 600 && amount(helper, gutter.above()) == 0,
                "gutter " + amount(helper, gutter) + ", tank " + amount(helper, gutter.above())));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void guttersLevelOutSideways(GameTestHelper helper) {
        BlockPos a = new BlockPos(2, 1, 3);
        BlockPos b = a.east();
        BlockPos c = b.east();
        for (BlockPos p : new BlockPos[] {a, b, c}) {
            helper.setBlock(p, RIRegistries.GUTTER.get());
        }
        helper.assertBlockProperty(a, net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST, true);
        handler(helper, a, Direction.NORTH).fill(water(900), IFluidHandler.FluidAction.EXECUTE);
        helper.succeedWhen(() -> {
            int x = amount(helper, a), y = amount(helper, b), z = amount(helper, c);
            helper.assertTrue(x + y + z == 900, "water lost: " + (x + y + z));
            helper.assertTrue(Math.abs(x - 300) <= 2 && Math.abs(y - 300) <= 2 && Math.abs(z - 300) <= 2, "not level: " + x + ", " + y + ", " + z);
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void gutterTakesNothingThroughItsTop(GameTestHelper helper) {
        BlockPos gutter = new BlockPos(3, 1, 3);
        helper.setBlock(gutter, RIRegistries.GUTTER.get());
        helper.assertTrue(handler(helper, gutter, Direction.UP).fill(water(100), IFluidHandler.FluidAction.EXECUTE) == 0, "filled through the top");
        helper.succeed();
    }

    // ---- tanks ----

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void tanksShareFluidAndFillFromTheBottom(GameTestHelper helper) {
        BlockPos low1 = new BlockPos(2, 1, 3);
        BlockPos low2 = low1.east();
        BlockPos high = low1.above();
        for (BlockPos p : new BlockPos[] {low1, low2, high}) {
            helper.setBlock(p, RIRegistries.TANK.get());
        }
        IFluidHandler tanks = handler(helper, high, Direction.UP);
        helper.assertTrue(tanks.getTankCapacity(0) == 3 * TankBlockEntity.CAPACITY, "joined capacity " + tanks.getTankCapacity(0));
        helper.assertTrue(tanks.fill(water(40000), IFluidHandler.FluidAction.EXECUTE) == 40000, "did not take 40 buckets");
        helper.assertTrue(amount(helper, low1) == 16000 && amount(helper, low2) == 16000 && amount(helper, high) == 8000,
                "filled " + amount(helper, low1) + ", " + amount(helper, low2) + ", " + amount(helper, high));
        helper.assertTrue(tanks.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "mixed lava in");
        // Draining comes off the top.
        helper.assertTrue(tanks.drain(10000, IFluidHandler.FluidAction.EXECUTE).getAmount() == 10000, "did not drain 10 buckets");
        helper.assertTrue(amount(helper, high) == 0 && amount(helper, low1) == 15000 && amount(helper, low2) == 15000,
                "after draining " + amount(helper, low1) + ", " + amount(helper, low2) + ", " + amount(helper, high));
        // 30 of 48 buckets: comparator 9.
        int signal = helper.getBlockState(low1).getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(low1));
        helper.assertTrue(signal == 9, "comparator reads " + signal);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void brokenTankKeepsItsFluid(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, RIRegistries.TANK.get());
        handler(helper, pos, Direction.UP).fill(water(5000), IFluidHandler.FluidAction.EXECUTE);
        helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
        helper.succeedWhen(() -> {
            var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(pos)).inflate(2));
            helper.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(RIRegistries.TANK_ITEM.get())
                    && e.getItem().getOrDefault(RIRegistries.FLUID_CONTENT.get(), SimpleFluidContent.EMPTY).getAmount() == 5000), "no tank item holding 5 buckets");
        });
    }

    // ---- pipes and the pump ----

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void pipesJoinOnlyTheirOwnKind(GameTestHelper helper) {
        BlockPos a = new BlockPos(1, 1, 3);
        pipe(helper, a, PipeMaterial.IRON);
        pipe(helper, a.east(), PipeMaterial.IRON);
        pipe(helper, a.east(2), PipeMaterial.CERAMIC);
        BlockPos d = new BlockPos(1, 1, 5);
        pipe(helper, d, PipeMaterial.IRON);
        pipe(helper, d.east(), PipeMaterial.IRON);
        pipe(helper, d.east(2), PipeMaterial.IRON);
        ((PipeBlockEntity) helper.getBlockEntity(d)).setColor(DyeColor.RED);
        ((PipeBlockEntity) helper.getBlockEntity(d.east(2))).setColor(DyeColor.BLUE);
        helper.runAfterDelay(10, () -> {
            helper.assertBlockProperty(a, PipeBlock.SIDES.get(Direction.EAST), true);
            helper.assertBlockProperty(a.east(), PipeBlock.SIDES.get(Direction.EAST), false);
            // Red joins undyed, undyed joins blue, red and blue would not.
            helper.assertBlockProperty(d, PipeBlock.SIDES.get(Direction.EAST), true);
            helper.assertBlockProperty(d.east(), PipeBlock.SIDES.get(Direction.EAST), true);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void dyedPipesKeepApart(GameTestHelper helper) {
        BlockPos a = new BlockPos(2, 1, 3);
        pipe(helper, a, PipeMaterial.IRON);
        pipe(helper, a.east(), PipeMaterial.IRON);
        ((PipeBlockEntity) helper.getBlockEntity(a)).setColor(DyeColor.RED);
        ((PipeBlockEntity) helper.getBlockEntity(a.east())).setColor(DyeColor.BLUE);
        helper.runAfterDelay(10, () -> {
            helper.assertBlockProperty(a, PipeBlock.SIDES.get(Direction.EAST), false);
            helper.assertBlockProperty(a.east(), PipeBlock.SIDES.get(Direction.WEST), false);
            helper.succeed();
        });
    }

    /**
     * Tank, pump (turned by a creative gear), pipe, tank: the pump pulls from the first and pushes down the pipe. The creative gear
     * gives the pump pressure 2 and each pipe loses one, as in the original, so it reaches through one pipe.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void pumpMovesWaterDownAPipe(GameTestHelper helper) {
        BlockPos source = new BlockPos(1, 1, 3);
        BlockPos pump = source.east();
        BlockPos target = source.east(3);
        helper.setBlock(source, RIRegistries.TANK.get());
        helper.setBlock(pump, RIRegistries.PUMP.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        MechanicalGameTests.gear(helper, pump.above(), GearBlock.CREATIVE, Direction.DOWN);
        pipe(helper, pump.east(), PipeMaterial.IRON);
        helper.setBlock(target, RIRegistries.TANK.get());
        handler(helper, source, Direction.UP).fill(water(4000), IFluidHandler.FluidAction.EXECUTE);
        helper.succeedWhen(() -> {
            helper.assertTrue(amount(helper, source) == 0, "source still holds " + amount(helper, source));
            helper.assertTrue(amount(helper, target) == 4000, "target holds " + amount(helper, target));
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void idlePumpMovesNothing(GameTestHelper helper) {
        BlockPos source = new BlockPos(1, 1, 3);
        BlockPos pump = source.east();
        helper.setBlock(source, RIRegistries.TANK.get());
        helper.setBlock(pump, RIRegistries.PUMP.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        helper.setBlock(pump.east(), RIRegistries.TANK.get());
        handler(helper, source, Direction.UP).fill(water(4000), IFluidHandler.FluidAction.EXECUTE);
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(amount(helper, source) == 4000 && amount(helper, pump.east()) == 0, "an unturned pump moved water");
            helper.succeed();
        });
    }

    /** Pressure falls by one at each pipe away from the pump (driven hard here, for a high pressure). */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void pressureFallsAlongThePipes(GameTestHelper helper) {
        BlockPos pump = new BlockPos(1, 1, 3);
        helper.setBlock(pump, RIRegistries.PUMP.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        PumpBlockEntity be = helper.getBlockEntity(pump);
        helper.onEachTick(() -> {
            be.node().torque = 84000;
            be.node().angularVelocity = 1;
        });
        for (int i = 1; i <= 3; i++) {
            pipe(helper, pump.east(i), PipeMaterial.STEEL);
        }
        helper.succeedWhen(() -> {
            int[] p = new int[3];
            for (int i = 0; i < 3; i++) {
                p[i] = ((PipeBlockEntity) helper.getBlockEntity(pump.east(i + 1))).node().getPressure(Direction.UP);
            }
            int source = be.pressureNode().getPressure(Direction.EAST);
            helper.assertTrue(source > 4, "pump pressure only " + source);
            helper.assertTrue(p[0] == source - 1 && p[1] == source - 2 && p[2] == source - 3,
                    "pump " + source + ", pipes " + p[0] + ", " + p[1] + ", " + p[2]);
        });
    }

    // ---- grates ----

    /** Pumped into, a grate facing up pours source blocks out on top of itself. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void pressurisedGrateFillsTheWorld(GameTestHelper helper) {
        BlockPos source = new BlockPos(1, 1, 3);
        BlockPos pump = source.east();
        BlockPos grate = pump.east();
        helper.setBlock(source, RIRegistries.TANK.get());
        helper.setBlock(pump, RIRegistries.PUMP.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        // On its side: the glass round the grate's face takes the space above the pump.
        MechanicalGameTests.gear(helper, pump.north(), GearBlock.CREATIVE, Direction.SOUTH);
        helper.setBlock(grate, RIRegistries.GRATE.get().defaultBlockState().setValue(GrateBlock.FACING, Direction.UP));
        SmeltingGameTests.wallIn(helper, grate.above(), null);
        handler(helper, source, Direction.UP).fill(water(2000), IFluidHandler.FluidAction.EXECUTE);
        helper.succeedWhen(() -> helper.assertTrue(helper.getBlockState(grate.above()).getFluidState().isSourceOfType(Fluids.WATER),
                "no water poured out of the grate: " + ((GrateBlockEntity) helper.getBlockEntity(grate)).debug()));
    }

    /** Pumped out of, a grate drains the water in front of it, and the pump sends it on to a tank. */
    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void suctionGrateDrainsTheWorld(GameTestHelper helper) {
        BlockPos grate = new BlockPos(1, 1, 3);
        BlockPos pump = grate.east();
        BlockPos target = pump.east();
        helper.setBlock(grate, RIRegistries.GRATE.get().defaultBlockState().setValue(GrateBlock.FACING, Direction.UP));
        helper.setBlock(pump, RIRegistries.PUMP.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        // On its side: the glass round the grate's face takes the space above the pump.
        MechanicalGameTests.gear(helper, pump.north(), GearBlock.CREATIVE, Direction.SOUTH);
        helper.setBlock(target, RIRegistries.TANK.get());
        SmeltingGameTests.wallIn(helper, grate.above(), null);
        helper.setBlock(grate.above(), Blocks.WATER);
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(grate.above()).getFluidState().isEmpty(),
                    "water still in front of the grate: " + ((GrateBlockEntity) helper.getBlockEntity(grate)).debug());
            helper.assertTrue(amount(helper, target) == 1000, "tank holds " + amount(helper, target));
        });
    }
}
