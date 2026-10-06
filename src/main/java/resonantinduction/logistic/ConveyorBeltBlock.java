package resonantinduction.logistic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import resonantinduction.battery.BatteryBlock;
import resonantinduction.registry.RIRegistries;

/**
 * Conveyor Belt, as the original: it carries whatever is on it along its facing (always running; a redstone signal stops it), keeping items centred and alive. Sneaking players nearby can pick the items up. Wrench turns it; sneak + wrench
 * cycles flat, slanted up, slanted down and raised.
 */
public class ConveyorBeltBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ConveyorBeltBlock> CODEC = simpleCodec(ConveyorBeltBlock::new);
    public static final EnumProperty<Type> TYPE = EnumProperty.create("type", Type.class);
    /** The original belt's speed: angular velocity 1, over 20. */
    public static final double SPEED = 1 / 20d;

    public enum Type implements StringRepresentable {
        FLAT, SLANT_UP, SLANT_DOWN, RAISED;

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }

    private static final VoxelShape FLAT = Block.box(0, 0, 0, 16, 5, 16);
    private static final VoxelShape RAISED = Block.box(0, 10.88, 0, 16, 15.7, 16);
    private static final VoxelShape SLANT_OUTLINE = Block.box(0, 0, 0, 16, 15.4, 16);

    public ConveyorBeltBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TYPE, Type.FLAT));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TYPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(TYPE)) {
            case FLAT -> FLAT;
            case RAISED -> RAISED;
            default -> SLANT_OUTLINE;
        };
    }

    /** Slants: the low half and a step up the high half, as the original's boxes. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Type type = state.getValue(TYPE);
        if (type == Type.FLAT) {
            return FLAT;
        }
        if (type == Type.RAISED) {
            return RAISED;
        }
        Direction high = type == Type.SLANT_UP ? state.getValue(FACING) : state.getValue(FACING).getOpposite();
        double x0 = high == Direction.EAST ? 8 : 0;
        double x1 = high == Direction.WEST ? 8 : 16;
        double z0 = high == Direction.SOUTH ? 8 : 0;
        double z1 = high == Direction.NORTH ? 8 : 16;
        return Shapes.or(Block.box(0, 0, 0, 16, 4.8, 16), Block.box(x0, 0, z0, x1, 12.8, z1));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        if (!stack.is(BatteryBlock.WRENCHES)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            if (player.isSecondaryUseActive()) {
                Type next = Type.values()[(state.getValue(TYPE).ordinal() + 1) % Type.values().length];
                level.setBlockAndUpdate(pos, state.setValue(TYPE, next));
            } else {
                level.setBlockAndUpdate(pos, state.setValue(FACING, state.getValue(FACING).getCounterClockWise()));
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        move(state, level, pos, entity);
    }

    static void move(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.hasNeighborSignal(pos)) {
            return;
        }
        Direction dir = state.getValue(FACING);
        Type type = state.getValue(TYPE);
        Vec3 v = entity.getDeltaMovement();
        double vx = dir.getStepX() != 0 ? dir.getStepX() * SPEED : v.x / 2;
        double vz = dir.getStepZ() != 0 ? dir.getStepZ() * SPEED : v.z / 2;
        double vy = type == Type.SLANT_UP ? SPEED * 3 : type == Type.SLANT_DOWN ? -SPEED : v.y;
        if (entity instanceof ItemEntity item) {
            // Keep items in the middle of the belt.
            if (dir.getStepX() != 0) {
                vz += (pos.getZ() + 0.5 - item.getZ()) * 0.1;
            } else {
                vx += (pos.getX() + 0.5 - item.getX()) * 0.1;
            }
            item.setUnlimitedLifetime();
            boolean sneaking = !level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(1), Player::isShiftKeyDown).isEmpty();
            item.setPickUpDelay(sneaking ? 0 : 20);
        }
        entity.setDeltaMovement(vx, vy, vz);
        if (type != Type.FLAT) {
            entity.setOnGround(false);
        }
        entity.hurtMarked = true;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // The first belt of a line hums, as the original.
        if (random.nextInt(4) == 0 && !level.getBlockState(pos.west()).is(this) && !level.getBlockState(pos.north()).is(this) && !level.hasNeighborSignal(pos)) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, RIRegistries.CONVEYOR_SOUND.get(), SoundSource.BLOCKS, 0.5f, 0.5f + 0.15f, false);
        }
    }
}
