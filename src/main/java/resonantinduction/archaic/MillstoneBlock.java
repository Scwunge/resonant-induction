package resonantinduction.archaic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import resonantinduction.mechanical.gear.HandCrankItem;
import resonantinduction.registry.RIRegistries;
import resonantinduction.resource.MachineRecipes;

/**
 * Millstone: the hand-powered grinder. Put a grindable item in (right-click), then turn it with a Hand Crank: every twentieth
 * turn grinds one item, as in the original. Empty hand takes the stack back out.
 */
public class MillstoneBlock extends BaseEntityBlock {
    public static final MapCodec<MillstoneBlock> CODEC = simpleCodec(MillstoneBlock::new);

    public MillstoneBlock(Properties properties) {
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

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof Tile mill)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.getItem() instanceof HandCrankItem) {
            if (!mill.inventory.getStackInSlot(0).isEmpty()) {
                if (!level.isClientSide) {
                    mill.grind(player);
                    player.causeFoodExhaustion(0.3f);
                    level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.5f, 1f);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (mill.inventory.getStackInSlot(0).isEmpty() && MachineRecipes.canGrind(stack)) {
            if (!level.isClientSide) {
                mill.inventory.setStackInSlot(0, stack.copy());
                stack.setCount(0);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof Tile mill && !mill.inventory.getStackInSlot(0).isEmpty()) {
            if (!level.isClientSide) {
                ItemStack out = mill.inventory.extractItem(0, 64, false);
                if (!player.addItem(out)) {
                    player.drop(out, false);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof Tile mill) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, mill.inventory.getStackInSlot(0));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static class Tile extends BlockEntity {
        final ItemStackHandler inventory = new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return MachineRecipes.canGrind(stack);
            }

            @Override
            protected void onContentsChanged(int slot) {
                turns = 0;
                setChanged();
                if (level != null && !level.isClientSide) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
            }
        };
        private int turns;

        public Tile(BlockPos pos, BlockState state) {
            super(RIRegistries.MILLSTONE_BE.get(), pos, state);
        }

        public ItemStackHandler inventory() {
            return inventory;
        }

        void grind(Player player) {
            ItemStack input = inventory.getStackInSlot(0);
            var outputs = MachineRecipes.grinder(input);
            if (outputs.isEmpty() || ++turns <= 20) {
                return;
            }
            for (MachineRecipes.Output out : outputs) {
                if (level.random.nextFloat() <= out.chance()) {
                    ItemStack drop = out.stack().copy();
                    if (!player.addItem(drop)) {
                        Block.popResource(level, worldPosition.above(), drop);
                    }
                }
            }
            inventory.extractItem(0, 1, false);
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.put("inventory", inventory.serializeNBT(registries));
            tag.putInt("turns", turns);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            inventory.deserializeNBT(registries, tag.getCompound("inventory"));
            turns = tag.getInt("turns");
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
