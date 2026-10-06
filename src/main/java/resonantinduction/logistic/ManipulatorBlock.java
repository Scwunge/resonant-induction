package resonantinduction.logistic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import resonantinduction.archaic.ImprintableBlock;
import resonantinduction.battery.BatteryBlock;
import resonantinduction.registry.RIRegistries;

/**
 * Manipulator block. Right-click with an Imprint to filter it (empty hand takes the imprint back); sneak-use with an empty hand
 * switches the self pulse; wrench turns it; sneak + wrench steps through its modes.
 */
public class ManipulatorBlock extends ImprintableBlock {
    public static final MapCodec<ManipulatorBlock> CODEC = simpleCodec(ManipulatorBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OUTPUT = BooleanProperty.create("output");
    private static final VoxelShape FLOOR = Block.box(0, 0, 0, 16, 1.44, 16);

    public ManipulatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OUTPUT, false));
    }

    @Override
    protected MapCodec<? extends ImprintableBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OUTPUT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FLOOR;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManipulatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != RIRegistries.MANIPULATOR_BE.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> ManipulatorBlockEntity.serverTick(lvl, pos, st, (ManipulatorBlockEntity) be);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(BatteryBlock.WRENCHES) && level.getBlockEntity(pos) instanceof ManipulatorBlockEntity m) {
            if (!level.isClientSide) {
                if (player.isSecondaryUseActive()) {
                    m.cycleMode();
                    player.displayClientMessage(Component.translatable(m.output() ? "message.resonantinduction.manipulator.output" : "message.resonantinduction.manipulator.input",
                            Component.translatable(m.inverted() ? "message.resonantinduction.inverted" : "message.resonantinduction.normal")), true);
                } else {
                    level.setBlockAndUpdate(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()));
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive() && level.getBlockEntity(pos) instanceof ManipulatorBlockEntity m) {
            if (!level.isClientSide) {
                m.toggleSelfPulse();
                player.displayClientMessage(Component.translatable(m.selfPulse() ? "message.resonantinduction.manipulator.pulse_on"
                        : "message.resonantinduction.manipulator.pulse_off"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }
}
