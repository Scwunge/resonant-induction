package resonantinduction.atomic.reactor;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Control Rod, as the original: each one beside a reactor cell takes a tenth off the heat it makes. */
public class ControlRodBlock extends Block {
    public static final MapCodec<ControlRodBlock> CODEC = simpleCodec(ControlRodBlock::new);
    private static final VoxelShape SHAPE = box(4.8, 0, 4.8, 11.2, 16, 11.2);

    public ControlRodBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
