package resonantinduction.mechanical.process;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import resonantinduction.mechanical.MechanicalBlockEntity;
import resonantinduction.mechanical.MechanicalNode;
import resonantinduction.registry.RIRegistries;
import resonantinduction.resource.MachineRecipes;
import resonantinduction.resource.Materials;
import resonantinduction.resource.PoolBlock;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mixer. Driven from above or below, with its four diagonal neighbours walled in, it stirs everything in the 3x3 around it.
 * Dirty dust stirred for 12 seconds over water turns that water into a dust mixture (more dust thickens it, up to eight), which a
 * filter below then washes into refined dust, as in the original.
 */
public class MixerBlockEntity extends MechanicalBlockEntity {
    public static final int PROCESS_TIME = 12 * 20;

    private final Map<UUID, Integer> timers = new HashMap<>();
    private boolean walledIn;
    private int ticks;

    public MixerBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.MIXER_BE.get(), pos, state);
    }

    @Override
    protected boolean canMesh(Direction dir, MechanicalBlockEntity other) {
        return dir.getAxis() == Direction.Axis.Y;
    }

    @Override
    public boolean inverseRotation(Direction dir, MechanicalNode with) {
        return dir == Direction.DOWN;
    }

    @Override
    public void onNodeUpdate(MechanicalNode n) {
        ServerLevel server = (ServerLevel) level;
        if (++ticks % 20 == 0) {
            walledIn = true;
            for (int x = -1; x <= 1; x += 2) {
                for (int z = -1; z <= 1; z += 2) {
                    BlockState corner = server.getBlockState(worldPosition.offset(x, 0, z));
                    if (corner.isAir() || !corner.getFluidState().isEmpty()) {
                        walledIn = false;
                    }
                }
            }
        }
        if (n.getAngularVelocity() == 0 || !walledIn) {
            return;
        }
        Vec3 center = worldPosition.getCenter();
        boolean worked = false;
        // Items float on water in modern Minecraft, so reach half a block above the mixer's layer.
        AABB area = new AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 1.5, worldPosition.getZ() + 2);
        for (Entity entity : server.getEntities((Entity) null, area, e -> !e.isSpectator())) {
            // Swirl around the mixer's axis.
            Vec3 rel = entity.position().subtract(center);
            double angle = Math.toRadians(-n.getAngularVelocity());
            Vec3 rotated = new Vec3(rel.x * Math.cos(angle) - rel.z * Math.sin(angle), rel.y, rel.x * Math.sin(angle) + rel.z * Math.cos(angle));
            Vec3 push = rotated.subtract(rel).scale(0.5);
            // At high speed the original flung things out of the pool; keep the stir to a swirl.
            if (push.lengthSqr() > 0.01) {
                push = push.normalize().scale(0.1);
            }
            entity.push(push.x, push.y, push.z);
            if (entity instanceof ItemEntity item && MachineRecipes.mixer(item.getItem()) != null && item.position().distanceTo(center) < 2) {
                int left = timers.getOrDefault(item.getUUID(), PROCESS_TIME) - 1;
                if (left <= 0) {
                    if (mixIn(server, item)) {
                        ItemStack stack = item.getItem();
                        stack.shrink(1);
                        if (stack.isEmpty()) {
                            item.discard();
                            timers.remove(item.getUUID());
                        } else {
                            item.setItem(stack);
                            timers.put(item.getUUID(), PROCESS_TIME);
                        }
                    } else {
                        timers.put(item.getUUID(), 1);
                    }
                } else {
                    timers.put(item.getUUID(), left);
                    item.setPickUpDelay(20);
                    server.sendParticles(ParticleTypes.BUBBLE, item.getX(), item.getY(), item.getZ(), 1, 0.2, 0.2, 0.2, 0.05);
                }
                worked = true;
            }
        }
        if (worked && ticks % 20 == 0) {
            server.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.3f, 1.5f);
        }
        if (ticks % 200 == 0) {
            timers.keySet().removeIf(id -> server.getEntity(id) == null);
        }
    }

    public String debug() {
        return "av=" + node.getAngularVelocity() + " walled=" + walledIn + " timers=" + timers;
    }

    /** Stirs one dust into the water (or mixture) at the item's spot, level with the mixer. */
    private boolean mixIn(ServerLevel level, ItemEntity item) {
        String material = Materials.material(item.getItem());
        BlockPos pos = BlockPos.containing(item.getX(), worldPosition.getY(), item.getZ());
        if (material == null) {
            return false;
        }
        if (pos.equals(worldPosition)) {
            // Caught in the mixer's own column: use the side of the pool it is nearest to.
            Vec3 off = item.position().subtract(worldPosition.getCenter());
            pos = worldPosition.relative(Math.abs(off.x) > Math.abs(off.z) ? (off.x > 0 ? Direction.EAST : Direction.WEST) : (off.z > 0 ? Direction.SOUTH : Direction.NORTH));
        }
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof PoolBlock pool && pool.kind() == PoolBlock.Kind.MIXTURE) {
            if (PoolBlock.material(level, pos).equals(material) && state.getValue(PoolBlock.LEVEL) < 8) {
                level.setBlock(pos, state.setValue(PoolBlock.LEVEL, state.getValue(PoolBlock.LEVEL) + 1), 3);
                return true;
            }
            return false;
        }
        if (state.getFluidState().is(Fluids.WATER)) {
            PoolBlock.place(level, pos, PoolBlock.Kind.MIXTURE, material, 1);
            return true;
        }
        return false;
    }
}
