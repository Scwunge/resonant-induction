package resonantinduction.archaic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import resonantinduction.battery.BatteryBlock;

/**
 * Blocks that take an Imprint: use one on the block to put it in, use the block with an empty hand to take it out,
 * sneak + wrench to invert it.
 */
public abstract class ImprintableBlock extends BaseEntityBlock {
    protected ImprintableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected abstract MapCodec<? extends ImprintableBlock> codec();

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ImprintableBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ImprintableBlockEntity be)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.is(BatteryBlock.WRENCHES) && player.isSecondaryUseActive()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.resonantinduction.imprint.inverted", be.toggleInverted()), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof ImprintItem && be.imprint().isEmpty()) {
            if (!level.isClientSide) {
                be.setImprint(stack.split(1));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof ImprintableBlockEntity be && !be.imprint().isEmpty()) {
            if (!level.isClientSide) {
                ItemStack imprint = be.imprint();
                be.setImprint(ItemStack.EMPTY);
                if (!player.addItem(imprint)) {
                    player.drop(imprint, false);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ImprintableBlockEntity be && !be.imprint().isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, be.imprint());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
