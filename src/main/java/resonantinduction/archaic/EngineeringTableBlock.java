package resonantinduction.archaic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import resonantinduction.battery.BatteryBlock;
import resonantinduction.registry.RIRegistries;
import resonantinduction.resource.MachineRecipes;

/**
 * Engineering Table block. Right-click a square of the top to put the held stack there (empty hand takes it back); right-click a
 * side to craft (sneaking: as many as you can). Hit it with a Hammer to crush the ores laid on it. Sneak + left-click clears the
 * grid. Wrench turns it; sneak + wrench decides whether it takes from neighbouring chests.
 */
public class EngineeringTableBlock extends BaseEntityBlock {
    public static final MapCodec<EngineeringTableBlock> CODEC = simpleCodec(EngineeringTableBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14.4, 16);

    public EngineeringTableBlock(Properties properties) {
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
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EngineeringTableBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof EngineeringTableBlockEntity table)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.getItem() instanceof HammerItem) {
            if (!level.isClientSide) {
                hammer((ServerLevel) level, pos, table, player, stack, hand);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.is(BatteryBlock.WRENCHES)) {
            if (!level.isClientSide) {
                if (player.isSecondaryUseActive()) {
                    table.toggleSearch();
                    player.displayClientMessage(Component.translatable(table.searchInventories()
                            ? "message.resonantinduction.engineering_table.search_on" : "message.resonantinduction.engineering_table.search_off"), true);
                } else if (hit.getDirection().getAxis().isHorizontal()) {
                    level.setBlockAndUpdate(pos, state.setValue(FACING, hit.getDirection()));
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return use(state, level, pos, player, hit, table) ? ItemInteractionResult.sidedSuccess(level.isClientSide) : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof EngineeringTableBlockEntity table && use(state, level, pos, player, hit, table)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    private static boolean use(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit, EngineeringTableBlockEntity table) {
        Direction side = hit.getDirection();
        if (side == Direction.UP) {
            if (!level.isClientSide) {
                int slot = GridFace.slot(hit.getLocation(), pos, state.getValue(FACING));
                interact(table, slot, player);
            }
            return true;
        }
        if (side != Direction.DOWN) {
            if (!level.isClientSide) {
                int made = 0;
                do {
                    ItemStack out = table.craft(player);
                    if (out.isEmpty()) {
                        break;
                    }
                    if (!player.addItem(out)) {
                        player.drop(out, false);
                    }
                    made++;
                } while (player.isSecondaryUseActive() && made < 64);
                player.containerMenu.broadcastChanges();
            }
            return true;
        }
        return false;
    }

    /** The original's square interaction: put the held stack down, or pick up what's there. */
    static void interact(EngineeringTableBlockEntity table, int slot, Player player) {
        ItemStack held = player.getMainHandItem();
        ItemStack there = table.grid().get(slot);
        if (!held.isEmpty() && (there.isEmpty() || ItemStack.isSameItemSameComponents(there, held))) {
            int room = there.isEmpty() ? held.getCount() : Math.min(held.getCount(), there.getMaxStackSize() - there.getCount());
            if (room > 0) {
                table.setSlot(slot, there.isEmpty() ? held.split(room) : there.copyWithCount(there.getCount() + room));
                if (!there.isEmpty()) {
                    held.shrink(room);
                }
            }
        } else if (!there.isEmpty()) {
            table.setSlot(slot, ItemStack.EMPTY);
            if (!player.addItem(there)) {
                player.drop(there, false);
            }
        }
    }

    /** One hammer blow: a one in five chance to crush the first ore on the table. */
    private static void hammer(ServerLevel level, BlockPos pos, EngineeringTableBlockEntity table, Player player, ItemStack hammer, InteractionHand hand) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack input = table.grid().get(slot);
            if (input.isEmpty() || !MachineRecipes.canCrush(input)) {
                continue;
            }
            if (level.random.nextFloat() < 0.2f) {
                for (MachineRecipes.Output out : MachineRecipes.crusher(input)) {
                    if (level.random.nextFloat() <= out.chance()) {
                        ItemStack drop = out.stack().copy();
                        if (!player.addItem(drop)) {
                            player.drop(drop, false);
                        }
                    }
                }
                input.shrink(1);
                table.changed();
            }
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, input.isEmpty() ? hammer : input), pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                    6, 0.3, 0.1, 0.3, 0.1);
            level.playSound(null, pos, RIRegistries.HAMMER_SOUND.get(), SoundSource.BLOCKS, 0.5f, 0.8f + 0.2f * level.random.nextFloat());
            player.causeFoodExhaustion(0.1f);
            hammer.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            return;
        }
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof EngineeringTableBlockEntity table) {
            for (int i = 0; i < 9; i++) {
                ItemStack s = table.grid().get(i);
                if (!s.isEmpty()) {
                    table.setSlot(i, ItemStack.EMPTY);
                    if (!player.addItem(s)) {
                        player.drop(s, false);
                    }
                }
            }
        }
    }
}
