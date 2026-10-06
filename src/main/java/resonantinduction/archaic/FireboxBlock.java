package resonantinduction.archaic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;
import resonantinduction.registry.RIRegistries;

/**
 * Firebox block. Right-click puts fuel in (or pours a lava bucket in), left-click takes the fuel back out, as the original. The
 * electric one also burns on power from any side but the top.
 */
public class FireboxBlock extends BaseEntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private final boolean electric;
    private final MapCodec<FireboxBlock> codec;

    public FireboxBlock(boolean electric, Properties properties) {
        super(properties);
        this.electric = electric;
        this.codec = simpleCodec(p -> new FireboxBlock(electric, p));
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    public boolean electric() {
        return electric;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FireboxBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != RIRegistries.FIREBOX_BE.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> FireboxBlockEntity.serverTick(lvl, pos, st, (FireboxBlockEntity) be);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FireboxBlockEntity firebox)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection().getAxis().isVertical() ? null : hit.getDirection())) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (firebox.fuel().isItemValid(0, stack)) {
            if (!level.isClientSide) {
                ItemStack left = firebox.fuel().insertItem(0, stack.copy(), false);
                player.setItemInHand(hand, left);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FireboxBlockEntity firebox) {
            ItemStack out = firebox.fuel().extractItem(0, 64, false);
            if (!out.isEmpty() && !player.addItem(out)) {
                player.drop(out, false);
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof FireboxBlockEntity firebox) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, firebox.fuel().getStackInSlot(0));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY() + random.nextFloat() * 6 / 16;
        double z = pos.getZ() + 0.5;
        double offset = 0.52;
        double side = random.nextFloat() * 0.6 - 0.3;
        for (double[] at : new double[][] {{x - offset, z + side}, {x + offset, z + side}, {x + side, z - offset}, {x + side, z + offset}}) {
            level.addParticle(ParticleTypes.SMOKE, at[0], y, at[1], 0, 0, 0);
            level.addParticle(ParticleTypes.FLAME, at[0], y, at[1], 0, 0, 0);
        }
        if (level.getFluidState(pos.above()).is(Fluids.WATER)) {
            for (int i = 0; i < 4; i++) {
                level.addParticle(ParticleTypes.BUBBLE, x + random.nextFloat() - 0.5, y + 1.5, z + random.nextFloat() - 0.5, 0, 0.05, 0);
            }
        }
    }
}
