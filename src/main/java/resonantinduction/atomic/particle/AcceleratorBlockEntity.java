package resonantinduction.atomic.particle;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import resonantinduction.RIConfig;
import resonantinduction.atomic.machine.MachineHost;
import resonantinduction.atomic.machine.MachineLayout;
import resonantinduction.atomic.machine.MachineMenu;
import resonantinduction.atomic.machine.SidedSlots;
import resonantinduction.multimeter.Measure;
import resonantinduction.registry.RIRegistries;

import java.util.List;

/**
 * Particle Accelerator, as the original. Powered by redstone and fed energy, it fires the item in its first slot as a particle out
 * of its back into a tunnel of electromagnets, every two seconds while none is flying. A particle that reaches full speed becomes
 * antimatter (5 mg and up to the item's block hardness more); 125 mg fill an empty cell. A fast crash may leave dark matter.
 */
public class AcceleratorBlockEntity extends BlockEntity implements MenuProvider, MachineHost {
    public static final float MAX_VELOCITY = 0.9f;
    public static final int ITEM = 0, CELL = 1, ANTIMATTER = 2, DARK_MATTER = 3;
    /** Milligrams of antimatter in a cell. */
    public static final int PER_CELL = 125;
    /** The original's use a tick, in joules; it holds forty ticks' worth. */
    private static final long BASE_USE = 240_000;

    private final ItemStackHandler inventory = new ItemStackHandler(4) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case CELL -> stack.is(RIRegistries.EMPTY_CELL.get());
                case ANTIMATTER -> stack.is(RIRegistries.ANTIMATTER.get());
                case DARK_MATTER -> stack.is(RIRegistries.DARK_MATTER.get());
                default -> true;
            };
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private long energy;
    private long energyUsed;
    private int antimatter;
    @Nullable
    private ParticleEntity particle;
    private float velocity;
    private int lastSpawnTick;
    private int density = 1;

    public AcceleratorBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.ACCELERATOR_BE.get(), pos, state);
    }

    public long usePerTick() {
        return (long) Math.ceil(BASE_USE * RIConfig.get(RIConfig.ATOMIC_ENERGY_SCALE));
    }

    public long capacity() {
        return usePerTick() * 40;
    }

    public int antimatter() {
        return antimatter;
    }

    @Nullable
    public ParticleEntity particle() {
        return particle;
    }

    /** Where particles come out: behind it. */
    public BlockPos emitPos() {
        return worldPosition.relative(getBlockState().getValue(AcceleratorBlock.FACING).getOpposite());
    }

    /** A particle that lost track of its accelerator (after a reload) finds it again. */
    void claim(ParticleEntity p) {
        if (particle == null) {
            particle = p;
        }
    }

    private boolean powered() {
        return level.hasNeighborSignal(worldPosition);
    }

    void tickServer() {
        velocity = particle != null ? (float) particle.velocity() : 0;
        fillCell();
        if (powered() && energy >= usePerTick()) {
            if (particle == null) {
                ItemStack item = inventory.getStackInSlot(ITEM);
                if (!item.isEmpty() && lastSpawnTick >= 40 && ParticleEntity.canTravel(level, emitPos())) {
                    energyUsed = 0;
                    particle = new ParticleEntity(level, emitPos(), worldPosition, getBlockState().getValue(AcceleratorBlock.FACING).getOpposite());
                    level.addFreshEntity(particle);
                    density = item.getItem() instanceof BlockItem block ? Math.max(1, (int) Math.abs(block.getBlock().defaultDestroyTime())) : 1;
                    inventory.extractItem(ITEM, 1, false);
                    lastSpawnTick = 0;
                }
            } else if (particle.isRemoved()) {
                if (particle.collided() && level.random.nextFloat() <= RIConfig.get(RIConfig.DARK_MATTER_CHANCE)) {
                    inventory.insertItem(DARK_MATTER, new ItemStack(RIRegistries.DARK_MATTER.get()), false);
                }
                particle = null;
            } else if (velocity >= MAX_VELOCITY) {
                level.playSound(null, worldPosition, RIRegistries.ANTIMATTER_SOUND.get(), SoundSource.BLOCKS, 2f, 1f - level.random.nextFloat() * 0.3f);
                antimatter += 5 + level.random.nextInt(density);
                energyUsed = 0;
                particle.discard();
                particle = null;
            }
            if (particle != null) {
                level.playSound(null, worldPosition, RIRegistries.ACCELERATOR_SOUND.get(), SoundSource.BLOCKS, 1.5f, 0.6f + 0.4f * velocity / MAX_VELOCITY);
            }
            energy -= usePerTick();
            energyUsed += usePerTick();
        } else if (particle != null) {
            particle.discard();
            particle = null;
        }
        lastSpawnTick++;
        setChanged();
    }

    /** 125 mg of antimatter and an empty cell make an antimatter cell. */
    private void fillCell() {
        ItemStack cell = new ItemStack(RIRegistries.ANTIMATTER.get());
        if (antimatter >= PER_CELL && !inventory.getStackInSlot(CELL).isEmpty() && inventory.insertItem(ANTIMATTER, cell, true).isEmpty()) {
            antimatter -= PER_CELL;
            inventory.extractItem(CELL, 1, false);
            inventory.insertItem(ANTIMATTER, cell, false);
        }
    }

    @Override
    public ItemStackHandler inventory() {
        return inventory;
    }

    @Override
    public List<FluidTank> tanks() {
        return List.of();
    }

    @Override
    public BlockEntity self() {
        return this;
    }

    @Override
    public ContainerData data() {
        return data;
    }

    private int status() {
        if (!ParticleEntity.canTravel(level, emitPos())) {
            return 2;
        }
        return particle != null && velocity > 0 ? 1 : 0;
    }

    /** Synced to the screen: energy and capacity (two ints each), two unused, velocity in ten-thousandths, antimatter, energy used, status. */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> (int) (energy >>> 31);
                case 1 -> (int) (energy & Integer.MAX_VALUE);
                case 2 -> (int) (capacity() >>> 31);
                case 3 -> (int) (capacity() & Integer.MAX_VALUE);
                case 6 -> (int) (velocity * 10000);
                case 7 -> antimatter;
                case 8 -> (int) (energyUsed >>> 31);
                case 9 -> (int) (energyUsed & Integer.MAX_VALUE);
                case 10 -> status();
                default -> 0;
            };
        }

        @Override
        public void set(int i, int value) {}

        @Override
        public int getCount() {
            return 11;
        }
    };

    @Override
    public MachineLayout layout(ContainerData d) {
        long used = ((long) d.get(8) << 31) | d.get(9);
        long stored = ((long) d.get(0) << 31) | d.get(1);
        long cap = ((long) d.get(2) << 31) | d.get(3);
        Component status = switch (d.get(10)) {
            case 2 -> Component.translatable("gui.resonantinduction.accelerator.blocked").withStyle(ChatFormatting.DARK_RED);
            case 1 -> Component.translatable("gui.resonantinduction.accelerator.accelerating").withStyle(ChatFormatting.GOLD);
            default -> Component.translatable("gui.resonantinduction.accelerator.idle").withStyle(ChatFormatting.DARK_GREEN);
        };
        Direction out = getBlockState().getValue(AcceleratorBlock.FACING).getOpposite();
        return new MachineLayout(228,
                List.of(MachineLayout.slot(ITEM, 130, 24), MachineLayout.slot(CELL, 130, 49), MachineLayout.output(ANTIMATTER, 130, 73),
                        MachineLayout.output(DARK_MATTER, 104, 73)),
                List.of(), -1, 0, -1, 0,
                List.of(new MachineLayout.Line(Component.translatable("gui.resonantinduction.accelerator.velocity", Math.round(d.get(6) / 10000f / MAX_VELOCITY * 100)), 8, 20),
                        new MachineLayout.Line(Component.translatable("gui.resonantinduction.accelerator.used"), 8, 31),
                        new MachineLayout.Line(Component.literal(" " + Measure.ENERGY.format(used)), 8, 42),
                        new MachineLayout.Line(Component.translatable("gui.resonantinduction.accelerator.use", Measure.POWER.format(usePerTick())), 8, 53),
                        new MachineLayout.Line(Component.translatable("gui.resonantinduction.accelerator.antimatter", d.get(7)), 8, 64),
                        new MachineLayout.Line(Component.translatable("gui.resonantinduction.accelerator.status", status), 8, 97),
                        new MachineLayout.Line(Component.translatable("gui.resonantinduction.accelerator.buffer", Measure.ENERGY.format(stored), Measure.ENERGY.format(cap)), 8, 108),
                        new MachineLayout.Line(Component.translatable("gui.resonantinduction.accelerator.facing", Component.translatable("direction.resonantinduction." + out.getSerializedName())), 8, 119)));
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

    /** It takes power only while powered by redstone with something to fire, as the original; at most one tick's use a tick. */
    public IEnergyStorage getEnergyCapability(@Nullable Direction side) {
        return new IEnergyStorage() {
            @Override
            public int receiveEnergy(int max, boolean simulate) {
                if (inventory.getStackInSlot(ITEM).isEmpty() || !powered()) {
                    return 0;
                }
                int accepted = (int) Math.max(0, Math.min(Math.min(max, usePerTick()), capacity() - energy));
                if (!simulate) {
                    energy += accepted;
                    setChanged();
                }
                return accepted;
            }

            @Override
            public int extractEnergy(int max, boolean simulate) {
                return 0;
            }

            @Override
            public int getEnergyStored() {
                return (int) Math.min(Integer.MAX_VALUE, energy);
            }

            @Override
            public int getMaxEnergyStored() {
                return (int) Math.min(Integer.MAX_VALUE, capacity());
            }

            @Override
            public boolean canExtract() {
                return false;
            }

            @Override
            public boolean canReceive() {
                return true;
            }
        };
    }

    /** Items and empty cells go in; antimatter and dark matter come out. */
    public IItemHandler getItemCapability(@Nullable Direction side) {
        return new SidedSlots(inventory, new int[] {ITEM, CELL}, new int[] {ANTIMATTER, DARK_MATTER});
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.putLong("energy", energy);
        tag.putLong("energyUsed", energyUsed);
        tag.putInt("antimatter", antimatter);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        energy = tag.getLong("energy");
        energyUsed = tag.getLong("energyUsed");
        antimatter = tag.getInt("antimatter");
    }

    static void tick(Level level, BlockPos pos, BlockState state, AcceleratorBlockEntity be) {
        be.tickServer();
    }
}
