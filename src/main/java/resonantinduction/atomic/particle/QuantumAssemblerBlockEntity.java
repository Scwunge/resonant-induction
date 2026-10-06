package resonantinduction.atomic.particle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import resonantinduction.RIConfig;
import resonantinduction.ResonantInduction;
import resonantinduction.atomic.machine.AtomicMachineBlockEntity;
import resonantinduction.atomic.machine.MachineLayout;
import resonantinduction.atomic.machine.SidedSlots;
import resonantinduction.registry.RIRegistries;

import java.util.List;

/**
 * Quantum Assembler, as the original: with dark matter in all six of its outer slots, it copies the item in the middle, one more
 * every two minutes, using one dark matter from each slot. It needs a full buffer of energy every tick of the job (see the
 * antimatter energy scale). What it can copy is set by the config (items, or items and blocks, plus a list) less a tag.
 */
public class QuantumAssemblerBlockEntity extends AtomicMachineBlockEntity {
    public static final int MAX_TIME = 20 * 120;
    public static final int FIRST_DARK_MATTER = 1, TARGET = 7;
    public static final TagKey<Item> BLACKLIST = TagKey.create(Registries.ITEM, ResonantInduction.id("quantum_assembler_blacklist"));

    public QuantumAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.QUANTUM_ASSEMBLER_BE.get(), pos, state, 8);
    }

    public static boolean canCopy(ItemStack stack) {
        if (stack.isEmpty() || stack.is(BLACKLIST)) {
            return false;
        }
        if (RIConfig.get(RIConfig.QUANTUM_ASSEMBLER_RECIPES).contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
            return true;
        }
        int mode = RIConfig.get(RIConfig.QUANTUM_ASSEMBLER_MODE);
        return mode == 2 || mode == 1 && !(stack.getItem() instanceof BlockItem);
    }

    public ItemStack target() {
        return inventory.getStackInSlot(TARGET);
    }

    @Override
    protected long baseUse() {
        return 10_000_000_000_000L;
    }

    @Override
    public long usePerTick() {
        return (long) Math.ceil(baseUse() * RIConfig.get(RIConfig.ANTIMATTER_ENERGY_SCALE));
    }

    /** One tick's worth, as the original. */
    @Override
    public long capacity() {
        return Math.max(1, usePerTick());
    }

    @Override
    public int jobTime() {
        return MAX_TIME;
    }

    @Override
    protected boolean canWork() {
        ItemStack target = target();
        if (!canCopy(target) || target.getCount() >= target.getMaxStackSize()) {
            return false;
        }
        for (int i = FIRST_DARK_MATTER; i < TARGET; i++) {
            if (!inventory.getStackInSlot(i).is(RIRegistries.DARK_MATTER.get())) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void finishJob() {
        for (int i = FIRST_DARK_MATTER; i < TARGET; i++) {
            inventory.extractItem(i, 1, false);
        }
        inventory.setStackInSlot(TARGET, target().copyWithCount(target().getCount() + 1));
    }

    @Override
    protected void tickServer() {
        super.tickServer();
        if (timer > 0 && level.getGameTime() % 600 == 0) {
            level.playSound(null, worldPosition, RIRegistries.ASSEMBLER_SOUND.get(), SoundSource.BLOCKS, 0.7f, 1f);
        }
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        return slot == TARGET || stack.is(RIRegistries.DARK_MATTER.get());
    }

    @Override
    public List<FluidTank> tanks() {
        return List.of();
    }

    @Override
    public MachineLayout layout() {
        return layout(data);
    }

    @Override
    public MachineLayout layout(ContainerData d) {
        Component status;
        int time = d.get(4);
        if (time > 0) {
            status = Component.translatable("gui.resonantinduction.quantum_assembler.progress", (int) (100 - (float) time / MAX_TIME * 100));
        } else {
            status = Component.translatable(canWork() ? "gui.resonantinduction.quantum_assembler.ready" : "gui.resonantinduction.quantum_assembler.idle");
        }
        return new MachineLayout(230,
                List.of(MachineLayout.slot(1, 79, 39), MachineLayout.slot(2, 52, 55), MachineLayout.slot(3, 106, 55), MachineLayout.slot(4, 52, 87),
                        MachineLayout.slot(5, 106, 87), MachineLayout.slot(6, 79, 102), MachineLayout.slot(TARGET, 79, 71)),
                List.of(), -1, 0, 8, 124, List.of(new MachineLayout.Line(status, 9, 114)));
    }

    @Override
    protected long syncHash() {
        return super.syncHash() * 31 + ItemStack.hashItemAndComponents(target()) * 7 + target().getCount();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (!target().isEmpty()) {
            tag.put("target", target().save(registries));
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        inventory.setStackInSlot(TARGET, ItemStack.parseOptional(registries, tag.getCompound("target")));
    }

    /** Dark matter goes in; the copies come out. */
    public IItemHandler getItemCapability(@Nullable Direction side) {
        return new SidedSlots(inventory, new int[] {1, 2, 3, 4, 5, 6}, new int[] {TARGET});
    }
}
