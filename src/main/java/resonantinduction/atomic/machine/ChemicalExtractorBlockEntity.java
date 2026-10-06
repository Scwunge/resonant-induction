package resonantinduction.atomic.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import resonantinduction.RIConfig;
import resonantinduction.registry.RIRegistries;
import resonantinduction.resource.Materials;

import java.util.List;

/**
 * Chemical Extractor, as the original: with a bucket of water it extracts uranium ore into three yellowcake; otherwise it extracts
 * water into deuterium, and deuterium into tritium, 100 mB at a time. 14 seconds a job.
 */
public class ChemicalExtractorBlockEntity extends AtomicMachineBlockEntity {
    public static final int TICK_TIME = 20 * 14;
    public static final int EXTRACT_SPEED = 100;
    public static final int INPUT = 1, OUTPUT = 2, IN_FILL = 3, IN_EMPTY = 4, OUT_FILL = 5, OUT_FULL = 6;

    private final FluidTank inputTank = new FluidTank(10000, fs -> fs.getFluid().isSame(Fluids.WATER) || fs.getFluid().isSame(RIRegistries.DEUTERIUM.get()));
    private final FluidTank outputTank = new FluidTank(10000, fs -> fs.getFluid().isSame(RIRegistries.DEUTERIUM.get()) || fs.getFluid().isSame(RIRegistries.TRITIUM.get()));

    public ChemicalExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.CHEMICAL_EXTRACTOR_BE.get(), pos, state, 7);
    }

    public FluidTank inputTank() {
        return inputTank;
    }

    public FluidTank outputTank() {
        return outputTank;
    }

    @Override
    protected long baseUse() {
        return 5000;
    }

    @Override
    public int jobTime() {
        return TICK_TIME;
    }

    @Override
    public List<FluidTank> tanks() {
        return List.of(inputTank, outputTank);
    }

    private static boolean isUraniumOre(ItemStack stack) {
        return stack.is(Materials.oreItemTag("uranium"));
    }

    private boolean canRefineUranium() {
        return inputTank.getFluid().getFluid().isSame(Fluids.WATER) && inputTank.getFluidAmount() >= 1000 && isUraniumOre(inventory.getStackInSlot(INPUT))
                && hasRoomFor(OUTPUT, new ItemStack(RIRegistries.YELLOWCAKE.get(), 3));
    }

    private boolean canExtract(net.minecraft.world.level.material.Fluid from, int ratio, net.minecraft.world.level.material.Fluid to) {
        return inputTank.getFluid().getFluid().isSame(from) && inputTank.getFluidAmount() >= ratio * EXTRACT_SPEED
                && outputTank.getFluidAmount() + EXTRACT_SPEED <= outputTank.getCapacity() && (outputTank.isEmpty() || outputTank.getFluid().getFluid().isSame(to));
    }

    @Override
    protected boolean canWork() {
        return canRefineUranium() || canExtract(RIRegistries.DEUTERIUM.get(), RIConfig.get(RIConfig.DEUTERIUM_PER_TRITIUM), RIRegistries.TRITIUM.get())
                || canExtract(Fluids.WATER, RIConfig.get(RIConfig.WATER_PER_DEUTERIUM), RIRegistries.DEUTERIUM.get());
    }

    @Override
    protected void finishJob() {
        if (canRefineUranium()) {
            inputTank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
            output(OUTPUT, new ItemStack(RIRegistries.YELLOWCAKE.get(), 3));
            inventory.extractItem(INPUT, 1, false);
        } else if (canExtract(RIRegistries.DEUTERIUM.get(), RIConfig.get(RIConfig.DEUTERIUM_PER_TRITIUM), RIRegistries.TRITIUM.get())) {
            inputTank.drain(RIConfig.get(RIConfig.DEUTERIUM_PER_TRITIUM) * EXTRACT_SPEED, IFluidHandler.FluidAction.EXECUTE);
            outputTank.fill(new FluidStack(RIRegistries.TRITIUM.get(), EXTRACT_SPEED), IFluidHandler.FluidAction.EXECUTE);
        } else if (canExtract(Fluids.WATER, RIConfig.get(RIConfig.WATER_PER_DEUTERIUM), RIRegistries.DEUTERIUM.get())) {
            inputTank.drain(RIConfig.get(RIConfig.WATER_PER_DEUTERIUM) * EXTRACT_SPEED, IFluidHandler.FluidAction.EXECUTE);
            outputTank.fill(new FluidStack(RIRegistries.DEUTERIUM.get(), EXTRACT_SPEED), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    @Override
    protected void handleContainers() {
        fillOrDrain(IN_FILL, IN_EMPTY, inputTank, false);
        fillOrDrain(OUT_FILL, OUT_FULL, outputTank, true);
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT -> isUraniumOre(stack);
            case IN_FILL, OUT_FILL -> stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM) != null;
            default -> true;
        };
    }

    @Override
    public MachineLayout layout() {
        return new MachineLayout(210,
                List.of(MachineLayout.battery(79, 49), MachineLayout.slot(INPUT, 52, 24), MachineLayout.output(OUTPUT, 106, 24),
                        MachineLayout.fluid(IN_FILL, 24, 18), MachineLayout.output(IN_EMPTY, 24, 49), MachineLayout.fluid(OUT_FILL, 134, 18),
                        MachineLayout.output(OUT_FULL, 134, 49)),
                List.of(new MachineLayout.GaugeAt(0, 8, 18), new MachineLayout.GaugeAt(1, 154, 18)), 75, 24, 8, 106,
                List.of(new MachineLayout.Line(Component.translatable("gui.resonantinduction.chemical_extractor.1"), 8, 75),
                        new MachineLayout.Line(Component.translatable("gui.resonantinduction.chemical_extractor.2"), 8, 85),
                        new MachineLayout.Line(Component.translatable("gui.resonantinduction.chemical_extractor.3"), 8, 95)));
    }

    /** Water or deuterium in; deuterium or tritium out. */
    @Nullable
    public IFluidHandler getFluidCapability(@Nullable Direction side) {
        return new TwoTankHandler(inputTank, outputTank, this::setChanged);
    }

    /** Ore and fluid containers go in; yellowcake and filled or emptied containers come out. */
    public IItemHandler getItemCapability(@Nullable Direction side) {
        return new SidedSlots(inventory, new int[] {INPUT, IN_FILL, OUT_FILL}, new int[] {OUTPUT, IN_EMPTY, OUT_FULL});
    }
}
