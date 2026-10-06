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
 * Nuclear Boiler, as the original: a bucket of water and a yellowcake (or uranium ore) boil into uranium hexafluoride gas (twice
 * the hexafluoride ratio, 400 mB by default). 15 seconds a job.
 */
public class NuclearBoilerBlockEntity extends AtomicMachineBlockEntity {
    public static final int TICK_TIME = 20 * 15;
    public static final int WATER_IN = 1, WATER_EMPTY = 2, INPUT = 3;

    private final FluidTank waterTank = new FluidTank(5000, fs -> fs.getFluid().isSame(Fluids.WATER));
    private final FluidTank gasTank = new FluidTank(5000, fs -> fs.getFluid().isSame(RIRegistries.URANIUM_HEXAFLUORIDE.get()));

    public NuclearBoilerBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.NUCLEAR_BOILER_BE.get(), pos, state, 4);
    }

    public FluidTank waterTank() {
        return waterTank;
    }

    public FluidTank gasTank() {
        return gasTank;
    }

    @Override
    protected long baseUse() {
        return 50000;
    }

    @Override
    public int jobTime() {
        return TICK_TIME;
    }

    @Override
    public List<FluidTank> tanks() {
        return List.of(waterTank, gasTank);
    }

    private static boolean isFuel(ItemStack stack) {
        return stack.is(RIRegistries.YELLOWCAKE.get()) || stack.is(Materials.oreItemTag("uranium"));
    }

    @Override
    protected boolean canWork() {
        return waterTank.getFluidAmount() >= 1000 && isFuel(inventory.getStackInSlot(INPUT)) && gasTank.getFluidAmount() < gasTank.getCapacity();
    }

    @Override
    protected void finishJob() {
        waterTank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
        gasTank.fill(new FluidStack(RIRegistries.URANIUM_HEXAFLUORIDE.get(), RIConfig.get(RIConfig.URANIUM_HEXAFLUORIDE_RATIO) * 2), IFluidHandler.FluidAction.EXECUTE);
        inventory.extractItem(INPUT, 1, false);
    }

    @Override
    protected void handleContainers() {
        fillOrDrain(WATER_IN, WATER_EMPTY, waterTank, false);
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT -> isFuel(stack);
            case WATER_IN -> stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM) != null;
            default -> true;
        };
    }

    @Override
    public MachineLayout layout() {
        return new MachineLayout(210,
                List.of(MachineLayout.battery(55, 25), MachineLayout.fluid(WATER_IN, 24, 49), MachineLayout.output(WATER_EMPTY, 135, 49),
                        MachineLayout.slot(INPUT, 80, 25)),
                List.of(new MachineLayout.GaugeAt(0, 8, 18), new MachineLayout.GaugeAt(1, 155, 18)), 110, 26, 8, 106,
                List.of(new MachineLayout.Line(Component.translatable("gui.resonantinduction.nuclear_boiler.1"), 8, 75),
                        new MachineLayout.Line(Component.translatable("gui.resonantinduction.nuclear_boiler.2"), 8, 85),
                        new MachineLayout.Line(Component.translatable("gui.resonantinduction.nuclear_boiler.3"), 8, 95)));
    }

    /** Water in; hexafluoride out. */
    @Nullable
    public IFluidHandler getFluidCapability(@Nullable Direction side) {
        return new TwoTankHandler(waterTank, gasTank, this::setChanged);
    }

    /** From below, emptied containers come out; from elsewhere, water containers and yellowcake go in. */
    public IItemHandler getItemCapability(@Nullable Direction side) {
        return side == Direction.DOWN ? new SidedSlots(inventory, new int[0], new int[] {WATER_EMPTY})
                : new SidedSlots(inventory, new int[] {WATER_IN, INPUT}, new int[] {WATER_EMPTY});
    }
}
