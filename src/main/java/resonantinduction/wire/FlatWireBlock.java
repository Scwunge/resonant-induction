package resonantinduction.wire;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * A wire lying flat on the face of a block (FACE points away from that block). It joins wires and machines in the four
 * directions along its face, and the block it lies on.
 */
public class FlatWireBlock extends WireBlock {
    public static final DirectionProperty FACE = BlockStateProperties.FACING;
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape> SHAPES_INSULATED = new EnumMap<>(Direction.class);

    static {
        for (Direction face : Direction.values()) {
            SHAPES.put(face, plate(face, 1));
            SHAPES_INSULATED.put(face, plate(face, 2));
        }
    }

    /** A full-size plate {@code thickness} pixels thick against the block behind the wire. */
    private static VoxelShape plate(Direction face, double t) {
        return switch (face) {
            case UP -> Block.box(0, 0, 0, 16, t, 16);
            case DOWN -> Block.box(0, 16 - t, 0, 16, 16, 16);
            case NORTH -> Block.box(0, 0, 16 - t, 16, 16, 16);
            case SOUTH -> Block.box(0, 0, 0, 16, 16, t);
            case WEST -> Block.box(16 - t, 0, 0, 16, 16, 16);
            case EAST -> Block.box(0, 0, 0, t, 16, 16);
        };
    }

    private final MapCodec<FlatWireBlock> codec;

    public FlatWireBlock(WireMaterial material, Properties properties) {
        super(material, properties);
        this.codec = simpleCodec(p -> new FlatWireBlock(material, p));
        registerDefaultState(defaultBlockState().setValue(FACE, Direction.UP));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACE);
    }

    @Override
    public boolean reaches(BlockState state, Direction dir) {
        Direction face = state.getValue(FACE);
        return dir.getAxis() != face.getAxis() || dir == face.getOpposite();
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACE, context.getClickedFace());
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction face = state.getValue(FACE);
        BlockPos support = pos.relative(face.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, face);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == state.getValue(FACE).getOpposite() && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(INSULATED) ? SHAPES_INSULATED : SHAPES).get(state.getValue(FACE));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }
}
