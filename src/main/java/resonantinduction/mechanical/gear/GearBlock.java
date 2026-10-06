package resonantinduction.mechanical.gear;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import resonantinduction.mechanical.MechanicalBlockEntity;
import resonantinduction.registry.RIRegistries;

import java.util.EnumMap;
import java.util.Map;

/** A gear on the face of a block (ATTACH points at the block it lies on). Use a Hand Crank on it to turn it. */
public class GearBlock extends BaseEntityBlock {
    public static final int CREATIVE = 3;
    public static final DirectionProperty ATTACH = BlockStateProperties.FACING;
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        SHAPES.put(Direction.DOWN, Block.box(0, 0, 0, 16, 2, 16));
        SHAPES.put(Direction.UP, Block.box(0, 14, 0, 16, 16, 16));
        SHAPES.put(Direction.NORTH, Block.box(0, 0, 0, 16, 16, 2));
        SHAPES.put(Direction.SOUTH, Block.box(0, 0, 14, 16, 16, 16));
        SHAPES.put(Direction.WEST, Block.box(0, 0, 0, 2, 16, 16));
        SHAPES.put(Direction.EAST, Block.box(14, 0, 0, 16, 16, 16));
    }

    private final int tier;
    private final MapCodec<GearBlock> codec;

    public GearBlock(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
        this.codec = simpleCodec(p -> new GearBlock(tier, p));
        registerDefaultState(stateDefinition.any().setValue(ATTACH, Direction.DOWN));
    }

    public int tier() {
        return tier;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ATTACH);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(ATTACH, context.getClickedFace().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(ATTACH));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GearBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, RIRegistries.GEAR_BE.get(), level.isClientSide ? MechanicalBlockEntity::clientTick : MechanicalBlockEntity::serverTick);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        if (level.getBlockEntity(pos) instanceof MechanicalBlockEntity be) {
            be.markRecache();
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof HandCrankItem && level.getBlockEntity(pos) instanceof GearBlockEntity gear) {
            if (!level.isClientSide) {
                gear.crank(!player.isSecondaryUseActive());
                level.playSound(null, pos, RIRegistries.GEAR_CRANK.get(), SoundSource.BLOCKS, 0.5f, 0.9f + level.random.nextFloat() * 0.2f);
                player.causeFoodExhaustion(0.01f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
