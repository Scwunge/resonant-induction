package resonantinduction.resource;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
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
 * A still pool of molten metal or of dust mixture (dust stirred into water), one to eight levels deep: the original's finite
 * per-metal fluids. Mixtures are filtered into refined dust; molten metal is poured into casting molds. An empty bucket scoops a
 * full pool; molten metal burns.
 */
public class PoolBlock extends BaseEntityBlock {
    public enum Kind { MOLTEN, MIXTURE }

    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 1, 8);
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
