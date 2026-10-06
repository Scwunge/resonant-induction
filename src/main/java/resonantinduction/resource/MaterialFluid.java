package resonantinduction.resource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import resonantinduction.registry.RIRegistries;

import java.util.function.Supplier;

/**
 * Molten metal and dust mixture as fluids for tanks, gutters and pipes. They carry their metal as a component on the fluid
 * stack (the original had one fluid per metal); in the world they are {@link PoolBlock}s, so this fluid has no block or bucket.
 */
public class MaterialFluid extends Fluid {
    private final Supplier<FluidType> type;

    public MaterialFluid(Supplier<FluidType> type) {
        this.type = type;
    }

    public static FluidStack stack(Fluid fluid, String material, int amount) {
        FluidStack stack = new FluidStack(fluid, amount);
        stack.set(RIRegistries.MATERIAL.get(), material);
        return stack;
    }

    public static String material(FluidStack stack) {
        return stack.getOrDefault(RIRegistries.MATERIAL.get(), "");
    }

    @Override
    public FluidType getFluidType() {
        return type.get();
    }

    @Override
    public Item getBucket() {
        return Items.AIR;
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) {
        return true;
    }

    @Override
    protected Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState state) {
        return Vec3.ZERO;
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return 5;
    }

    @Override
    protected float getExplosionResistance() {
        return 100f;
    }

    @Override
    public float getHeight(FluidState state, BlockGetter level, BlockPos pos) {
        return 1;
    }

    @Override
    public float getOwnHeight(FluidState state) {
        return 1;
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public boolean isSource(FluidState state) {
        return true;
    }

    @Override
    public int getAmount(FluidState state) {
        return 8;
    }

    @Override
    public VoxelShape getShape(FluidState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected void tick(Level level, BlockPos pos, FluidState state) {
    }
}
