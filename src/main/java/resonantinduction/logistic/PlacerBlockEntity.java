package resonantinduction.logistic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import resonantinduction.registry.RIRegistries;

/**
 * Placer, as the original: holds one stack, and a quarter second after a redstone signal places a block of it in front. Set to
 * pull, it refills itself from the inventory behind it.
 */
public class PlacerBlockEntity extends BlockEntity {
    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
            }
        }
    };
    private boolean autoPull;
    private int ticks;

    public PlacerBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.PLACER_BE.get(), pos, state);
    }

    public ItemStackHandler inventory() {
        return inventory;
    }

    public Direction facing() {
        return getBlockState().getValue(PlacerBlock.FACING);
    }

    public boolean autoPull() {
        return autoPull;
    }

    public void toggleAutoPull() {
        autoPull = !autoPull;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PlacerBlockEntity be) {
        if (be.autoPull && ++be.ticks % 5 == 0 && be.inventory.getStackInSlot(0).isEmpty()) {
            Direction facing = be.facing();
            be.inventory.setStackInSlot(0, ItemTransfer.grab(level, pos.relative(facing.getOpposite()), facing, 1, s -> true));
        }
    }

    /** Places one of the held block in front, if there's room. */
    public void place() {
        ItemStack stack = inventory.getStackInSlot(0);
        if (!(stack.getItem() instanceof BlockItem item)) {
            return;
        }
        Direction facing = facing();
        BlockPos target = worldPosition.relative(facing);
        ItemStack one = stack.copyWithCount(1);
        DirectionalPlaceContext context = new DirectionalPlaceContext(level, target, facing, one, facing.getOpposite());
        if (context.canPlace() && item.place(context) instanceof InteractionResult r && r.consumesAction()) {
            inventory.extractItem(0, 1, false);
        }
    }

    /** Hoppers fill it from behind. */
    @Nullable
    public IItemHandler getItemCapability(@Nullable Direction side) {
        return side == null || side == facing().getOpposite() ? inventory : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.putBoolean("autoPull", autoPull);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        autoPull = tag.getBoolean("autoPull");
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
