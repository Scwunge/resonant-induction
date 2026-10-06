package resonantinduction.atomic.reactor;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import resonantinduction.battery.BatteryBlock;
import resonantinduction.registry.RIRegistries;

/**
 * Siren, as the original: sounds the alarm every second and a half while it has a redstone signal, louder for each siren beside
 * it. A wrench lowers its pitch (sneak + wrench raises it).
 */
public class SirenBlock extends Block {
    public static final MapCodec<SirenBlock> CODEC = simpleCodec(SirenBlock::new);
    public static final IntegerProperty PITCH = IntegerProperty.create("pitch", 0, 15);

    public SirenBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PITCH, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PITCH);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBestNeighborSignal(pos) <= 0) {
            return;
        }
        float volume = 0.5f;
        for (Direction d : Direction.values()) {
            if (level.getBlockState(pos.relative(d)).is(this)) {
                volume *= 1.5f;
            }
        }
        level.playSound(null, pos, RIRegistries.ALARM_SOUND.get(), SoundSource.BLOCKS, volume, 1f - 0.18f * (state.getValue(PITCH) / 15f));
        level.scheduleTick(pos, this, 30);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(BatteryBlock.WRENCHES)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            int pitch = Math.floorMod(state.getValue(PITCH) + (player.isSecondaryUseActive() ? -1 : 1), 16);
            level.setBlockAndUpdate(pos, state.setValue(PITCH, pitch));
            player.displayClientMessage(Component.translatable("message.resonantinduction.siren.pitch", pitch), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
