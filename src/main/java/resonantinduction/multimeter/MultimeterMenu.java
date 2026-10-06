package resonantinduction.multimeter;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import resonantinduction.registry.RIRegistries;

/** Menu for the multimeter settings screen. It has no slots; settings travel in a payload. */
public class MultimeterMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final Player player;

    public MultimeterMenu(int id, Inventory inventory, BlockPos pos) {
        super(RIRegistries.MULTIMETER_MENU.get(), id);
        this.pos = pos;
        this.player = inventory.player;
    }

    public static MultimeterMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new MultimeterMenu(id, inventory, buf.readBlockPos());
    }

    public BlockPos pos() {
        return pos;
    }

    @Nullable
    public MultimeterBlockEntity meter() {
        return player.level().getBlockEntity(pos) instanceof MultimeterBlockEntity meter ? meter : null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return meter() != null && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }
}
