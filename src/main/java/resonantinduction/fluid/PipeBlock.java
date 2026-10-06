package resonantinduction.fluid;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import resonantinduction.registry.RIRegistries;

import java.util.Map;

/** Pipe block, one per material. Right-click with a dye to colour it: differently dyed pipes don't join. */
public class PipeBlock extends BaseEntityBlock {
    public static final Map<Direction, BooleanProperty> SIDES = net.minecraft.world.level.block.PipeBlock.PROPERTY_BY_DIRECTION;
    private static final VoxelShape[] SHAPES = new VoxelShape[64];

    static {
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape s = Block.box(4, 4, 4, 12, 12, 12);
            for (Direction d : Direction.values()) {
                if ((mask & (1 << d.ordinal())) != 0) {
                    double x0 = d.getStepX() < 0 ? 0 : d.getStepX() > 0 ? 12 : 4;
                    double x1 = d.getStepX() < 0 ? 4 : d.getStepX() > 0 ? 16 : 12;
                    double y0 = d.getStepY() < 0 ? 0 : d.getStepY() > 0 ? 12 : 4;
                    double y1 = d.getStepY() < 0 ? 4 : d.getStepY() > 0 ? 16 : 12;
                    double z0 = d.getStepZ() < 0 ? 0 : d.getStepZ() > 0 ? 12 : 4;
                    double z1 = d.getStepZ() < 0 ? 4 : d.getStepZ() > 0 ? 16 : 12;
                    s = Shapes.or(s, Block.box(x0, y0, z0, x1, y1, z1));
                }
            }
            SHAPES[mask] = s.optimize();
        }
    }

    private final PipeMaterial material;
    private final MapCodec<PipeBlock> codec;

    public PipeBlock(PipeMaterial material, Properties properties) {
        super(properties);
        this.material = material;
        this.codec = simpleCodec(p -> new PipeBlock(material, p));
        BlockState def = stateDefinition.any();
        for (BooleanProperty p : SIDES.values()) {
            def = def.setValue(p, false);
        }
        registerDefaultState(def);
    }

    public PipeMaterial material() {
        return material;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        SIDES.values().forEach(builder::add);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (Direction d : Direction.values()) {
            if (state.getValue(SIDES.get(d))) {
                mask |= 1 << d.ordinal();
            }
        }
        return SHAPES[mask];
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PipeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != RIRegistries.PIPE_BE.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> FluidNodeBlockEntity.serverTick(lvl, pos, st, (FluidNodeBlockEntity) be);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof FluidNodeBlockEntity be) {
            be.markRecache();
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof DyeItem dye && level.getBlockEntity(pos) instanceof PipeBlockEntity pipe) {
            if (pipe.color() == dye.getDyeColor()) {
                return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide) {
                pipe.setColor(dye.getDyeColor());
                stack.consume(1, player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
