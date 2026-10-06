package resonantinduction.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;
import resonantinduction.registry.RIRegistries;

/**
 * Pipe, as the original: a pressure node that holds as much as it can move in a tick. Pipes join pipes of the same material
 * (and the same dye, or an undyed one), and any other fluid container.
 */
public class PipeBlockEntity extends FluidNodeBlockEntity {
    @Nullable
    private DyeColor color;

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.PIPE_BE.get(), pos, state, ((PipeBlock) state.getBlock()).material().maxFlowRate);
        node.maxFlowRate = material().maxFlowRate;
        node.maxPressure = material().maxPressure;
    }

    public PipeMaterial material() {
        return ((PipeBlock) getBlockState().getBlock()).material();
    }

    @Nullable
    public DyeColor color() {
        return color;
    }

    public void setColor(@Nullable DyeColor color) {
        this.color = color;
        markRecache();
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            // Neighbouring pipes may join or part.
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    protected FluidNode createNode() {
        return new FluidNode(this) {
            @Override
            public boolean canConnect(Direction from, Object other) {
                if (other instanceof FluidNode n && n.host() instanceof PipeBlockEntity pipe) {
                    return pipe.material() == material() && (pipe.color == color || pipe.color == null || color == null);
                }
                return other instanceof FluidNode || other instanceof IFluidHandler;
            }
        };
    }

    @Override
    protected void tickServer(ServerLevel level) {
        super.tickServer(level);
        // Show the connections in the block state, for the model.
        BlockState state = getBlockState();
        BlockState shown = state;
        for (Direction d : Direction.values()) {
            shown = shown.setValue(PipeBlock.SIDES.get(d), node.connected(d));
        }
        if (shown != state) {
            level.setBlock(worldPosition, shown, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (color != null) {
            tag.putInt("color", color.getId());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        color = tag.contains("color") ? DyeColor.byId(tag.getInt("color")) : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (color != null) {
            tag.putInt("color", color.getId());
        }
        return tag;
    }
}
