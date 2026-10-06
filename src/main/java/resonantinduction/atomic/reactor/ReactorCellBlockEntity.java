package resonantinduction.atomic.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import resonantinduction.RIConfig;
import resonantinduction.atomic.FuelRodItem;
import resonantinduction.atomic.Radiation;
import resonantinduction.atomic.ThermalGrid;
import resonantinduction.atomic.machine.MachineHost;
import resonantinduction.atomic.machine.MachineLayout;
import resonantinduction.atomic.machine.MachineMenu;
import resonantinduction.registry.RIRegistries;

import java.util.List;

/**
 * Reactor Cell, as the original. Cells stacked in a column work as one, the lowest holding the fuel and the tank. A fuel rod heats
 * it (each control rod beside it takes off a tenth); the heat goes into the world's thermal grid, where it boils the water around
 * the reactor into steam. Three or more neighbouring cells hotter than 1200 K make a fissile rod breed instead of burn. Fission makes
 * toxic waste; a tank full of it leaks into the world. Kept at 2000 K or more for 50 seconds, it melts down.
 */
public class ReactorCellBlockEntity extends BlockEntity implements MenuProvider, MachineHost {
    public static final int RADIUS = 2;
    public static final int MELTING_POINT = 2000;
    private static final int SPECIFIC_HEAT = 1000;
    /** The original's mass: 1000 L at 7 kg/m^3. */
    private static final float MASS = 1000 / 1000f * 7;
    private static final int MELTDOWN_TICKS = 1000;

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof FuelRodItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            sync();
        }
    };
    private final FluidTank tank = new FluidTank(15000);
    private float temperature = ThermalGrid.AMBIENT;
    private long heatThisTick;
    private int meltdownCounter;
    private int ticks;

    public ReactorCellBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.REACTOR_CELL_BE.get(), pos, state);
    }

    @Override
    public ItemStackHandler inventory() {
        return inventory;
    }

    public FluidTank tank() {
        return tank;
    }

    public float temperature() {
        return temperature;
    }

    /** The cell holding this column's fuel: the lowest. */
    public ReactorCellBlockEntity primary() {
        BlockPos pos = worldPosition;
        ReactorCellBlockEntity lowest = this;
        while (level.getBlockEntity(pos.below()) instanceof ReactorCellBlockEntity below) {
            lowest = below;
            pos = pos.below();
        }
        return lowest;
    }

    public boolean isPrimary() {
        return !(level.getBlockEntity(worldPosition.below()) instanceof ReactorCellBlockEntity);
    }

    /** How many cells tall this column is from here up. */
    public int height() {
        int h = 0;
        BlockPos pos = worldPosition;
        while (level.getBlockEntity(pos) instanceof ReactorCellBlockEntity) {
            h++;
            pos = pos.above();
        }
        return h;
    }

    /** Fuel adds heat through here (the original's IReactor.heat). */
    public void heat(long energy) {
        heatThisTick = Math.max(heatThisTick + energy, 0);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ReactorCellBlockEntity be) {
        be.tickServer((ServerLevel) level);
    }

    private void tickServer(ServerLevel level) {
        ticks++;
        if (!isPrimary()) {
            // Fuel and fluid move down to the cell at the bottom.
            ReactorCellBlockEntity primary = primary();
            if (!inventory.getStackInSlot(0).isEmpty() && primary.inventory.getStackInSlot(0).isEmpty()) {
                primary.inventory.setStackInSlot(0, inventory.extractItem(0, 1, false));
            }
            if (!tank.isEmpty()) {
                tank.drain(primary.tank.fill(tank.getFluid().copy(), IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
            }
        }
        if (isPrimary() && tank.getFluid().getFluid().isSame(RIRegistries.PLASMA.get())) {
            FusionHooks.releasePlasma(level, this);
        } else {
            react(level);
        }
        if (ticks % 60 == 0) {
            sync();
        }
    }

    private void react(ServerLevel level) {
        ItemStack rod = primary().inventory.getStackInSlot(0);
        if (rod.getItem() instanceof FuelRodItem fuel) {
            burn(level, rod, fuel);
            if (rod.getDamageValue() >= rod.getMaxDamage()) {
                primary().inventory.setStackInSlot(0, ItemStack.EMPTY);
            }
            if (ticks % 20 == 0 && level.random.nextFloat() > 0.65f) {
                Radiation.exposeAround(level, worldPosition, RADIUS * 2, 0);
            }
        }
        temperature = ThermalGrid.temperature(level, worldPosition);
        if (heatThisTick > 0) {
            float deltaT = heatThisTick * 0.15f / (MASS * SPECIFIC_HEAT);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (level.getBlockState(worldPosition.relative(d)).is(RIRegistries.CONTROL_ROD.get())) {
                    deltaT /= 1.1f;
                }
            }
            ThermalGrid.addTemperature(level, worldPosition, deltaT);
            sounds(level);
        }
        heatThisTick = 0;
        if (temperature >= MELTING_POINT) {
            if (++meltdownCounter >= MELTDOWN_TICKS && RIConfig.get(RIConfig.REACTOR_MELTDOWNS)) {
                meltDown(level);
                return;
            }
        } else {
            meltdownCounter = 0;
        }
        if (isOverToxic()) {
            leak(level);
        }
    }

    /** The fuel rods' reaction, as the original's ItemFissileFuel and ItemBreederFuel. */
    private void burn(ServerLevel level, ItemStack rod, FuelRodItem fuel) {
        if (fuel.fissile()) {
            int hotNeighbours = 0;
            for (Direction d : Direction.values()) {
                if (level.getBlockEntity(worldPosition.relative(d)) instanceof ReactorCellBlockEntity other && other.temperature > FuelRodItem.BREEDING_TEMP) {
                    hotNeighbours++;
                }
            }
            if (hotNeighbours >= 3) {
                if (level.random.nextInt(1000) <= 100 && temperature > FuelRodItem.BREEDING_TEMP) {
                    rod.setDamageValue(Math.max(rod.getDamageValue() - level.random.nextInt(5), 0));
                }
                return;
            }
            heat(FuelRodItem.ENERGY_PER_TICK);
            if (RIConfig.get(RIConfig.TOXIC_WASTE) && level.random.nextFloat() > 0.5f) {
                primary().tank.fill(new FluidStack(RIRegistries.TOXIC_WASTE.get(), 1), IFluidHandler.FluidAction.EXECUTE);
            }
        } else {
            heat(FuelRodItem.ENERGY_PER_TICK / 2);
        }
        if (level.getGameTime() % 20 == 0) {
            rod.setDamageValue(Math.min(rod.getDamageValue() + 1, rod.getMaxDamage()));
            primary().sync();
        }
    }

    private void sounds(ServerLevel level) {
        if (temperature < 373) {
            return;
        }
        BlockPos p = worldPosition;
        if (level.random.nextInt(80) == 0) {
            level.playSound(null, p, SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 0.5f, 2.1f + (level.random.nextFloat() - level.random.nextFloat()) * 0.85f);
        }
        if (level.random.nextInt(40) == 0) {
            level.playSound(null, p, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.5f, 2.6f + (level.random.nextFloat() - level.random.nextFloat()) * 0.8f);
        }
        if (level.getGameTime() % 100 == 0) {
            level.playSound(null, p, RIRegistries.REACTOR_CELL_SOUND.get(), SoundSource.BLOCKS, Math.min(temperature / MELTING_POINT, 1f), 1f);
        }
    }

    public boolean isOverToxic() {
        return tank.getFluid().getFluid().isSame(RIRegistries.TOXIC_WASTE.get()) && tank.getFluidAmount() >= tank.getCapacity();
    }

    /** A full tank of toxic waste spills somewhere within ten blocks, poisoning grass into radioactive waste. */
    private void leak(ServerLevel level) {
        BlockPos at = worldPosition.offset(level.random.nextInt(20) - 10, level.random.nextInt(20) - 10, level.random.nextInt(20) - 10);
        BlockState there = level.getBlockState(at);
        if (there.is(Blocks.GRASS_BLOCK)) {
            level.setBlockAndUpdate(at, RIRegistries.RADIOACTIVE_WASTE.get().defaultBlockState());
            tank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
        } else if (there.canBeReplaced() && there.getFluidState().isEmpty()) {
            level.setBlockAndUpdate(at, RIRegistries.TOXIC_WASTE_BLOCK.get().defaultBlockState());
            tank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    /** The original's ReactorExplosion: a fiery blast of 9 that leaves radioactive waste on the ground it clears. */
    private void meltDown(ServerLevel level) {
        BlockPos c = worldPosition;
        level.removeBlock(c, false);
        level.explode(null, c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5, 9f, true, Level.ExplosionInteraction.BLOCK);
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-9, -9, -9), c.offset(9, 9, 9))) {
            if (p.distSqr(c) <= 81 && level.isEmptyBlock(p) && level.getBlockState(p.below()).isSolidRender(level, p.below()) && level.random.nextInt(3) == 0) {
                level.setBlockAndUpdate(p, RIRegistries.RADIOACTIVE_WASTE.get().defaultBlockState());
            }
        }
        Radiation.exposeAround(level, c, 20, 2);
    }

    void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    /** Plasma goes in (for fusion); toxic waste comes out (from a drain or pipe). */
    @Nullable
    public IFluidHandler getFluidCapability(@Nullable Direction side) {
        ReactorCellBlockEntity primary = level == null ? this : primary();
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int t) {
                return primary.tank.getFluid();
            }

            @Override
            public int getTankCapacity(int t) {
                return primary.tank.getCapacity();
            }

            @Override
            public boolean isFluidValid(int t, FluidStack stack) {
                return stack.getFluid().isSame(RIRegistries.PLASMA.get());
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return isFluidValid(0, resource) ? primary.tank.fill(resource, action) : 0;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return FluidStack.isSameFluidSameComponents(resource, primary.tank.getFluid()) ? drain(resource.getAmount(), action) : FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return primary.tank.getFluid().getFluid().isSame(RIRegistries.TOXIC_WASTE.get()) ? primary.tank.drain(maxDrain, action) : FluidStack.EMPTY;
            }
        };
    }

    /** Synced to the screen: temperature, seconds of fuel left. */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            if (i == 0) {
                return (int) temperature;
            }
            ItemStack rod = level == null ? ItemStack.EMPTY : primary().inventory.getStackInSlot(0);
            return rod.isEmpty() ? -1 : rod.getMaxDamage() - rod.getDamageValue();
        }

        @Override
        public void set(int i, int value) {}

        @Override
        public int getCount() {
            return 2;
        }
    };

    @Override
    public ContainerData data() {
        return data;
    }

    @Override
    public BlockEntity self() {
        return this;
    }

    @Override
    public List<FluidTank> tanks() {
        return List.of(tank);
    }

    @Override
    public MachineLayout layout(ContainerData d) {
        int seconds = d.get(1);
        return new MachineLayout(200, List.of(MachineLayout.slot(0, 78, 16)), List.of(new MachineLayout.GaugeAt(0, 79, 36)), -1, 0, -1, 0,
                List.of(new MachineLayout.Line(Component.translatable("gui.resonantinduction.reactor_cell.temperature"), 9, 45),
                        new MachineLayout.Line(Component.literal(d.get(0) + " / " + MELTING_POINT + " K"), 9, 58),
                        new MachineLayout.Line(Component.translatable("gui.resonantinduction.reactor_cell.remaining"), 110, 45),
                        new MachineLayout.Line(seconds < 0 ? Component.literal("-") : Component.translatable("gui.resonantinduction.seconds", seconds), 110, 58),
                        new MachineLayout.Line(Component.translatable("gui.resonantinduction.reactor_cell.1"), 9, 90),
                        new MachineLayout.Line(Component.translatable("gui.resonantinduction.reactor_cell.2"), 9, 100)));
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MachineMenu(id, inv, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putFloat("temperature", temperature);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        tank.readFromNBT(registries, tag.getCompound("tank"));
        temperature = tag.contains("temperature") ? tag.getFloat("temperature") : ThermalGrid.AMBIENT;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
