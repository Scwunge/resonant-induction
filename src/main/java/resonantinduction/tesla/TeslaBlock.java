package resonantinduction.tesla;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import resonantinduction.registry.RIRegistries;

public class TeslaBlock extends BaseEntityBlock {
    public static final MapCodec<TeslaBlock> CODEC = simpleCodec(TeslaBlock::new);
    public static final EnumProperty<TeslaPart> PART = EnumProperty.create("part", TeslaPart.class);

    private static final VoxelShape SHAPE_BOTTOM = Block.box(2.5, 0, 2.5, 13.5, 16, 13.5);
    private static final VoxelShape SHAPE_UPPER = Block.box(3.5, 0, 3.5, 12.5, 16, 12.5);

    public TeslaBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, TeslaPart.BOTTOM));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(PART) == TeslaPart.BOTTOM ? SHAPE_BOTTOM : SHAPE_UPPER;
    }

    private TeslaPart partAt(LevelAccessor level, BlockPos pos) {
        return TeslaPart.of(level.getBlockState(pos.below()).is(this), level.getBlockState(pos.above()).is(this));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(PART, partAt(context.getLevel(), context.getClickedPos()));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction.getAxis() != Direction.Axis.Y) {
            return state;
        }
        // The bottom coil owns the energy port, so cached capabilities change whenever the tower does.
        if (level instanceof Level l) {
            l.invalidateCapabilities(pos);
        }
        return state.setValue(PART, partAt(level, pos));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TeslaBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, RIRegistries.TESLA_BE.get(), TeslaBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level.getBlockEntity(pos) instanceof TeslaBlockEntity coil)) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        DyeColor dye = DyeColor.getColor(stack);
        if (dye != null) {
            if (!level.isClientSide) {
                coil.primary().setColor(dye);
                stack.consume(1, player);
                player.displayClientMessage(Component.translatable("message.resonantinduction.tesla.color", Component.translatable("color.minecraft." + dye.getName())), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.is(Items.REDSTONE)) {
            if (!level.isClientSide) {
                boolean attack = coil.primary().toggleAttack();
                stack.consume(1, player);
                player.displayClientMessage(Component.translatable("message.resonantinduction.tesla.toggle_attack", attack), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        // Any other item (the Quantum Entangler included) does its own thing; don't toggle receive mode.
        return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TeslaBlockEntity coil)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            TeslaBlockEntity primary = coil.primary();
            if (player.isSecondaryUseActive()) {
                player.displayClientMessage(primary.status(), false);
            } else {
                player.displayClientMessage(Component.translatable("message.resonantinduction.tesla.mode", primary.toggleReceive()), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
