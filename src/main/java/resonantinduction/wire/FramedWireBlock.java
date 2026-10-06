package resonantinduction.wire;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A wire through the middle of a block, joining in all six directions (placed by sneaking with a wire). */
public class FramedWireBlock extends WireBlock {
    private static final VoxelShape[] SHAPES = new VoxelShape[64];
    private static final VoxelShape[] SHAPES_INSULATED = new VoxelShape[64];

    static {
        for (int mask = 0; mask < 64; mask++) {
            SHAPES[mask] = shape(mask, 6.5, 9.5);
            SHAPES_INSULATED[mask] = shape(mask, 5, 11);
        }
    }

    private static VoxelShape shape(int mask, double lo, double hi) {
        VoxelShape s = Block.box(lo, lo, lo, hi, hi, hi);
        for (Direction d : Direction.values()) {
            if ((mask & (1 << d.ordinal())) == 0) {
                continue;
            }
            double x0 = d.getStepX() < 0 ? 0 : d.getStepX() > 0 ? hi : lo;
            double x1 = d.getStepX() < 0 ? lo : d.getStepX() > 0 ? 16 : hi;
            double y0 = d.getStepY() < 0 ? 0 : d.getStepY() > 0 ? hi : lo;
            double y1 = d.getStepY() < 0 ? lo : d.getStepY() > 0 ? 16 : hi;
            double z0 = d.getStepZ() < 0 ? 0 : d.getStepZ() > 0 ? hi : lo;
            double z1 = d.getStepZ() < 0 ? lo : d.getStepZ() > 0 ? 16 : hi;
            s = Shapes.or(s, Block.box(x0, y0, z0, x1, y1, z1));
        }
        return s.optimize();
    }

    private final MapCodec<FramedWireBlock> codec;

    public FramedWireBlock(WireMaterial material, Properties properties) {
        super(material, properties);
        this.codec = simpleCodec(p -> new FramedWireBlock(material, p));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    public boolean reaches(BlockState state, Direction dir) {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (Direction d : Direction.values()) {
            if (state.getValue(SIDES.get(d))) {
                mask |= 1 << d.ordinal();
            }
        }
        return state.getValue(INSULATED) ? SHAPES_INSULATED[mask] : SHAPES[mask];
    }
}
