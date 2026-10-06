package resonantinduction.atomic;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;

/** Hazmat suit piece: no armour, but the whole suit keeps radiation off (see {@link Radiation}). Lasts a very long time. */
public class HazmatArmorItem extends ArmorItem {
    public HazmatArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties.durability(200000));
    }
}
