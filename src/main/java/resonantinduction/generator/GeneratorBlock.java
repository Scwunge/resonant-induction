package resonantinduction.generator;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/** Block for the simple generators; right-click shows the buffer. */
public class GeneratorBlock extends BaseEntityBlock {
    private final Supplier<? extends BlockEntityType<? extends GeneratorBlockEntity>> type;
    private final BiFunction<BlockPos, BlockState, ? extends GeneratorBlockEntity> factory;
    private final VoxelShape shape;
    private final MapCodec<GeneratorBlock> codec;

    public GeneratorBlock(Properties properties, Supplier<? extends BlockEntityType<? extends GeneratorBlockEntity>> type,
                          BiFunction<BlockPos, BlockState, ? extends GeneratorBlockEntity> factory, VoxelShape shape) {
        super(properties);
        this.type = type;
        this.factory = factory;
        this.shape = shape;
        this.codec = simpleCodec(p -> new GeneratorBlock(p, type, factory, shape));
    }

    public static VoxelShape full() {
        return Shapes.block();
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.apply(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide || blockEntityType != type.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> GeneratorBlockEntity.serverTick(lvl, pos, st, (GeneratorBlockEntity) be);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GeneratorBlockEntity generator) {
            player.displayClientMessage(Component.translatable("tooltip.resonantinduction.energy", generator.energy(), generator.getEnergyCapability(null).getMaxEnergyStored()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
