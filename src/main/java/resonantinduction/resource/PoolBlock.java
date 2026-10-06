package resonantinduction.resource;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import resonantinduction.registry.RIRegistries;

/**
 * A pool of molten metal or of dust mixture (dust stirred into water), one to eight levels deep: the original's finite per-metal
 * fluids. Like them it falls into the space below and spreads out sideways until it is a level deep. Mixtures are filtered into refined dust; molten metal is poured into casting molds. An empty bucket scoops a
 * full pool; molten metal burns.
 */
public class PoolBlock extends BaseEntityBlock {
    public enum Kind { MOLTEN, MIXTURE }

    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 1, 8);
    /** A full pool is a bucket. */
    public static final int MB_PER_LEVEL = 125;
    private static final VoxelShape[] SHAPES = new VoxelShape[9];

    static {
        for (int i = 1; i <= 8; i++) {
            SHAPES[i] = Block.box(0, 0, 0, 16, i * 2, 16);
        }
    }

    private final Kind kind;
    private final MapCodec<PoolBlock> codec;

    public PoolBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        this.codec = simpleCodec(p -> new PoolBlock(kind, p));
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 8));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(LEVEL)];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MaterialBlockEntity(pos, state);
    }

    public static String material(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof MaterialBlockEntity be ? be.material() : "";
    }

    /** Places a pool of {@code kind} for {@code material}. */
    public static void place(Level level, BlockPos pos, Kind kind, String material, int amount) {
        Block block = kind == Kind.MOLTEN ? RIRegistries.MOLTEN_POOL.get() : RIRegistries.MIXTURE_POOL.get();
        level.setBlock(pos, block.defaultBlockState().setValue(LEVEL, Math.max(1, Math.min(8, amount))), 3);
        if (level.getBlockEntity(pos) instanceof MaterialBlockEntity be) {
            be.setMaterial(material);
        }
    }

    /** Lowers the pool by {@code amount} levels, removing it when empty. */
    public static void lower(Level level, BlockPos pos, int amount) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof PoolBlock)) {
            return;
        }
        int left = state.getValue(LEVEL) - amount;
        if (left <= 0) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else {
            level.setBlock(pos, state.setValue(LEVEL, left), 3);
        }
    }

    /** Molten metal is slow (viscosity 5000 in the original, so a flow every 25 ticks); a mixture flows like water. */
    private int flowDelay() {
        return kind == Kind.MOLTEN ? 25 : 5;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !oldState.is(this)) {
            level.scheduleTick(pos, this, flowDelay());
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, flowDelay());
        }
    }

    /** Levels a neighbour holds that this pool could flow into: 0 for an empty space, its level for the same pool, -1 otherwise. */
    private int levelAt(Level level, BlockPos pos, String material) {
        BlockState state = level.getBlockState(pos);
        if (state.is(this)) {
            return material.equals(material(level, pos)) ? state.getValue(LEVEL) : -1;
        }
        return state.canBeReplaced() && state.getFluidState().isEmpty() && !(state.getBlock() instanceof PoolBlock) ? 0 : -1;
    }

    private void setLevel(Level level, BlockPos pos, String material, int amount) {
        BlockState state = level.getBlockState(pos);
        if (amount <= 0) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else if (state.is(this)) {
            if (state.getValue(LEVEL) != amount) {
                level.setBlock(pos, state.setValue(LEVEL, amount), 3);
            }
        } else {
            place(level, pos, kind, material, amount);
        }
    }

    /** Forge's finite fluid flow, as the original used: fall first, then share out with lower neighbours. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        String material = material(level, pos);
        int amount = state.getValue(LEVEL);
        BlockPos below = pos.below();
        int under = level.isOutsideBuildHeight(below) ? -1 : levelAt(level, below, material);
        if (under >= 0 && under < 8) {
            int moved = Math.min(amount, 8 - under);
            setLevel(level, below, material, under + moved);
            amount -= moved;
            if (amount <= 0) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                return;
            }
        }
        int total = amount;
        int count = 1;
        int[] levels = new int[4];
        Direction[] sides = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        for (int i = 0; i < 4; i++) {
            int l = levelAt(level, pos.relative(sides[i]), material);
            levels[i] = l >= 0 && l < amount - 1 ? l : -1;
            if (levels[i] >= 0) {
                count++;
                total += levels[i];
            }
        }
        if (count == 1) {
            setLevel(level, pos, material, amount);
            return;
        }
        int each = total / count;
        int rem = total % count;
        for (int i = 0; i < 4; i++) {
            if (levels[i] < 0) {
                continue;
            }
            int share = each;
            if (rem == count || rem > 1) {
                rem--;
                share++;
            }
            if (share != levels[i]) {
                setLevel(level, pos.relative(sides[i]), material, share);
            }
            count--;
        }
        setLevel(level, pos, material, rem > 0 ? each + 1 : each);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (kind == Kind.MOLTEN && entity instanceof LivingEntity living && !living.fireImmune()) {
            living.igniteForSeconds(5);
            living.hurt(level.damageSources().lava(), 4);
        }
        entity.makeStuckInBlock(state, new net.minecraft.world.phys.Vec3(0.8, 0.8, 0.8));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.BUCKET) || state.getValue(LEVEL) < 8) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            String material = material(level, pos);
            ItemStack filled = Materials.of(kind == Kind.MOLTEN ? RIRegistries.MOLTEN_BUCKET.get() : RIRegistries.MIXTURE_BUCKET.get(), material, 1);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            player.setItemInHand(hand, net.minecraft.world.item.ItemUtils.createFilledResult(stack, player, filled));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }
}
