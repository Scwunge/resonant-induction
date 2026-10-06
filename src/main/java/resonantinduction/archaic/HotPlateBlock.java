package resonantinduction.archaic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
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

/**
 * Hot Plate block: a thin plate with four spots. Right-click a spot to put the held stack on it (or, empty-handed, take it back);
 * left-click takes a stack off. Standing on it while it's smelting burns.
 */
public class HotPlateBlock extends BaseEntityBlock {
    public static final MapCodec<HotPlateBlock> CODEC = simpleCodec(HotPlateBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 3.2, 16);

    public HotPlateBlock(Properties properties) {
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
        return new HotPlateBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != RIRegistries.HOT_PLATE_BE.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> HotPlateBlockEntity.serverTick(lvl, pos, st, (HotPlateBlockEntity) be);
    }

    /** The spot under the hit: slot = x half * 2 + z half. */
    public static int slot(BlockHitResult hit, BlockPos pos) {
        double x = hit.getLocation().x - pos.getX();
        double z = hit.getLocation().z - pos.getZ();
        return (x >= 0.5 ? 2 : 0) + (z >= 0.5 ? 1 : 0);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof HotPlateBlockEntity plate)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        int slot = slot(hit, pos);
        if (!plate.inventory().isItemValid(slot, stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            player.setItemInHand(hand, plate.inventory().insertItem(slot, stack.copy(), false));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof HotPlateBlockEntity plate) {
            int slot = slot(hit, pos);
            if (plate.inventory().getStackInSlot(slot).isEmpty()) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                give(player, plate.inventory().extractItem(slot, 64, false));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof HotPlateBlockEntity plate) {
            for (int i = 0; i < HotPlateBlockEntity.SLOTS; i++) {
                if (!plate.inventory().getStackInSlot(i).isEmpty()) {
                    give(player, plate.inventory().extractItem(i, 64, false));
                    return;
                }
            }
        }
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof HotPlateBlockEntity plate && plate.isSmelting()) {
            entity.hurt(level.damageSources().inFire(), 1);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof HotPlateBlockEntity plate) {
            for (int i = 0; i < HotPlateBlockEntity.SLOTS; i++) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, plate.inventory().getStackInSlot(i));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof HotPlateBlockEntity plate)) {
            return;
        }
        for (int i = 0; i < HotPlateBlockEntity.SLOTS; i++) {
            ItemStack stack = plate.inventory().getStackInSlot(i);
            if (stack.isEmpty() || plate.smeltTime(i) <= 0) {
                continue;
            }
            double x = pos.getX() + (i / 2) * 0.5 + 0.25;
            double y = pos.getY() + 0.2;
            double z = pos.getZ() + (i % 2) * 0.5 + 0.25;
            int max = HotPlateBlockEntity.MAX_SMELT_TIME * stack.getCount();
            int smoke = (int) ((double) (max - plate.smeltTime(i)) / max * 30);
            for (int n = 0; n < smoke; n++) {
                level.addParticle(ParticleTypes.SMOKE, x + (random.nextFloat() - 0.5) * 0.2, y + (random.nextFloat() - 0.5) * 0.2,
                        z + (random.nextFloat() - 0.5) * 0.2, 0, 0, 0);
            }
            level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.01, 0);
        }
    }
}
