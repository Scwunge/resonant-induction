package resonantinduction.archaic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Imprinter block. Right-click a square of the top to lay one of the held item there (sneaking: the whole stack); clicking a
 * square you can't add to takes its items back. Right-click a side to take the imprint out, or to put the held Imprint in. A piston
 * pushing onto it stamps the imprint.
 */
public class ImprinterBlock extends BaseEntityBlock {
    public static final MapCodec<ImprinterBlock> CODEC = simpleCodec(ImprinterBlock::new);

    public ImprinterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ImprinterBlockEntity(pos, state);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, neighborPos, movedByPiston);
        if (!level.isClientSide && neighborPos.equals(pos.above()) && level.getBlockState(neighborPos).is(Blocks.MOVING_PISTON)
                && level.getBlockEntity(pos) instanceof ImprinterBlockEntity imprinter) {
            imprinter.stamp();
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return use(level, pos, player, hit) ? ItemInteractionResult.sidedSuccess(level.isClientSide) : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return use(level, pos, player, hit) ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }

    private static boolean use(Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ImprinterBlockEntity imprinter) || hit.getDirection() == Direction.DOWN) {
            return false;
        }
        if (level.isClientSide) {
            return true;
        }
        var inv = imprinter.inventory();
        ItemStack held = player.getMainHandItem();
        if (hit.getDirection() == Direction.UP) {
            double x = hit.getLocation().x - pos.getX();
            double z = hit.getLocation().z - pos.getZ();
            int slot = Math.min(2, (int) (z * 3)) * 3 + Math.min(2, (int) (x * 3));
            ItemStack there = inv.getStackInSlot(slot);
            boolean inserted = false;
            if (!held.isEmpty() && (there.isEmpty() || ItemStack.isSameItemSameComponents(there, held))) {
                int amount = player.isSecondaryUseActive() ? held.getCount() : 1;
                ItemStack left = inv.insertItem(slot, held.copyWithCount(amount), false);
                int moved = amount - left.getCount();
                held.shrink(moved);
                inserted = moved > 0;
            }
            if (!inserted && !there.isEmpty()) {
                ItemStack out = inv.extractItem(slot, 64, false);
                if (!player.addItem(out)) {
                    player.drop(out, false);
                }
            }
            return true;
        }
        ItemStack imprint = inv.getStackInSlot(ImprinterBlockEntity.IMPRINT_SLOT);
        if (!imprint.isEmpty()) {
            ItemStack out = inv.extractItem(ImprinterBlockEntity.IMPRINT_SLOT, 1, false);
            if (!player.addItem(out)) {
                player.drop(out, false);
            }
        } else if (held.getItem() instanceof ImprintItem) {
            inv.setStackInSlot(ImprinterBlockEntity.IMPRINT_SLOT, held.split(1));
        }
        return true;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ImprinterBlockEntity imprinter) {
            for (int i = 0; i < imprinter.inventory().getSlots(); i++) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, imprinter.inventory().getStackInSlot(i));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
