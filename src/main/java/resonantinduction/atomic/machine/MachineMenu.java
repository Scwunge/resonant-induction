package resonantinduction.atomic.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import resonantinduction.registry.RIRegistries;

/** The atomic machines' container: the machine's slots as its layout places them, then the player's inventory. */
public class MachineMenu extends AbstractContainerMenu {
    private final MachineHost machine;
    private final ContainerData data;
    private final int machineSlots;

    public MachineMenu(int id, Inventory inventory, MachineHost machine, ContainerData data) {
        super(RIRegistries.ATOMIC_MACHINE_MENU.get(), id);
        this.machine = machine;
        this.data = data;
        MachineLayout layout = machine.layout(data);
        for (MachineLayout.SlotAt s : layout.slots()) {
            addSlot(new SlotItemHandler(machine.inventory(), s.index(), s.x() + 1, s.y() + 1) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return s.kind() != MachineLayout.Kind.OUTPUT && super.mayPlace(stack);
                }
            });
        }
        machineSlots = layout.slots().size();
        int top = layout.height() - 82;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, top + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, top + 58));
        }
        addDataSlots(data);
    }

    public static MachineMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        MachineHost machine = (MachineHost) inventory.player.level().getBlockEntity(pos);
        return new MachineMenu(id, inventory, machine, new SimpleContainerData(machine.data().getCount()));
    }

    public MachineHost machine() {
        return machine;
    }

    public ContainerData data() {
        return data;
    }

    public MachineLayout layout() {
        return machine.layout(data);
    }

    public long energy() {
        return ((long) data.get(0) << 31) | data.get(1);
    }

    public long capacity() {
        return ((long) data.get(2) << 31) | data.get(3);
    }

    public int timer() {
        return data.get(4);
    }

    public int jobTime() {
        return data.get(5);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            for (int i = 0; i < machineSlots && !stack.isEmpty(); i++) {
                Slot target = slots.get(i);
                if (target.mayPlace(stack)) {
                    moved |= moveItemStackTo(stack, i, i + 1, false);
                }
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return !machine.self().isRemoved() && player.distanceToSqr(machine.self().getBlockPos().getCenter()) <= 64;
    }
}
