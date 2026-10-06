package resonantinduction.archaic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import resonantinduction.resource.MachineRecipes;
import resonantinduction.resource.Materials;
import resonantinduction.resource.PoolBlock;
import resonantinduction.registry.RIRegistries;

/**
 * Filter. Under a dust mixture, every three seconds it lets two levels drip through as water and catches one refined dust,
 * as the original. Items marked on an Imprint in it fall through it; others don't.
 */
public class FilterBlock extends ImprintableBlock {
    public static final MapCodec<FilterBlock> CODEC = simpleCodec(FilterBlock::new);

    public FilterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends ImprintableBlock> codec() {
        return CODEC;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        level.scheduleTick(pos, this, 60);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.scheduleTick(pos, this, 60);
        BlockPos above = pos.above();
        BlockPos below = pos.below();
        BlockState top = level.getBlockState(above);
        if (!(top.getBlock() instanceof PoolBlock pool) || pool.kind() != PoolBlock.Kind.MIXTURE) {
            return;
        }
        boolean outlet = level.isEmptyBlock(below) || level.getCapability(Capabilities.FluidHandler.BLOCK, below, null) != null;
        if (!outlet) {
            return;
        }
        level.sendParticles(ParticleTypes.DRIPPING_WATER, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 3, 0.2, 0, 0.2, 0);
        String material = PoolBlock.material(level, above);
        ItemStack refined = MachineRecipes.mixer(Materials.of(RIRegistries.DUST.get(), material, 1));
        if (refined != null) {
            Block.popResource(level, above, refined);
        }
        PoolBlock.lower(level, above, 2);
        if (level.isEmptyBlock(below)) {
            level.setBlockAndUpdate(below, Fluids.FLOWING_WATER.getFlowing(7, false).createLegacyBlock());
        }
    }

    /** Items on the filter's imprint pass through; everything else (and every entity) is stopped. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext ec && ec.getEntity() instanceof ItemEntity item
                && level.getBlockEntity(pos) instanceof ImprintableBlockEntity be && be.isFiltering(item.getItem())) {
            return Shapes.empty();
        }
        return Shapes.block();
    }
}
