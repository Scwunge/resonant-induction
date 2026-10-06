package resonantinduction.resource;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

/** Fluid type that names its metal: "Molten Iron", "Iron Mixture". */
public class MaterialFluidType extends FluidType {
    public MaterialFluidType(Properties properties) {
        super(properties);
    }

    @Override
    public Component getDescription(FluidStack stack) {
        String material = MaterialFluid.material(stack);
        if (material.isEmpty()) {
            return super.getDescription(stack);
        }
        return Component.translatable(getDescriptionId() + ".named", Materials.displayName(material));
    }
}
