package resonantinduction.atomic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import resonantinduction.registry.RIRegistries;

/**
 * Radiation sickness: radioactive items, ores, waste and explosions give it; it hurts every so often, faster the stronger it is.
 * A full hazmat suit keeps it off, wearing a little with each exposure.
 */
public final class Radiation {
    private Radiation() {}

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** Fifteen seconds of radiation at {@code amplifier}, unless the entity is protected. */
    public static void expose(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide || (entity instanceof Player p && (p.isCreative() || p.isSpectator()))) {
            return;
        }
        if (protectedBySuit(entity)) {
            for (EquipmentSlot slot : ARMOR) {
                entity.getItemBySlot(slot).hurtAndBreak(1, entity, slot);
            }
            return;
        }
        entity.addEffect(new MobEffectInstance(RIRegistries.RADIATION, 15 * 20, amplifier));
    }

    /** Everything alive within {@code radius} of {@code pos}. */
    public static void exposeAround(Level level, BlockPos pos, double radius, int amplifier) {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(radius))) {
            expose(e, amplifier);
        }
    }

    public static boolean protectedBySuit(LivingEntity entity) {
        for (EquipmentSlot slot : ARMOR) {
            ItemStack worn = entity.getItemBySlot(slot);
            if (!(worn.getItem() instanceof HazmatArmorItem)) {
                return false;
            }
        }
        return true;
    }

    /** The effect: one point of damage every 40 ticks, halving the wait for each level of strength. */
    public static class Effect extends MobEffect {
        public Effect() {
            super(MobEffectCategory.HARMFUL, 0x5A8C2B);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            int interval = Math.max(1, 40 >> amplifier);
            return duration % interval == 0;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            entity.hurt(entity.damageSources().source(RIRegistries.RADIATION_DAMAGE), 1);
            return true;
        }
    }
}
