package resonantinduction.logistic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import resonantinduction.archaic.ImprintItem;
import resonantinduction.battery.BatteryBlock;

import java.util.Map;

/** Sorter block. Right-click a face with an Imprint to set that face's (empty hand takes it back); a wrench inverts all faces. */
public class SorterBlock extends BaseEntityBlock {
    public static final MapCodec<SorterBlock> CODEC = simpleCodec(SorterBlock::new);
    public static final Map<Direction, BooleanProperty> SIDES = PipeBlock.PROPERTY_BY_DIRECTION;
    public static final BooleanProperty INVERTED = BooleanProperty.create("inverted");
    private static final VoxelShape COLLISION = Block.box(0.16, 0.16, 0.16, 15.84, 15.84, 15.84);

    public SorterBlock(Properties properties) {
        super(properties);
        BlockState def = stateDefinition.any().setValue(INVERTED, false);
        for (BooleanProperty p : SIDES.values()) {
            def = def.setValue(p, false);
        }
        registerDefaultState(def);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        SIDES.values().forEach(builder::add);
        builder.add(INVERTED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SorterBlockEntity(pos, state);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof ItemEntity item && item.isAlive() && level.getBlockEntity(pos) instanceof SorterBlockEntity sorter) {
            ItemStack stack = item.getItem().copy();
            item.discard();
            sorter.sort(stack);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SorterBlockEntity sorter)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.is(BatteryBlock.WRENCHES)) {
            if (!level.isClientSide) {
                sorter.toggleInverted();
                player.displayClientMessage(Component.translatable("message.resonantinduction.imprint.inverted", sorter.inverted()), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        int slot = hit.getDirection().get3DDataValue();
        if (stack.getItem() instanceof ImprintItem && sorter.imprints().getStackInSlot(slot).isEmpty()) {
            if (!level.isClientSide) {
                sorter.imprints().setStackInSlot(slot, stack.split(1));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof SorterBlockEntity sorter) {
            int slot = hit.getDirection().get3DDataValue();
            if (!sorter.imprints().getStackInSlot(slot).isEmpty()) {
                if (!level.isClientSide) {
                    ItemStack out = sorter.imprints().extractItem(slot, 1, false);
                    if (!player.addItem(out)) {
                        player.drop(out, false);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SorterBlockEntity sorter) {
            for (int i = 0; i < 6; i++) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, sorter.imprints().getStackInSlot(i));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
