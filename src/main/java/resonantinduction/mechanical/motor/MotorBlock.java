package resonantinduction.mechanical.motor;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import resonantinduction.battery.BatteryBlock;
import resonantinduction.mechanical.MechanicalBlockEntity;
import resonantinduction.registry.RIRegistries;

/**
 * Electric Motor. A wrench turns it to face the clicked side; sneak + wrench switches between motor and generator;
 * sneak-use with an empty hand changes the gear (low, medium, high).
 */
public class MotorBlock extends BaseEntityBlock {
    public static final MapCodec<MotorBlock> CODEC = simpleCodec(MotorBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public MotorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MotorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, RIRegistries.MOTOR_BE.get(), level.isClientSide ? MechanicalBlockEntity::clientTick : MechanicalBlockEntity::serverTick);
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
        if (!stack.is(BatteryBlock.WRENCHES) || !(level.getBlockEntity(pos) instanceof MotorBlockEntity motor)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            if (player.isSecondaryUseActive()) {
                boolean isMotor = motor.toggleMode();
                player.displayClientMessage(Component.translatable(isMotor ? "message.resonantinduction.motor.motor" : "message.resonantinduction.motor.generator"), true);
            } else {
                level.setBlockAndUpdate(pos, state.setValue(FACING, hit.getDirection()));
                level.invalidateCapabilities(pos);
                motor.markRecache();
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MotorBlockEntity motor) {
            if (player.isSecondaryUseActive()) {
                int gear = motor.toggleGear();
                player.displayClientMessage(Component.translatable("message.resonantinduction.motor.gear",
                        Component.translatable("message.resonantinduction.motor.gear." + gear)), true);
            } else {
                player.displayClientMessage(Component.translatable("message.resonantinduction.motor.status",
                        Component.translatable(motor.isMotor() ? "message.resonantinduction.motor.motor" : "message.resonantinduction.motor.generator"),
                        motor.energy(), String.format("%.2f", motor.node().getTorque()), String.format("%.2f", motor.node().getAngularVelocity())), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
