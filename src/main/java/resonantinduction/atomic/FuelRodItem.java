package resonantinduction.atomic;

import net.minecraft.world.item.ItemStack;

/**
 * Fuel rods, as the original: a fissile rod is the main fuel of a reactor cell, a breeder rod re-breeds spent fuel. Both last 2500
 * seconds of burning (one point of wear a second).
 */
public class FuelRodItem extends RadioactiveItem {
    public static final int DECAY = 2500;
    public static final int BREEDING_TEMP = 1200;
    public static final long ENERGY = 100_000_000_000L;
    public static final long ENERGY_PER_TICK = ENERGY / 50000;

    private final boolean fissile;

    public FuelRodItem(boolean fissile, Properties properties) {
        super(properties.stacksTo(1).durability(DECAY).setNoRepair());
        this.fissile = fissile;
    }

    public boolean fissile() {
        return fissile;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}
