package resonantinduction.atomic.fusion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import resonantinduction.ResonantInduction;
import resonantinduction.atomic.Edges;

/**
 * Electromagnet and electromagnet glass, as the original: the walls of a particle accelerator's tunnel and of a fusion reactor.
 * They hold plasma back and shed heat thirty times as fast as other blocks (both by tag). Joined electromagnets share one border.
 */
public class ElectromagnetBlock extends Block {
    public static final MapCodec<ElectromagnetBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.fieldOf("glass").forGetter(b -> b.glass), propertiesCodec()).apply(i, ElectromagnetBlock::new));
    /** What counts as an electromagnet to a particle in an accelerator: these blocks and the accelerator itself. */
    public static final TagKey<Block> ELECTROMAGNETS = TagKey.create(Registries.BLOCK, ResonantInduction.id("electromagnets"));

    private final boolean glass;

    public ElectromagnetBlock(boolean glass, Properties properties) {
        super(properties);
        this.glass = glass;
        BlockState state = stateDefinition.any();
        for (Direction dir : Direction.values()) {
            state = state.setValue(Edges.property(dir), false);
        }
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        Edges.addProperties(builder);
    }

    static boolean joins(BlockState other) {
        return other.getBlock() instanceof ElectromagnetBlock;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return Edges.join(defaultBlockState(), context.getLevel(), context.getClickedPos(), ElectromagnetBlock::joins);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return Edges.update(state, dir, neighbour, ElectromagnetBlock::joins);
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState neighbour, Direction dir) {
        return glass && neighbour.getBlock() instanceof ElectromagnetBlock other && other.glass || super.skipRendering(state, neighbour, dir);
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return glass ? Shapes.empty() : super.getVisualShape(state, level, pos, context);
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return glass ? 1f : super.getShadeBrightness(state, level, pos);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return glass;
    }
}
