package resonantinduction.atomic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import resonantinduction.RIConfig;

/**
 * A radioactive block. Now and then it irradiates what's around it, and standing on it does too. Uranium ore does this only while
 * radioactive ores are allowed; radioactive waste is stronger, glows, and spreads over grass, dirt and mycelium.
 */
public class RadioactiveBlock extends Block {
    private final boolean ore;
    private final double radius;
    private final int amplifier;
    private final boolean spreads;
    private final MapCodec<RadioactiveBlock> codec;

    public RadioactiveBlock(boolean ore, double radius, int amplifier, boolean spreads, Properties properties) {
        super(properties.randomTicks());
        this.ore = ore;
        this.radius = radius;
        this.amplifier = amplifier;
        this.spreads = spreads;
        this.codec = simpleCodec(p -> new RadioactiveBlock(ore, radius, amplifier, spreads, p));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return codec;
    }

    private boolean active() {
        return !ore || RIConfig.get(RIConfig.RADIOACTIVE_ORES);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!active()) {
            return;
        }
        Radiation.exposeAround(level, pos, radius, amplifier);
        if (spreads) {
            BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(3) - 1, random.nextInt(3) - 1);
            BlockState there = level.getBlockState(target);
            if (there.is(Blocks.GRASS_BLOCK) || there.is(Blocks.DIRT) || there.is(Blocks.MYCELIUM) || there.is(Blocks.PODZOL)) {
                level.setBlockAndUpdate(target, defaultBlockState());
            }
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && active() && entity instanceof LivingEntity living) {
            Radiation.expose(living, amplifier);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (spreads && random.nextInt(3) == 0) {
            level.addParticle(new DustParticleOptions(new Vector3f(0.4f, 1f, 0.2f), 1f), pos.getX() + random.nextDouble(), pos.getY() + 1.05,
                    pos.getZ() + random.nextDouble(), 0, 0.02, 0);
        }
    }
}
