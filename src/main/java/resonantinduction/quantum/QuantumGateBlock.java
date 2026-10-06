package resonantinduction.quantum;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import resonantinduction.client.QuantumGateClient;
import resonantinduction.registry.RIRegistries;

import java.util.ArrayList;
import java.util.List;

/** The block a placed glyph lives in. Drawn by a block entity renderer; its shape is the filled corner slots. */
public class QuantumGateBlock extends BaseEntityBlock {
    public static final MapCodec<QuantumGateBlock> CODEC = simpleCodec(QuantumGateBlock::new);
    private static final VoxelShape[] SHAPES = new VoxelShape[256];

    static {
        for (int mask = 0; mask < 256; mask++) {
            VoxelShape shape = Shapes.empty();
            for (int slot = 0; slot < QuantumGateBlockEntity.SLOTS; slot++) {
                if ((mask & (1 << slot)) != 0) {
                    shape = Shapes.or(shape, Shapes.create(QuantumGateBlockEntity.slotBox(slot).deflate(0.02)));
                }
            }
            SHAPES[mask] = mask == 0 ? Shapes.create(0.25, 0.25, 0.25, 0.75, 0.75, 0.75) : shape.optimize();
        }
    }

    public QuantumGateBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return level.getBlockEntity(pos) instanceof QuantumGateBlockEntity gate ? SHAPES[gate.mask()] : SHAPES[0];
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuantumGateBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? createTickerHelper(type, RIRegistries.QUANTUM_GATE_BE.get(), QuantumGateClient::tick)
                : createTickerHelper(type, RIRegistries.QUANTUM_GATE_BE.get(), QuantumGateBlockEntity::serverTick);
    }

    /** Clicking a gate with a glyph adds it (the item handles that); any other item leaves the gate alone. */
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                                                  net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        return stack.isEmpty() ? net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof QuantumGateBlockEntity gate)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            int frequency = gate.frequency();
            if (player.isSecondaryUseActive() && frequency != -1) {
                gate.transport(player);
            } else if (frequency != -1) {
                player.displayClientMessage(Component.translatable("message.resonantinduction.quantum_gate.frequency", frequency), true);
            } else {
                player.displayClientMessage(Component.translatable("message.resonantinduction.quantum_gate.incomplete", gate.glyphCount()), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>();
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof QuantumGateBlockEntity gate) {
            for (int glyph : gate.glyphList()) {
                drops.add(new ItemStack(QuantumGlyphItem.byGlyph(glyph)));
            }
        }
        return drops;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof QuantumGateBlockEntity gate && !gate.glyphList().isEmpty()) {
            return new ItemStack(QuantumGlyphItem.byGlyph(gate.glyphList().get(0)));
        }
        return new ItemStack(QuantumGlyphItem.byGlyph(0));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof QuantumGateBlockEntity gate) {
            gate.unregister();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
