package resonantinduction.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import resonantinduction.ResonantInduction;
import resonantinduction.atomic.machine.AtomicMachineBlockEntity;
import resonantinduction.atomic.machine.CentrifugeBlockEntity;
import resonantinduction.atomic.machine.ChemicalExtractorBlockEntity;
import resonantinduction.atomic.machine.NuclearBoilerBlockEntity;
import resonantinduction.battery.BatteryItem;
import resonantinduction.registry.RIRegistries;

@GameTestHolder(ResonantInduction.MODID)
@PrefixGameTestTemplate(false)
public class AtomicMachineGameTests {
    static final String TEMPLATE = TeslaGameTests.TEMPLATE;

    /** A full top-tier battery: plenty for any of these. */
    static ItemStack battery() {
        ItemStack battery = new ItemStack(RIRegistries.BATTERY_ITEM.get());
        battery.set(RIRegistries.BATTERY_TIER.get(), 2);
        battery.set(RIRegistries.ENERGY.get(), BatteryItem.capacity(battery));
        return battery;
    }

    static <T extends AtomicMachineBlockEntity> T machine(GameTestHelper helper, BlockPos pos, net.minecraft.world.level.block.Block block) {
        helper.setBlock(pos, block);
        T be = helper.getBlockEntity(pos);
        be.inventory().setStackInSlot(AtomicMachineBlockEntity.BATTERY_SLOT, battery());
        return be;
    }

    static IFluidHandler fluids(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), Direction.NORTH);
    }

    /** 14 seconds: a bucket of water and an ore make three yellowcake. */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void extractorRefinesUraniumIntoYellowcake(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        ChemicalExtractorBlockEntity be = machine(helper, pos, RIRegistries.CHEMICAL_EXTRACTOR.get());
        helper.assertTrue(fluids(helper, pos).fill(new FluidStack(Fluids.WATER, 2000), IFluidHandler.FluidAction.EXECUTE) == 2000, "did not take water");
        be.inventory().setStackInSlot(ChemicalExtractorBlockEntity.INPUT, new ItemStack(RIRegistries.URANIUM_ORE.get(), 2));
        helper.runAfterDelay(200, () -> helper.assertTrue(be.inventory().getStackInSlot(ChemicalExtractorBlockEntity.OUTPUT).isEmpty(), "done in under 14 s"));
        helper.succeedWhen(() -> {
            ItemStack out = be.inventory().getStackInSlot(ChemicalExtractorBlockEntity.OUTPUT);
            helper.assertTrue(out.is(RIRegistries.YELLOWCAKE.get()) && out.getCount() == 3, "made " + out);
            helper.assertTrue(be.inputTank().getFluidAmount() == 1000, "water left " + be.inputTank().getFluidAmount());
            helper.assertTrue(be.inventory().getStackInSlot(ChemicalExtractorBlockEntity.INPUT).getCount() == 1, "ore not used");
        });
    }

    /** Without ore, four parts water make one part deuterium, 100 mB a job. */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void extractorExtractsDeuteriumFromWater(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        ChemicalExtractorBlockEntity be = machine(helper, pos, RIRegistries.CHEMICAL_EXTRACTOR.get());
        fluids(helper, pos).fill(new FluidStack(Fluids.WATER, 600), IFluidHandler.FluidAction.EXECUTE);
        helper.succeedWhen(() -> {
            helper.assertTrue(be.outputTank().getFluid().getFluid().isSame(RIRegistries.DEUTERIUM.get()) && be.outputTank().getFluidAmount() == 100,
                    "output " + be.outputTank().getFluidAmount());
            helper.assertTrue(be.inputTank().getFluidAmount() == 200, "water left " + be.inputTank().getFluidAmount());
            // Then it stops: 200 mB is too little for another job, and an idle machine takes no power.
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos), Direction.UP).receiveEnergy(1000, true) == 0,
                    "an idle extractor takes power");
        });
    }

    /** Draining its deuterium into an empty cell in the product slot fills a deuterium cell. */
    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void cellsFillAndEmpty(GameTestHelper helper) {
        IFluidHandlerItem cell = new ItemStack(RIRegistries.EMPTY_CELL.get()).getCapability(Capabilities.FluidHandler.ITEM);
        helper.assertTrue(cell.fill(new FluidStack(RIRegistries.DEUTERIUM.get(), 500), IFluidHandler.FluidAction.EXECUTE) == 200, "cell took the wrong amount");
        helper.assertTrue(cell.getContainer().is(RIRegistries.DEUTERIUM_CELL.get()), "not a deuterium cell: " + cell.getContainer());
        IFluidHandlerItem water = new ItemStack(RIRegistries.WATER_CELL.get()).getCapability(Capabilities.FluidHandler.ITEM);
        FluidStack out = water.drain(1000, IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(out.getFluid().isSame(Fluids.WATER) && out.getAmount() == 1000 && water.getContainer().is(RIRegistries.EMPTY_CELL.get()), "water cell did not empty");
        // In the extractor: water cells feed the input tank; empty cells take the product.
        BlockPos pos = new BlockPos(3, 1, 3);
        ChemicalExtractorBlockEntity be = machine(helper, pos, RIRegistries.CHEMICAL_EXTRACTOR.get());
        be.inventory().setStackInSlot(ChemicalExtractorBlockEntity.IN_FILL, new ItemStack(RIRegistries.WATER_CELL.get(), 2));
        be.outputTank().fill(new FluidStack(RIRegistries.DEUTERIUM.get(), 400), IFluidHandler.FluidAction.EXECUTE);
        be.inventory().setStackInSlot(ChemicalExtractorBlockEntity.OUT_FILL, new ItemStack(RIRegistries.EMPTY_CELL.get(), 2));
        helper.succeedWhen(() -> {
            helper.assertTrue(be.inputTank().getFluidAmount() == 2000, "water from cells " + be.inputTank().getFluidAmount());
            helper.assertTrue(be.inventory().getStackInSlot(ChemicalExtractorBlockEntity.IN_EMPTY).is(RIRegistries.EMPTY_CELL.get()), "no empty cells back");
            ItemStack filled = be.inventory().getStackInSlot(ChemicalExtractorBlockEntity.OUT_FULL);
            helper.assertTrue(filled.is(RIRegistries.DEUTERIUM_CELL.get()) && filled.getCount() == 2, "filled " + filled);
        });
    }

    /** 15 seconds: water and yellowcake boil into 400 mB of hexafluoride. */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void boilerBoilsYellowcakeIntoHexafluoride(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        NuclearBoilerBlockEntity be = machine(helper, pos, RIRegistries.NUCLEAR_BOILER.get());
        fluids(helper, pos).fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        be.inventory().setStackInSlot(NuclearBoilerBlockEntity.INPUT, new ItemStack(RIRegistries.YELLOWCAKE.get()));
        helper.succeedWhen(() -> {
            helper.assertTrue(be.gasTank().getFluidAmount() == 400, "hexafluoride " + be.gasTank().getFluidAmount());
            helper.assertTrue(be.waterTank().isEmpty() && be.inventory().getStackInSlot(NuclearBoilerBlockEntity.INPUT).isEmpty(), "inputs not used");
            FluidStack drained = fluids(helper, pos).drain(1000, IFluidHandler.FluidAction.SIMULATE);
            helper.assertTrue(drained.getFluid().isSame(RIRegistries.URANIUM_HEXAFLUORIDE.get()) && drained.getAmount() == 400, "gas can't be piped out");
        });
    }

    /** A boiler beside a centrifuge: the centrifuge draws the gas over and spins it into uranium (a minute a job). */
    @GameTest(template = TEMPLATE, timeoutTicks = 1800)
    public static void centrifugeSpinsTheBoilersGasIntoUranium(GameTestHelper helper) {
        BlockPos boilerPos = new BlockPos(2, 1, 3);
        NuclearBoilerBlockEntity boiler = machine(helper, boilerPos, RIRegistries.NUCLEAR_BOILER.get());
        CentrifugeBlockEntity centrifuge = machine(helper, boilerPos.east(), RIRegistries.CENTRIFUGE.get());
        fluids(helper, boilerPos).fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        boiler.inventory().setStackInSlot(NuclearBoilerBlockEntity.INPUT, new ItemStack(RIRegistries.YELLOWCAKE.get()));
        helper.succeedWhen(() -> {
            int u235 = centrifuge.inventory().getStackInSlot(CentrifugeBlockEntity.URANIUM_235).getCount();
            int u238 = centrifuge.inventory().getStackInSlot(CentrifugeBlockEntity.URANIUM_238).getCount();
            helper.assertTrue(u235 + u238 == 1, "uranium made: " + u235 + " + " + u238);
            helper.assertTrue(boiler.gasTank().isEmpty() && centrifuge.gasTank().getFluidAmount() == 200, "gas: boiler " + boiler.gasTank().getFluidAmount()
                    + ", centrifuge " + centrifuge.gasTank().getFluidAmount());
        });
    }
}
