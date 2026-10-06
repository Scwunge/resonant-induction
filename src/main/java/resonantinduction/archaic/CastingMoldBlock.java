package resonantinduction.archaic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import resonantinduction.registry.RIRegistries;

/** Casting Mold block: right- or left-click drops the cast ingots at your feet, as the original. */
public class CastingMoldBlock extends BaseEntityBlock {
    public static final MapCodec<CastingMoldBlock> CODEC = simpleCodec(CastingMoldBlock::new);
    private static final VoxelShape SHAPE = Block.box(0.5, 0, 0.5, 15.5, 13.5, 15.5);

    public CastingMoldBlock(Properties properties) {
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

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CastingMoldBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != RIRegistries.CASTING_MOLD_BE.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> CastingMoldBlockEntity.serverTick(lvl, pos, st, (CastingMoldBlockEntity) be);
    }

    private static boolean takeOutput(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof CastingMoldBlockEntity mold) || mold.output().getStackInSlot(0).isEmpty()) {
            return false;
        }
        if (!level.isClientSide) {
            ItemStack out = mold.output().extractItem(0, 64, false);
            Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), out);
        }
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return takeOutput(level, pos, player) ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        takeOutput(level, pos, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CastingMoldBlockEntity mold) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, mold.output().getStackInSlot(0));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
