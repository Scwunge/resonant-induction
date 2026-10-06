package resonantinduction.mechanical.process;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import resonantinduction.RIConfig;
import resonantinduction.mechanical.MachinePlayer;
import resonantinduction.mechanical.MechanicalBlockEntity;
import resonantinduction.mechanical.MechanicalNode;
import resonantinduction.registry.RIRegistries;
import resonantinduction.resource.MachineRecipes;

import java.util.List;

/**
 * Mechanical Piston (the crusher). Driven from any side but its face; once per turn it strikes the block in front: crushable
 * blocks (ores, stone, cobblestone, chests) crack and, after {@code breakCount} strikes, break into their crushed products;
 * anything else is pushed one block forward, block entity and all.
 */
public class MechanicalPistonBlockEntity extends MechanicalBlockEntity {
    private int strikes;

    public MechanicalPistonBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.MECHANICAL_PISTON_BE.get(), pos, state);
    }

    public Direction facing() {
        return getBlockState().getValue(MachineBlock.FACING);
    }

    @Override
    public double torqueLoad() {
        return 0.5;
    }

    @Override
    public double angularVelocityLoad() {
        return 0.5;
    }

    @Override
    protected boolean canMesh(Direction dir, MechanicalBlockEntity other) {
        return dir != facing();
    }

    @Override
    public void onRevolve(MechanicalNode n) {
        ServerLevel level = (ServerLevel) this.level;
        BlockPos target = worldPosition.relative(facing());
        if (!strike(level, target)) {
            BlockPos to = target.relative(facing());
            if (canMove(level, target, to)) {
                move(level, target, to);
            }
        }
    }

    private boolean strike(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        ItemStack asItem = new ItemStack(state.getBlock());
        List<MachineRecipes.Output> outputs = state.isAir() ? List.of() : MachineRecipes.crusher(asItem);
        int needed = RIConfig.get(RIConfig.PISTON_BREAK_COUNT);
        if (outputs.isEmpty() || state.getDestroySpeed(level, pos) < 0) {
            strikes = 0;
            level.destroyBlockProgress(-worldPosition.hashCode(), pos, -1);
            return false;
        }
        strikes++;
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.1);
        if (strikes >= needed) {
            strikes = 0;
            level.destroyBlockProgress(-worldPosition.hashCode(), pos, -1);
            if (MachinePlayer.mayBreak(level, pos, state)) {
                level.destroyBlock(pos, false);
                for (MachineRecipes.Output out : outputs) {
                    if (level.random.nextFloat() <= out.chance()) {
                        Block.popResource(level, pos, out.stack().copy());
                    }
                }
            }
        } else {
            level.destroyBlockProgress(-worldPosition.hashCode(), pos, strikes * 10 / needed);
        }
        return true;
    }

    private boolean canMove(ServerLevel level, BlockPos from, BlockPos to) {
        BlockState state = level.getBlockState(from);
        if (state.isAir() || state.getDestroySpeed(level, from) < 0 || state.getPistonPushReaction() == PushReaction.BLOCK
                || state.getPistonPushReaction() == PushReaction.DESTROY || level.getBlockEntity(from) instanceof MechanicalBlockEntity) {
            return false;
        }
        BlockState target = level.getBlockState(to);
        return (target.isAir() || target.canBeReplaced()) && level.isInWorldBounds(to);
    }

    /** Moves a block (with its block entity data) one step, as the original did. */
    private void move(ServerLevel level, BlockPos from, BlockPos to) {
        BlockState state = level.getBlockState(from);
        if (!MachinePlayer.mayBreak(level, from, state)) {
            return;
        }
        BlockEntity be = level.getBlockEntity(from);
        CompoundTag data = be == null ? null : be.saveWithFullMetadata(level.registryAccess());
        if (be != null) {
            level.removeBlockEntity(from);
        }
        level.setBlock(from, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_MOVE_BY_PISTON);
        level.setBlock(to, state, Block.UPDATE_ALL | Block.UPDATE_MOVE_BY_PISTON);
        if (data != null) {
            data.putInt("x", to.getX());
            data.putInt("y", to.getY());
            data.putInt("z", to.getZ());
            BlockEntity moved = level.getBlockEntity(to);
            if (moved != null) {
                moved.loadWithComponents(data, level.registryAccess());
                moved.setChanged();
            }
        }
    }
}
