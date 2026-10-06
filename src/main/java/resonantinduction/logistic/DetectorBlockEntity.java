package resonantinduction.logistic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import resonantinduction.archaic.ImprintableBlockEntity;
import resonantinduction.registry.RIRegistries;

import java.util.List;

/**
 * Detector, as the original: twice a second it looks for items in the block in front of it and gives a redstone signal (15, out of
 * every side but the front) while there are any. With an imprint, only items on it count (or, inverted, items not on it).
 */
public class DetectorBlockEntity extends ImprintableBlockEntity {
    private int ticks;

    public DetectorBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.DETECTOR_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DetectorBlockEntity be) {
        if (++be.ticks % 10 != 0) {
            return;
        }
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos.relative(state.getValue(DetectorBlock.FACING))));
        boolean detected;
        if (be.imprint().isEmpty()) {
            detected = !items.isEmpty();
        } else {
            // The original checks each item in turn, so the last one decides.
            detected = false;
            for (ItemEntity item : items) {
                detected = be.isFiltering(item.getItem());
            }
        }
        if (detected != state.getValue(DetectorBlock.POWERED)) {
            level.setBlock(pos, state.setValue(DetectorBlock.POWERED, detected), 3);
            level.updateNeighborsAt(pos, state.getBlock());
            level.updateNeighborsAt(pos.above(), state.getBlock());
        }
    }

    @Override
    protected void changed() {
        super.changed();
        if (level != null && !level.isClientSide && getBlockState().getValue(DetectorBlock.INVERTED) != inverted()) {
            level.setBlock(worldPosition, getBlockState().setValue(DetectorBlock.INVERTED, inverted()), 3);
        }
    }
}
