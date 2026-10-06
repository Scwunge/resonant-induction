package resonantinduction.atomic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.function.Predicate;

/**
 * The original's connected-texture border: a block draws an edge strip along each side of its faces where the neighbour that way
 * is not part of the same structure. Each direction is a block state property, true when joined; the blockstate file draws the
 * strips (one edge model, rotated) for the directions that are false.
 */
public final class Edges {
    private Edges() {}

    public static BooleanProperty property(Direction dir) {
        return PipeBlock.PROPERTY_BY_DIRECTION.get(dir);
    }

    public static void addProperties(StateDefinition.Builder<Block, BlockState> builder) {
        for (Direction dir : Direction.values()) {
            builder.add(property(dir));
        }
    }

    /** {@code state} with every direction joined or not to what is there now. */
    public static BlockState join(BlockState state, BlockGetter level, BlockPos pos, Predicate<BlockState> joins) {
        for (Direction dir : Direction.values()) {
            state = state.setValue(property(dir), joins.test(level.getBlockState(pos.relative(dir))));
        }
        return state;
    }

    /** {@code state} after the neighbour on {@code dir} changed to {@code neighbour}. */
    public static BlockState update(BlockState state, Direction dir, BlockState neighbour, Predicate<BlockState> joins) {
        return state.setValue(property(dir), joins.test(neighbour));
    }
}
