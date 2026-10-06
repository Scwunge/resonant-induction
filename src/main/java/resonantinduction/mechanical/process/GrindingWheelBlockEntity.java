package resonantinduction.mechanical.process;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import resonantinduction.mechanical.MechanicalBlockEntity;
import resonantinduction.mechanical.MechanicalNode;
import resonantinduction.registry.RIRegistries;
import resonantinduction.resource.MachineRecipes;

import java.util.List;
import java.util.UUID;

/**
 * Grinding Wheel. Driven through its axle (the two sides across its facing), it builds up torque; once it has enough, items
 * resting on it are ground (20 seconds each, as the original) into their ground products, and it flings and hurts whatever else
 * touches it.
 */
public class GrindingWheelBlockEntity extends MechanicalBlockEntity implements MachineBlock.EntityCollider {
    public static final int PROCESS_TIME = 20 * 20;
    private static final double REQUIRED_TORQUE = 1000;

    private double counter;
    @Nullable
    private UUID grinding;
    private int timeLeft;

    public GrindingWheelBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.GRINDING_WHEEL_BE.get(), pos, state);
    }

    public Direction facing() {
        return getBlockState().getValue(MachineBlock.FACING);
    }

    /** The axle runs across the facing: for a horizontal wheel, its left and right; for a vertical one, up and down. */
    public Direction.Axis axle() {
        Direction f = facing();
        return f.getAxis() == Direction.Axis.Y ? Direction.Axis.Y : f.getClockWise().getAxis();
    }

    @Override
    public double torqueLoad() {
        return 2;
    }

    @Override
    public double angularVelocityLoad() {
        return 2;
    }

    @Override
    protected boolean canMesh(Direction dir, MechanicalBlockEntity other) {
        return dir.getAxis() == axle();
    }

    @Override
    public boolean inverseRotation(Direction dir, MechanicalNode with) {
        return !(dir.getStepX() > 0 || dir.getStepZ() < 0 || dir.getStepY() < 0);
    }

    public String debug() {
        return "counter=" + counter + " grinding=" + grinding + " left=" + timeLeft + " torque=" + node.torque + " av=" + node.angularVelocity;
    }

    private boolean canWork() {
        return counter >= REQUIRED_TORQUE;
    }

    @Override
    public void onNodeUpdate(MechanicalNode n) {
        counter = Math.max(counter + n.torque, 0);
        if (!canWork() || grinding == null) {
            return;
        }
        ServerLevel server = (ServerLevel) level;
        if (!(server.getEntity(grinding) instanceof ItemEntity item) || !item.isAlive()
                || item.position().distanceTo(worldPosition.getCenter()) >= 1.1 || !MachineRecipes.canGrind(item.getItem())) {
            grinding = null;
            return;
        }
        item.setPickUpDelay(20);
        if (--timeLeft <= 0) {
            ItemStack stack = item.getItem();
            for (MachineRecipes.Output out : MachineRecipes.grinder(stack)) {
                if (server.random.nextFloat() <= out.chance()) {
                    Block.popResource(server, worldPosition.above(), out.stack().copy());
                }
            }
            stack.shrink(1);
            if (stack.isEmpty()) {
                item.discard();
                grinding = null;
            } else {
                item.setItem(stack);
                timeLeft = PROCESS_TIME;
            }
        } else {
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, item.getItem()), item.getX(), item.getY(), item.getZ(), 2, 0.2, 0.2, 0.2, 0.05);
        }
        if (server.getGameTime() % 8 == 0) {
            server.playSound(null, worldPosition, RIRegistries.GRINDER_SOUND.get(), SoundSource.BLOCKS, 0.5f, 1f);
        }
        counter -= REQUIRED_TORQUE;
    }

    @Override
    public void collide(Entity entity) {
        if (level.isClientSide) {
            return;
        }
        if (canWork()) {
            if (entity instanceof ItemEntity item) {
                if (MachineRecipes.canGrind(item.getItem())) {
                    if (grinding == null) {
                        grinding = item.getUUID();
                        timeLeft = PROCESS_TIME;
                    }
                } else {
                    // Things it can't grind drop through, as in the original.
                    item.setPos(item.getX(), item.getY() - 1.2, item.getZ());
                }
            } else {
                entity.hurt(level.damageSources().cactus(), 2);
            }
        }
        double speed = node.getAngularVelocity() / 20;
        if (speed != 0) {
            Direction push = facing().getAxis() == Direction.Axis.Y ? facing() : facing().getOpposite();
            entity.push(push.getStepX() * speed, level.random.nextDouble() * Math.abs(speed), push.getStepZ() * speed);
        }
    }
}
