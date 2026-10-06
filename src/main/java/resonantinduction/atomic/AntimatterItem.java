package resonantinduction.atomic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import resonantinduction.RIConfig;
import resonantinduction.ResonantInduction;
import resonantinduction.atomic.particle.FulminationBlock;
import resonantinduction.registry.RIRegistries;

/**
 * Antimatter cell, as the original. Dropped on the ground it lasts eight seconds, then annihilates: an explosion (bigger for a
 * gram) and radiation for anything within twenty blocks. Fulmination generators in the blast catch its energy.
 */
@EventBusSubscriber(modid = ResonantInduction.MODID)
public class AntimatterItem extends Item {
    /** The original's energy of a milligram going off, in joules. */
    private static final double ENERGY = 2_000_000_000_000_000d;

    private final int tier;

    public AntimatterItem(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    @Override
    public int getEntityLifespan(ItemStack stack, Level level) {
        return 160;
    }

    @SubscribeEvent
    public static void expire(ItemExpireEvent event) {
        ItemEntity item = event.getEntity();
        if (!(item.getItem().getItem() instanceof AntimatterItem antimatter) || item.level().isClientSide) {
            return;
        }
        Level level = item.level();
        level.playSound(null, item.getX(), item.getY(), item.getZ(), RIRegistries.ANTIMATTER_SOUND.get(), SoundSource.BLOCKS, 3f, 1f - level.random.nextFloat() * 0.3f);
        if (RIConfig.get(RIConfig.ANTIMATTER_EXPLOSIONS)) {
            float radius = 4 + 2 * antimatter.tier;
            double joules = (ENERGY + ENERGY * 9 * antimatter.tier) * RIConfig.get(RIConfig.FULMINATION_MULTIPLIER);
            FulminationBlock.absorb((ServerLevel) level, item.position(), radius, joules);
            level.explode(item, item.getX(), item.getY(), item.getZ(), radius, Level.ExplosionInteraction.TNT);
        }
        Radiation.exposeAround(level, item.blockPosition(), 20, 0);
        ResonantInduction.LOGGER.debug("Antimatter cell annihilated at {}", item.blockPosition());
    }
}
