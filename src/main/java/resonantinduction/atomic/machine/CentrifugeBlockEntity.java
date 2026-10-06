package resonantinduction.atomic.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import resonantinduction.RIConfig;
import resonantinduction.registry.RIRegistries;

import java.util.List;

/**
 * Centrifuge, as the original: spins uranium hexafluoride (the hexafluoride ratio, 200 mB, a job; a minute each) into uranium,
 * 40% of the time enriched Uranium-235, otherwise Uranium-238. Once a second it draws the gas from anything next to it.
 */
public class CentrifugeBlockEntity extends AtomicMachineBlockEntity {
    public static final int TICK_TIME = 20 * 60;
    public static final int GAS_IN = 1, URANIUM_235 = 2, URANIUM_238 = 3;

    private final FluidTank gasTank = new FluidTank(5000, fs -> fs.getFluid().isSame(RIRegistries.URANIUM_HEXAFLUORIDE.get()));

    public CentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.CENTRIFUGE_BE.get(), pos, state, 4);
    }

    public FluidTank gasTank() {
        return gasTank;
    }

    @Override
    protected long baseUse() {
        return 500000;
    }

    @Override
    public int jobTime() {
        return TICK_TIME;
    }

    @Override
    public List<FluidTank> tanks() {
        return List.of(gasTank);
    }

    @Override
    protected boolean canWork() {
        return gasTank.getFluidAmount() >= RIConfig.get(RIConfig.URANIUM_HEXAFLUORIDE_RATIO)
                && hasRoomFor(URANIUM_235, new ItemStack(RIRegistries.URANIUM.get())) && hasRoomFor(URANIUM_238, new ItemStack(RIRegistries.URANIUM_238.get()));
    }

    @Override
    protected void finishJob() {
        gasTank.drain(RIConfig.get(RIConfig.URANIUM_HEXAFLUORIDE_RATIO), IFluidHandler.FluidAction.EXECUTE);
        if (level.random.nextFloat() > 0.6f) {
            output(URANIUM_235, new ItemStack(RIRegistries.URANIUM.get()));
        } else {
            output(URANIUM_238, new ItemStack(RIRegistries.URANIUM_238.get()));
        }
    }

    @Override
    protected void tickServer() {
        if (level.getGameTime() % 20 == 0) {
            for (Direction d : Direction.values()) {
                if (level.getBlockEntity(worldPosition.relative(d)) instanceof CentrifugeBlockEntity) {
                    continue;
                }
                IFluidHandler neighbour = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(d), d.getOpposite());
                int room = gasTank.getCapacity() - gasTank.getFluidAmount();
                if (neighbour != null && room > 0) {
                    FluidStack got = neighbour.drain(new FluidStack(RIRegistries.URANIUM_HEXAFLUORIDE.get(), room), IFluidHandler.FluidAction.EXECUTE);
                    gasTank.fill(got, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
        super.tickServer();
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case URANIUM_235 -> stack.is(RIRegistries.URANIUM.get());
            case URANIUM_238 -> stack.is(RIRegistries.URANIUM_238.get());
            default -> true;
        };
    }

    @Override
    public MachineLayout layout() {
        return new MachineLayout(210,
                List.of(MachineLayout.battery(130, 25), MachineLayout.fluid(GAS_IN, 24, 49), MachineLayout.output(URANIUM_235, 80, 25),
                        MachineLayout.output(URANIUM_238, 100, 25)),
                List.of(new MachineLayout.GaugeAt(0, 8, 18)), 40, 26, 8, 106,
                List.of(new MachineLayout.Line(Component.translatable("gui.resonantinduction.centrifuge.1"), 8, 75),
                        new MachineLayout.Line(Component.translatable("gui.resonantinduction.centrifuge.2"), 8, 85),
                        new MachineLayout.Line(Component.translatable("gui.resonantinduction.centrifuge.3"), 8, 95)));
    }

    /** Takes the gas in; gives nothing out. */
    @Nullable
    public IFluidHandler getFluidCapability(@Nullable Direction side) {
        return new TwoTankHandler(gasTank, null, this::setChanged);
    }

    /** From the top, containers go in; from the sides and below, the uranium comes out. */
    public IItemHandler getItemCapability(@Nullable Direction side) {
        return side == Direction.UP ? new SidedSlots(inventory, new int[] {GAS_IN}, new int[0])
                : new SidedSlots(inventory, new int[0], new int[] {URANIUM_235, URANIUM_238});
    }
}
