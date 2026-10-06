package resonantinduction.resource;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import resonantinduction.registry.RIRegistries;

import java.util.List;

/** A pile of (refined) dust, one to eight layers deep like snow, as the original BlockDust. A burning firebox below melts it. */
public class DustPileBlock extends BaseEntityBlock {
    public static final IntegerProperty LAYERS = IntegerProperty.create("layers", 1, 8);
    private static final VoxelShape[] SHAPES = new VoxelShape[9];

    static {
        for (int i = 1; i <= 8; i++) {
            SHAPES[i] = Block.box(0, 0, 0, 16, i * 2, 16);
        }
    }

    private final boolean refined;
    private final MapCodec<DustPileBlock> codec;

    public DustPileBlock(boolean refined, Properties properties) {
        super(properties);
        this.refined = refined;
        this.codec = simpleCodec(p -> new DustPileBlock(refined, p));
        registerDefaultState(stateDefinition.any().setValue(LAYERS, 1));
    }

    public boolean refined() {
        return refined;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LAYERS);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(LAYERS)];
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return Block.isFaceFull(below.getCollisionShape(level, pos.below()), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MaterialBlockEntity(pos, state);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof MaterialBlockEntity be && !be.material().isEmpty()) {
            return List.of(Materials.of(refined ? RIRegistries.REFINED_DUST.get() : RIRegistries.DUST.get(), be.material(), state.getValue(LAYERS)));
        }
        return List.of();
    }

    /** Dust items: add a layer to a pile of the same dust, or start a new pile on the clicked face. */
    public static InteractionResult placeOrGrow(UseOnContext context, String material, boolean refined) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        Block block = refined ? RIRegistries.REFINED_DUST_PILE.get() : RIRegistries.DUST_PILE.get();
        BlockPos clicked = context.getClickedPos();
        BlockState state = level.getBlockState(clicked);
        if (state.is(block) && level.getBlockEntity(clicked) instanceof MaterialBlockEntity be && be.material().equals(material)) {
            int layers = state.getValue(LAYERS);
            if (layers < 8) {
                if (!level.isClientSide) {
                    level.setBlock(clicked, state.setValue(LAYERS, layers + 1), 3);
                    level.playSound(null, clicked, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 1f, 0.8f);
                    stack.consume(1, player);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        BlockPlaceContext place = new BlockPlaceContext(context);
        BlockPos pos = place.getClickedPos();
        if (!place.canPlace() || (player != null && !player.mayUseItemAt(pos, context.getClickedFace(), stack))) {
            return InteractionResult.FAIL;
        }
        BlockState newState = block.defaultBlockState();
        if (!newState.canSurvive(level, pos) || !level.isUnobstructed(newState, pos, CollisionContext.empty())) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, newState, 3);
            if (level.getBlockEntity(pos) instanceof MaterialBlockEntity be) {
                be.setMaterial(material);
            }
            level.playSound(null, pos, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 1f, 0.8f);
            stack.consume(1, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
