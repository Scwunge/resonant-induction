package resonantinduction.logistic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import resonantinduction.archaic.ImprintableBlockEntity;
import resonantinduction.registry.RIRegistries;

/**
 * Manipulator, as the original. Taking in, it grabs items that come onto it and stores them in the inventory above, below or
 * behind it (or drops them behind). Putting out, on a redstone pulse (or by itself every half second), it takes an item from
 * above, below or behind and drops it in front. Its imprint (and inversion) decides which items it handles; with none, all.
 */
public class ManipulatorBlockEntity extends ImprintableBlockEntity {
    private boolean output;
    private boolean selfPulse;
    private int ticks;

    public ManipulatorBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.MANIPULATOR_BE.get(), pos, state);
    }

    public Direction facing() {
        return getBlockState().getValue(ManipulatorBlock.FACING);
    }

    public boolean output() {
        return output;
    }

    public boolean selfPulse() {
        return selfPulse;
    }

    public void toggleSelfPulse() {
        selfPulse = !selfPulse;
        changed();
    }

    /** Sneak + wrench steps through: take in, take in inverted, put out, put out inverted. */
    public void cycleMode() {
        if (inverted()) {
            output = !output;
        }
        toggleInverted();
        level.setBlock(worldPosition, getBlockState().setValue(ManipulatorBlock.OUTPUT, output), 3);
    }

    public void setMode(boolean output, boolean selfPulse) {
        this.output = output;
        this.selfPulse = selfPulse;
        level.setBlock(worldPosition, getBlockState().setValue(ManipulatorBlock.OUTPUT, output), 3);
        changed();
    }

    /** Handles {@code stack}? With no imprint, everything, as the original. */
    public boolean handles(ItemStack stack) {
        return imprint().isEmpty() || isFiltering(stack);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ManipulatorBlockEntity be) {
        be.ticks++;
        if (!be.output) {
            be.takeIn();
        } else if (level.hasNeighborSignal(pos) || (be.selfPulse && be.ticks % 10 == 0)) {
            be.putOut();
        }
    }

    private void takeIn() {
        BlockPos behind = worldPosition.relative(facing().getOpposite());
        // Two manipulators facing each other would pass items back and forth for ever.
        if (level.getBlockEntity(behind) instanceof ManipulatorBlockEntity other && other.facing() == facing().getOpposite()) {
            return;
        }
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(worldPosition))) {
            if (!item.isAlive() || !handles(item.getItem())) {
                continue;
            }
            ItemStack left = item.getItem().copy();
            left = ItemTransfer.store(level, worldPosition.above(), Direction.DOWN, left);
            if (!left.isEmpty()) {
                left = ItemTransfer.store(level, worldPosition.below(), Direction.UP, left);
            }
            if (!left.isEmpty()) {
                left = ItemTransfer.store(level, behind, facing(), left);
            }
            ItemTransfer.drop(level, behind, left);
            item.discard();
        }
    }

    private void putOut() {
        BlockPos behind = worldPosition.relative(facing().getOpposite());
        ItemStack got = ItemTransfer.grab(level, worldPosition.above(), Direction.DOWN, 1, this::handles);
        if (got.isEmpty()) {
            got = ItemTransfer.grab(level, worldPosition.below(), Direction.UP, 1, this::handles);
        }
        if (got.isEmpty()) {
            got = ItemTransfer.grab(level, behind, facing(), 1, this::handles);
        }
        ItemTransfer.drop(level, worldPosition.relative(facing()), got);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("isOutput", output);
        tag.putBoolean("selfpulse", selfPulse);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        output = tag.getBoolean("isOutput");
        selfPulse = tag.getBoolean("selfpulse");
    }
}
