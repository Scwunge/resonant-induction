package resonantinduction.atomic.reactor;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
import org.jetbrains.annotations.Nullable;
import resonantinduction.atomic.ThermalGrid;
import resonantinduction.battery.BatteryBlock;
import resonantinduction.registry.RIRegistries;

/**
 * Thermometer, as the original: shows the temperature of the block it tracks (set by sneak-using the thermometer item on a block;
 * itself if none), and gives a redstone signal at or above its warning level. Right-click lowers the warning level by 100 (sneaking:
 * raises it); a wrench raises it by 10 (sneak + wrench lowers). Keeps its settings when broken.
 */
public class ThermometerBlock extends BaseEntityBlock {
    public static final MapCodec<ThermometerBlock> CODEC = simpleCodec(ThermometerBlock::new);
    public static final int MAX_THRESHOLD = 5000;

    public ThermometerBlock(Properties properties) {
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
        return new Tile(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != RIRegistries.THERMOMETER_BE.get()) {
            return null;
        }
        return (l, p, s, be) -> ((Tile) be).tick(l, p);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof Tile t && t.powering ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    private static InteractionResult adjust(Level level, BlockPos pos, Player player, int delta) {
        if (level.getBlockEntity(pos) instanceof Tile t) {
            if (!level.isClientSide) {
                t.setThreshold(t.threshold + delta);
                player.displayClientMessage(Component.translatable("message.resonantinduction.thermometer.threshold", t.threshold), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(BatteryBlock.WRENCHES)) {
            adjust(level, pos, player, player.isSecondaryUseActive() ? -10 : 10);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return adjust(level, pos, player, player.isSecondaryUseActive() ? 100 : -100);
    }

    public static class Tile extends BlockEntity {
        private float detected = ThermalGrid.AMBIENT;
        @Nullable
        private BlockPos track;
        private int threshold = 1000;
        private boolean powering;
        private int ticks;

        public Tile(BlockPos pos, BlockState state) {
            super(RIRegistries.THERMOMETER_BE.get(), pos, state);
        }

        public float detected() {
            return detected;
        }

        public int threshold() {
            return threshold;
        }

        @Nullable
        public BlockPos track() {
            return track;
        }

        void setThreshold(int value) {
            threshold = value % MAX_THRESHOLD;
            if (threshold <= 0) {
                threshold = MAX_THRESHOLD;
            }
            setChanged();
            sync();
        }

        void tick(Level level, BlockPos pos) {
            if (++ticks % 10 != 0) {
                return;
            }
            float now = ThermalGrid.temperature(level, track != null ? track : pos);
            boolean over = now >= threshold;
            if (now != detected || over != powering) {
                detected = now;
                if (over != powering) {
                    powering = over;
                    level.updateNeighborsAt(pos, getBlockState().getBlock());
                }
                sync();
            }
        }

        private void sync() {
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
            }
        }

        @Override
        protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
            super.applyImplicitComponents(input);
            GlobalPos saved = input.get(RIRegistries.LINK_TARGET.get());
            track = saved == null ? null : saved.pos();
        }

        @Override
        protected void collectImplicitComponents(DataComponentMap.Builder builder) {
            super.collectImplicitComponents(builder);
            if (track != null && level != null) {
                builder.set(RIRegistries.LINK_TARGET.get(), GlobalPos.of(level.dimension(), track));
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putInt("threshold", threshold);
            tag.putFloat("detected", detected);
            tag.putBoolean("powering", powering);
            if (track != null) {
                tag.put("track", NbtUtils.writeBlockPos(track));
            }
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            threshold = tag.contains("threshold") ? tag.getInt("threshold") : 1000;
            detected = tag.contains("detected") ? tag.getFloat("detected") : ThermalGrid.AMBIENT;
            powering = tag.getBoolean("powering");
            track = NbtUtils.readBlockPos(tag, "track").orElse(null);
        }

        @Override
        public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
            return saveWithoutMetadata(registries);
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }
    }
}
