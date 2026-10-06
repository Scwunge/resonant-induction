package resonantinduction.archaic;

import net.minecraft.world.item.Item;

/** Hammer: crushes ores laid on an Engineering Table (see {@link EngineeringTableBlock}). 400 uses, as the original. */
public class HammerItem extends Item {
    public HammerItem(Properties properties) {
        super(properties.stacksTo(1).durability(400));
    }
}
